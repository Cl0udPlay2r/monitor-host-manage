package com.example.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.entity.dto.Client;

public interface ClientService extends IService<Client> {
    String registerToken();
    Client findClientByToken(String token);
    Client findClientById(int id);
    boolean verifyAndRegister(String token);
}
