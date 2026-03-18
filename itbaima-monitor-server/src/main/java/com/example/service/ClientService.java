package com.example.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.entity.dto.Client;
import com.example.entity.vo.request.ClientDetailVO;
import com.example.entity.vo.request.RenameClientVO;
import com.example.entity.vo.request.RenameNodeVO;
import com.example.entity.vo.request.RuntimeDetailVO;
import com.example.entity.vo.response.ClientDetailsVO;
import com.example.entity.vo.response.ClientPreviewVO;
import com.example.entity.vo.response.RuntimeHistoryVO;

import java.util.List;

public interface ClientService extends IService<Client> {
    String registerToken();
    Client findClientByToken(String token);
    Client findClientById(int id);
    boolean verifyAndRegister(String token);
    void updateClientDetail(Client client, ClientDetailVO vo);
    void updateRuntimeDetail(Client client, RuntimeDetailVO vo);
    List<ClientPreviewVO> listClients();
    void renameClient(RenameClientVO vo);
    ClientDetailsVO findClientDetailsById(int clientId);
    void renameNode(RenameNodeVO vo);
    RuntimeHistoryVO runtimeDetailsHistory(int id);
    RuntimeDetailVO runtimeDetailNow(int id);
    void deleteClientById(int clientId);
}
