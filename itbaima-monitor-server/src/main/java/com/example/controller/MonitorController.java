package com.example.controller;

import com.example.entity.RestBean;
import com.example.entity.dto.Account;
import com.example.entity.vo.request.RenameClientVO;
import com.example.entity.vo.request.RenameNodeVO;
import com.example.entity.vo.request.RuntimeDetailVO;
import com.example.entity.vo.request.SshConnectionVO;
import com.example.entity.vo.response.*;
import com.example.service.AccountService;
import com.example.service.ClientService;
import com.example.utils.Const;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/monitor")
public class MonitorController {

    @Resource
    ClientService service;

    @Resource
    AccountService accountService;

    @GetMapping("/list")
    public RestBean<List<ClientPreviewVO>> listAllClient(@RequestAttribute(Const.ATTR_USER_ID) int userId,
                                                         @RequestAttribute(Const.ATTR_USER_ROLE) String role) {
        List<ClientPreviewVO> clients = service.listClients();
        if (isAdminAccount(role)) {
            return RestBean.success(clients);
        } else {
            List<Integer> ids = accountAccessClients(userId);
            return RestBean.success(clients.stream()
                    .filter(o -> ids.contains(o.getId()))
                    .toList());
        }
    }

    @GetMapping("/simple-list")
    public RestBean<List<ClientSimpleVO>> simpleClientList(@RequestAttribute(Const.ATTR_USER_ROLE) String role) {
        if (isAdminAccount(role)) {
            return RestBean.success(service.listSimpleList());
        } else {
            return RestBean.noPermission();
        }
    }

    @PostMapping("/rename")
    public RestBean<Void> reClientName(@RequestBody @Valid RenameClientVO vo,
                                       @RequestAttribute(Const.ATTR_USER_ID) int userId,
                                       @RequestAttribute(Const.ATTR_USER_ROLE) String role) {
        if (permissionCheck(role, userId, vo.getId())) {
            service.renameClient(vo);
            return RestBean.success();
        } else {
            return RestBean.noPermission();
        }
    }

    @PostMapping("/node")
    public RestBean<Void> reNodeName(@RequestBody @Valid RenameNodeVO vo,
                                     @RequestAttribute(Const.ATTR_USER_ID) int userId,
                                     @RequestAttribute(Const.ATTR_USER_ROLE) String role) {
        if (permissionCheck(role, userId, vo.getId())) {
            service.renameNode(vo);
            return RestBean.success();
        } else {
            return RestBean.noPermission();
        }
    }

    @GetMapping("/details")
    public RestBean<ClientDetailsVO> details(int clientId,
                                             @RequestAttribute(Const.ATTR_USER_ID) int userId,
                                             @RequestAttribute(Const.ATTR_USER_ROLE) String role) {
        if (permissionCheck(role, userId, clientId)) {
            if (clientNotExist(clientId)) return RestBean.failure(404, "主机不存在");
            return RestBean.success(service.findClientDetailsById(clientId));
        } else {
            return RestBean.noPermission();
        }
    }

    @GetMapping("/runtime-history")
    public RestBean<RuntimeHistoryVO> runtimeDetailsHistory(int clientId,
                                                            @RequestAttribute(Const.ATTR_USER_ID) int userId,
                                                            @RequestAttribute(Const.ATTR_USER_ROLE) String role) {
        if (permissionCheck(role, userId, clientId)) {
            if (clientNotExist(clientId)) return RestBean.failure(404, "主机不存在");
            return RestBean.success(service.runtimeDetailsHistory(clientId));
        } else {
            return RestBean.noPermission();
        }
    }

    @GetMapping("/runtime-now")
    public RestBean<RuntimeDetailVO> runtimeDetailsNow(int clientId,
                                                       @RequestAttribute(Const.ATTR_USER_ID) int userId,
                                                       @RequestAttribute(Const.ATTR_USER_ROLE) String role) {
        if (permissionCheck(role, userId, clientId)) {
            if (clientNotExist(clientId)) return RestBean.failure(404, "主机不存在");
            return RestBean.success(service.runtimeDetailNow(clientId));
        } else {
            return RestBean.noPermission();
        }
    }

    @GetMapping("/register")
    public RestBean<String> registerToken(@RequestAttribute(Const.ATTR_USER_ROLE) String role) {
        if (isAdminAccount(role)) {
            return RestBean.success(service.registerToken());
        } else {
            return RestBean.noPermission();
        }
    }

    @GetMapping("/delete")
    public RestBean<Void> deleteClient(int clientId,
                                       @RequestAttribute(Const.ATTR_USER_ROLE) String role) {
        if (isAdminAccount(role)) {
            service.deleteClientById(clientId);
            return RestBean.success();
        } else {
            return RestBean.noPermission();
        }
    }

    @PostMapping("/ssh-save")
    public RestBean<Void> sshConnection(@RequestBody @Valid SshConnectionVO vo,
                                        @RequestAttribute(Const.ATTR_USER_ID) int userId,
                                        @RequestAttribute(Const.ATTR_USER_ROLE) String role){
        if(permissionCheck(role, userId, vo.getId())){
            service.saveSshConnection(vo);
            return RestBean.success();
        }else {
            return RestBean.noPermission();
        }
    }

    @GetMapping("/ssh")
    public RestBean<SshSettingVO> sshSetting(int clientId,
                                             @RequestAttribute(Const.ATTR_USER_ID) int userId,
                                             @RequestAttribute(Const.ATTR_USER_ROLE) String role){
        if(permissionCheck(role, userId, clientId)){
            if (clientNotExist(clientId)) return RestBean.failure(404, "主机不存在");
            return  RestBean.success(service.sshSetting(clientId));
        }else {
            return RestBean.noPermission();
        }
    }

    /**
     * 主机是否存在。刻意放在权限校验之后调用：越权用户仍应得到 403，
     * 而不应通过 404 与 200 的差异推测出某台主机是否存在（见 F15）。
     */
    private boolean clientNotExist(int clientId) {
        return service.findClientById(clientId) == null;
    }

    private boolean isAdminAccount(String role) {
        role = role.substring(5);
        return Const.ROLE_ADMIN.equals(role);
    }

    private List<Integer> accountAccessClients(int uid) {
        Account account = accountService.getById(uid);
        return account.getClientList();
    }

    private boolean permissionCheck(String role, int uid, int clientId) {
        if (isAdminAccount(role)) return true;
        else {
            return accountAccessClients(uid).contains(clientId);
        }
    }
}
