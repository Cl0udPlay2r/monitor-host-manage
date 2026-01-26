package com.example.service.impl;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.entity.dto.*;
import com.example.entity.vo.request.AddCommentVO;
import com.example.entity.vo.request.TopicCreateVO;
import com.example.entity.vo.request.TopicUpdateVO;
import com.example.entity.vo.response.CommentVO;
import com.example.entity.vo.response.TopicDetailVO;
import com.example.entity.vo.response.TopicPreviewVO;
import com.example.entity.vo.response.TopicTopVO;
import com.example.mapper.*;
import com.example.service.NotificationService;
import com.example.service.TopicService;
import com.example.utils.CacheUtils;
import com.example.utils.Const;
import com.example.utils.FlowUtils;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Service
public class TopicServiceImpl extends ServiceImpl<TopicMapper, Topic> implements TopicService {

    @Resource
    TopicTypeMapper topicTypeMapper;

    @Resource
    FlowUtils flowUtils;

    @Resource
    CacheUtils cacheUtils;

    @Resource
    StringRedisTemplate template;

    @Resource
    AccountMapper accountMapper;

    @Resource
    AccountDetailsMapper accountDetailsMapper;

    @Resource
    AccountPrivacyMapper accountPrivacyMapper;

    @Resource
    TopicCommentMapper commentMapper;

    @Resource
    NotificationService notificationService;

    private Set<Integer> types = null;

    @PostConstruct
    private void initTypes() {
        types = this.listTypes()
                .stream()
                .map(TopicType::getId)
                .collect(Collectors.toSet());
    }

    @Override
    public List<TopicType> listTypes() {
        return topicTypeMapper.selectList(null);
    }

    /**
     * 发表帖子
     *
     * @param uid 用户id
     * @param vo  前端请求的用户编辑的帖子内容
     * @return 不为空返回失败原因 为空是成功
     */
    @Override
    public String createTopic(int uid, TopicCreateVO vo) {
        JSONObject content = vo.getContent();
        if (content == null) return "帖子内容为空！";
        if (!textLimitCheck(content, 20000)) return "帖子内容太多，发文失败！";
        if (!types.contains(vo.getType())) return "文章类型非法！";
        if (!flowUtils.limitPeriodCounterCheck(Const.FORUM_TOPIC_CREATE_COUNTER + uid, 5, 3600))
            return "发文频繁，请稍后再试!";
        Topic topic = new Topic();
        BeanUtils.copyProperties(vo, topic);
        topic.setContent(content.toJSONString());
        topic.setUid(uid);
        topic.setTime(new Date());
        if (this.save(topic)) {
            cacheUtils.deleteFromCachePattern(Const.FORUM_TOPIC_CREATE_COUNTER + "*");//当发帖成功时将所以缓存清空
            return null;
        } else {
            return "内部错误，请联系管理员";
        }
    }

    @Override
    public String updateTopic(TopicUpdateVO vo, int uid) {
        JSONObject content = vo.getContent();
        if (content == null) return "帖子内容为空！";
        if (!textLimitCheck(content, 20000)) return "帖子内容太多，发文失败！";
        if (!types.contains(vo.getType())) return "文章类型非法！";
        if (!flowUtils.limitPeriodCounterCheck(Const.FORUM_TOPIC_CREATE_COUNTER + uid, 5, 3600))
            return "修改频繁，请稍后再试!";
        baseMapper.update(null, Wrappers.<Topic>update()
                .eq("uid", uid)
                .eq("id", vo.getId())
                .set("title", vo.getTitle())
                .set("content", content.toString())
                .set("type", vo.getType())
        );
        return null;
    }

    @Override
    public String createComment(AddCommentVO vo, int uid) {
        String content = vo.getContent();
        if (content == null) return "评论内容为空！";
        if (!textLimitCheck(JSONObject.parseObject(content), 2000)) return "评论内容太多，发文失败！";
        if (!flowUtils.limitPeriodCounterCheck(Const.FORUM_TOPIC_COMMENT_COUNTER + uid, 3, 60))
            return "评论频繁，请稍后再试!";
        TopicComment comment = new TopicComment();
        comment.setUid(uid);
        BeanUtils.copyProperties(vo, comment);
        comment.setTime(new Date());
        commentMapper.insert(comment);
        //下面是通知功能的实现 有两种通知 一种评论了该用户的帖子的评论 一种是评论了该用户评论的评论
        Topic topic = baseMapper.selectById(vo.getTid());//获取该评论的帖子信息
        Account account = accountMapper.selectById(uid);//获取发表评论的用户信息
        if(vo.getQuote() > 0){  //quote大于一说明这个发表的评论是引用了其他评论 （下面判断是否为回复自己的评论）
            TopicComment com = commentMapper.selectById(vo.getQuote());
            if(!Objects.equals(account.getId(),com.getUid())){//评论评论的用户不是自己
                notificationService.addNotification(
                        com.getUid(),
                        "您有新的评论回复"
                        ,account.getUsername() + "回复了您的评论，快去看看吧"
                        ,"success"
                        ,"/index/topic-detail/" + com.getTid()
                );
            }
        }else if(!Objects.equals(account.getId(),topic.getUid())){//当该评论的用户和帖子用户不相同时
            notificationService.addNotification(
                    topic.getUid(),
                    "您有新的帖子回复"
                    ,account.getUsername() + "回复了您的帖子:"+ topic.getTitle() +"，快去看看吧"
                    ,"success"
                    ,"/index/topic-detail/" + topic.getId()
            );
        }
        return null;
    }

    @Override
    public List<CommentVO> listComments(int tid, int pageNumber) {
        Page<TopicComment> page = new Page<>(pageNumber, 10);
        commentMapper.selectPage(page,Wrappers.<TopicComment>query().eq("tid",tid));
        return page.getRecords().stream().map(dto -> {
            CommentVO vo = new CommentVO();
            BeanUtils.copyProperties(dto,vo);
           if(dto.getQuote() > 0){
               TopicComment comment = commentMapper.selectOne(Wrappers.<TopicComment>query()
                       .eq("id", dto.getQuote()).orderByAsc("time"));
               if(comment != null){
                   JSONObject object = JSONObject.parseObject(comment.getContent());
                   StringBuilder builder = new StringBuilder();
                   this.shortContent(object.getJSONArray("ops"),builder,ignore -> {});
                   vo.setQuote(builder.toString());
               }else{
                   vo.setQuote("此评论已被删除！");
               }

           }
           CommentVO.User user =new CommentVO.User();
            Account account = accountMapper.selectById(dto.getUid());
            BeanUtils.copyProperties(account,user);
            user.setName(account.getUsername());
            vo.setUser(user);
           return vo;
        }).toList();
    }

    @Override
    public void deleteComment(int id, int uid) {
        commentMapper.delete(Wrappers.<TopicComment>query().eq("id",id).eq("uid",uid));
    }

    @Override
    public List<TopicPreviewVO> listTopicCollects(int uid) {
        return baseMapper.listCollects(uid)
                .stream()
                .map(topic -> {
                    TopicPreviewVO vo = new TopicPreviewVO();
                    BeanUtils.copyProperties(topic, vo);
                    return vo;
                }).toList();
    }

    /**
     * 获取帖子列表，将帖子转化成帖子预览格式列表返回
     *
     * @param pageNumber 开始页面 默认为0
     * @param type       查看帖子类型 默认为0（全查看）
     * @return 预览格式的帖子
     */
    @Override
    public List<TopicPreviewVO> listTopicByPage(int pageNumber, int type) {
        String key = Const.FORUM_TOPIC_PREVIEW_CACHE + pageNumber + ":" + type;
        List<TopicPreviewVO> list = cacheUtils.takeListFromCache(key, TopicPreviewVO.class);
        if (list != null) return list;
        Page<Topic> page = Page.of(pageNumber, 10);
        if (type == 0)
            baseMapper.selectPage(page, Wrappers.<Topic>query().orderByDesc("time"));
        else
            baseMapper.selectPage(page, Wrappers.<Topic>query().eq("type", type).orderByDesc("time"));
        List<Topic> topics = page.getRecords();
        if (topics == null) return null;
        list = topics.stream().map(this::resolveToPreview).toList();
        cacheUtils.saveListToCache(key, list, 60);
        return list;
    }

    /**
     * 将数据库中获取的topic数据处理成返回给前端的TopicPreviewVO（内容最多展示300字）
     *
     * @param topic 数据库中获取的帖子
     * @return 回给前端的帖子
     */
    private TopicPreviewVO resolveToPreview(Topic topic) {
        TopicPreviewVO preview = new TopicPreviewVO();
        //由于简化了topic表 现在topic只负责自己的数据 不携带account的数据 所以需要另外给preview传一份account的数据
        BeanUtils.copyProperties(accountMapper.selectById(topic.getUid()), preview);
        BeanUtils.copyProperties(topic, preview);
        preview.setLike(baseMapper.interactCount(topic.getId(), "like"));
        preview.setCollect(baseMapper.interactCount(topic.getId(), "collect"));
        List<String> images = new ArrayList<>();
        StringBuilder previewText = new StringBuilder();
        JSONArray ops = JSONObject.parseObject(topic.getContent()).getJSONArray("ops");
        this.shortContent(ops, previewText, obj -> images.add(obj.toString()));
        preview.setImages(images);
        preview.setText(previewText.length() >= 300 ? previewText.substring(0, 300) : previewText.toString());
        return preview;
    }

    /**
     * 将一些长文本转化为短文本，超出部分舍去最长为300字
     * @param ops 需要处理的对象数组
     * @param previewText 拼接文本
     * @param imageHandler 处理图片的消费者，一般只有帖子内容需要
     */
    private void shortContent(JSONArray ops,StringBuilder previewText,Consumer<Object> imageHandler) {
        for (Object op : ops) {
            Object insert = JSONObject.from(op).get("insert");
            if (insert instanceof String text) {
                if (previewText.length() >= 300) continue;
                previewText.append(text);
            } else if (insert instanceof Map<?, ?> map) {
                Optional.ofNullable(map.get("image"))
                        .ifPresent(imageHandler);
            }
        }
    }

    @Override
    public List<TopicTopVO> listTopTopics() {
        List<Topic> topics = baseMapper.selectList(Wrappers.<Topic>query()
                .select("id", "title", "time")
                .eq("top", 1));
        return topics.stream().map(topic -> {
            TopicTopVO vo = new TopicTopVO();
            BeanUtils.copyProperties(topic, vo);
            return vo;
        }).toList();
    }

    @Override
    public TopicDetailVO getTopic(int tid, int uid) {
        TopicDetailVO vo = new TopicDetailVO();
        Topic topic = baseMapper.selectById(tid);
        BeanUtils.copyProperties(topic, vo);
        TopicDetailVO.InteractTopic interact = new TopicDetailVO.InteractTopic(
                hasInteract(tid, uid, "like"),
                hasInteract(tid, uid, "collect")
        );
        vo.setInteractTopic(interact);
        TopicDetailVO.User user = new TopicDetailVO.User();
        vo.setUser(this.fillUserDetailsByPrivacy(user, topic.getUid()));
        vo.setComments(commentMapper.selectCount(Wrappers.<TopicComment>query().eq("tid",tid)));
        return vo;
    }

    @Override
    public void interact(Interact interact, boolean status) {
        String type = interact.getType();
        synchronized (type.intern()) {
            template.opsForHash().put(type, interact.toKey(), Boolean.toString(status));
            this.saveInteractScheduled(type);
        }
    }

    private boolean hasInteract(int tid, int uid, String type) {
        String key = tid + ":" + uid;
        if (template.opsForHash().hasKey(key, type)) {
            return Boolean.parseBoolean(template.opsForHash().entries(type).get(key).toString());
        }
        return baseMapper.userInteractCount(tid, uid, type) > 0;
    }

    private final Map<String, Boolean> status = new HashMap<>();
    ScheduledExecutorService service = Executors.newScheduledThreadPool(2);

    private void saveInteractScheduled(String type) {
        if (!status.getOrDefault(type, false)) {
            status.put(type, true);
            service.schedule(() -> {
                try {
                    this.saveInteract(type);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                status.put(type, false);
            }, 3, TimeUnit.SECONDS);
        }
    }

    private void saveInteract(String type) {
        synchronized (type.intern()) {
            List<Interact> check = new LinkedList<>();
            List<Interact> unCheck = new LinkedList<>();
            template.opsForHash().entries(type).forEach((k, v) -> {
                if (Boolean.parseBoolean(v.toString()))
                    check.add(Interact.parseInteract(k.toString(), type));
                else
                    unCheck.add(Interact.parseInteract(k.toString(), type));
            });
            if (!check.isEmpty())
                baseMapper.addInteract(check, type);
            if (!unCheck.isEmpty())
                baseMapper.deleteInteract(unCheck, type);
            template.delete(type);
        }
    }

    /**
     * 根据用户设置的隐私设置过滤出要展示信息
     *
     * @param target 展示的实体
     * @param uid    用户id
     * @return 过滤完的实体
     */
    private <T> T fillUserDetailsByPrivacy(T target, int uid) {
        Account account = accountMapper.selectById(uid);
        AccountDetails accountDetails = accountDetailsMapper.selectById(uid);
        AccountPrivacy accountPrivacy = accountPrivacyMapper.selectById(uid);
        String[] ignores = accountPrivacy.hiddenFields();
        BeanUtils.copyProperties(account, target, ignores);
        BeanUtils.copyProperties(accountDetails, target, ignores);
        return target;
    }

    private boolean textLimitCheck(JSONObject object, int max) {
        if (object == null) return false;
        long length = 0;
        for (Object op : object.getJSONArray("ops")) {
            length += JSONObject.from(op).getString("insert").length();
            if (length > max) return false;
        }
        return true;
    }

}
