package com.example.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.entity.dto.Client;
import com.example.entity.vo.request.ClientDetailVO;
import com.example.entity.vo.request.RuntimeDetailVO;
import com.example.entity.vo.response.ClientPreviewVO;

import java.util.List;

public interface ClientService extends IService<Client> {
    String registerToken();
    Client findClientByToken(String token);
    Client findClientById(int id);
    boolean verifyAndRegister(String token);
    void updateClientDetail(Client client, ClientDetailVO vo);
    void updateRuntimeDetail(Client client, RuntimeDetailVO vo);
    List<ClientPreviewVO> listClients();
}
