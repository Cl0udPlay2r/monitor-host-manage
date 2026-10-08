package com.example.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.entity.dto.Client;
import com.example.entity.dto.ClientDetail;
import com.example.entity.dto.ClientSsh;
import com.example.entity.vo.request.*;
import com.example.entity.vo.response.*;
import com.example.mapper.ClientDetailMapper;
import com.example.mapper.ClientMapper;
import com.example.mapper.ClientSshMapper;
import com.example.service.ClientService;
import com.example.utils.InfluxDbUtils;
import com.example.utils.CredentialCipher;
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

    @Resource
    ClientSshMapper  sshMapper;

    @Resource
    CredentialCipher cipher;

    @PostConstruct
    public void initClientCache(){
        clientIdCache.clear();
        clientTokenCache.clear();
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
        // 先持久化、后更新缓存：旧实现先写缓存再同步写时序库，导致时序库彻底熔断时
        // 大盘依旧展示实时数据（假健康），只看到历史数据在丢（见 F5）。
        // 调整顺序后，写入失败则缓存保持旧值，主机将在 1 分钟后被判离线，故障会在大盘上暴露出来。
        influx.writeRuntimeDetail(vo, client.getId());
        currentRuntimeDetail.put(client.getId(), vo);
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
            if(this.isOnline(runtime)){
                BeanUtils.copyProperties(runtime,vo);
                vo.setOnline(true);
            }
            return vo;
        }).toList();
    }

    @Override
    public List<ClientSimpleVO> listSimpleList() {
        return clientIdCache.values().stream().map(client -> {
            ClientSimpleVO vo = client.asViewObject(ClientSimpleVO.class);
            BeanUtils.copyProperties(detailMapper.selectById(client.getId()),vo);
            return vo;
        }).toList();
    }

    /**
     * 重命名主机名称
     * @param vo 请求参数
     */
    @Override
    public void renameClient(RenameClientVO vo) {
        this.update(Wrappers.<Client>update().eq("id",vo.getId()).set("name",vo.getName()));
        this.initClientCache();
    }

    @Override
    public ClientDetailsVO findClientDetailsById(int clientId) {
        ClientDetailsVO vo = clientIdCache.get(clientId).asViewObject(ClientDetailsVO.class);
        BeanUtils.copyProperties(detailMapper.selectById(clientId),vo);
        vo.setOnline(isOnline(currentRuntimeDetail.get(clientId)));
        return vo;
    }

    @Override
    public RuntimeHistoryVO runtimeDetailsHistory(int id) {
        return influx.queryRuntimeHistory(id);
    }

    @Override
    public RuntimeDetailVO runtimeDetailNow(int id) {
        return currentRuntimeDetail.get(id);
    }

    @Override
    public void renameNode(RenameNodeVO vo) {
        this.update(Wrappers.<Client>update()
                .eq("id",vo.getId()).set("node",vo.getNode())
                .set("location",vo.getLocation()));
        this.initClientCache();
    }

    @Override
    public void deleteClientById(int clientId) {
        this.removeById(clientId);
        detailMapper.deleteById(clientId);
        this.initClientCache();
        currentRuntimeDetail.remove(clientId);
    }

    @Override
    public void saveSshConnection(SshConnectionVO vo) {
        Client client = clientIdCache.get(vo.getId());
        if(client == null) return;
        ClientSsh ssh = new ClientSsh();
        BeanUtils.copyProperties(vo,ssh);
        // SSH 口令加密后入库：原先明文存储，数据库或备份泄漏即导致所有被管主机失守（见 F4）
        ssh.setPassword(cipher.encrypt(ssh.getPassword()));
        if(Objects.nonNull(sshMapper.selectById(ssh.getId()))){
            sshMapper.updateById(ssh);
        }else {
            sshMapper.insert(ssh);
        }
    }

    @Override
    public SshSettingVO sshSetting(int clientId) {
        ClientDetail detail = detailMapper.selectById(clientId);
        ClientSsh ssh = sshMapper.selectById(clientId);
        SshSettingVO vo;
        if(ssh == null){
            vo = new SshSettingVO();
        }else {
            vo = ssh.asViewObject(SshSettingVO.class);
            // 库里存的是密文，出参仍解密为明文供前端表单回填
            vo.setPassword(cipher.decrypt(vo.getPassword()));
        }
        vo.setIp(detail.getIp());
        return vo;
    }

    private boolean isOnline(RuntimeDetailVO runtime){
        return runtime != null && System.currentTimeMillis() - runtime.getTimestamp() < 60 * 1000;
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
        return sb.toString();
    }
}
