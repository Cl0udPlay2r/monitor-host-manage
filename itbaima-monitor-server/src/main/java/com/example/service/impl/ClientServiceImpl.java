package com.example.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.entity.dto.Client;
import com.example.entity.dto.ClientDetail;
import com.example.entity.vo.request.ClientDetailVO;
import com.example.entity.vo.request.RenameClientVO;
import com.example.entity.vo.request.RuntimeDetailVO;
import com.example.entity.vo.response.ClientPreviewVO;
import com.example.mapper.ClientDetailMapper;
import com.example.mapper.ClientMapper;
import com.example.service.ClientService;
import com.example.utils.InfluxDbUtils;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;


import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ClientServiceImpl extends ServiceImpl<ClientMapper, Client> implements ClientService {

    private String registerToken = this.generateNewToken();

    private final Map<Integer, Client> clientIdCache = new ConcurrentHashMap<>();
    private final Map<String, Client> clientTokenCache = new ConcurrentHashMap<>();

    @Resource
    private ClientDetailMapper detailMapper;

    @Resource
    InfluxDbUtils influx;

    @PostConstruct
    public void initClientCache(){
        this.list().forEach(this::addClientCache);
    }

    @Override
    public String registerToken() {
        return registerToken;
    }

    @Override
    public Client findClientById(int id) {
        return clientIdCache.get(id);
    }

    @Override
    public Client findClientByToken(String token) {
        return clientTokenCache.get(token);
    }

    /**
     * 验证是否为系统用户发起的注册请求，并注册客户端
     * @param token 用户请求时携带的token
     * @return boolean
     */
    @Override
    public boolean verifyAndRegister(String token) {
        if (this.registerToken.equals(token)) {
            int id = this.randomClientId();
            Client client = new Client(id,"未命名主机",token,"cn","未命名节点" ,new Date());
            if (this.save(client)) {
                registerToken = this.generateNewToken();
                this.addClientCache(client);
                return true;
            }
        }
        return false;
    }

    private final Map<Integer,RuntimeDetailVO> currentRuntimeDetail = new ConcurrentHashMap<>();

    @Override
    public void updateRuntimeDetail(Client client, RuntimeDetailVO vo) {
        currentRuntimeDetail.put(client.getId(),vo);
        influx.writeRuntimeDetail(vo,client.getId());
    }

    @Override
    public void updateClientDetail(Client client, ClientDetailVO vo) {
        ClientDetail detail = new ClientDetail();
        BeanUtils.copyProperties(vo,detail);
        detail.setId(client.getId());
        if(Objects.nonNull(detailMapper.selectById(detail.getId()))){
            detailMapper.updateById(detail);
        }else {
            detailMapper.insert(detail);
        }
    }

    /**
     * 返回所有主机面板信息，当运行时的数据不存在或者最后一次上传时间大于1分钟，则判断为主机已离线
     *
     * @return 主机信息队列
     */
    @Override
    public List<ClientPreviewVO> listClients() {
        return clientIdCache.values().stream().map(client -> {
            ClientPreviewVO vo = client.asViewObject(ClientPreviewVO.class);
            BeanUtils.copyProperties(detailMapper.selectById(client.getId()),vo);
            RuntimeDetailVO runtime = currentRuntimeDetail.get(client.getId());
            if(runtime != null && System.currentTimeMillis() - runtime.getTimestamp() < 60 * 1000){
                BeanUtils.copyProperties(runtime,vo);
                vo.setOnline(true);
            }
            return vo;
        }).toList();
    }

    @Override
    public void renameClient(RenameClientVO vo) {
        this.update(Wrappers.<Client>update().eq("id",vo.getId()).set("name",vo.getName()));
        this.initClientCache();
    }

    private void addClientCache(Client client){
        clientIdCache.put(client.getId(),client);
        clientTokenCache.put(client.getToken(),client);
    }

    private int randomClientId() {
        return new Random().nextInt(90000000) + 10000000;
    }

    /**
     * 随即生成一个token
     * @return token
     */
    private String generateNewToken() {
        String CHARACTERS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890";
        StringBuilder sb = new StringBuilder();
        Random random = new Random();
        for (int i = 0; i < 24; i++) {
            sb.append(CHARACTERS.charAt(random.nextInt(CHARACTERS.length())));
        }
        System.out.println("token:" + sb);
        return sb.toString();
    }
}
