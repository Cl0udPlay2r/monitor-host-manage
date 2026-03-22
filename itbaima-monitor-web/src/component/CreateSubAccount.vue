<script setup >
import {reactive, ref} from "vue";
import {User, Lock, Message} from "@element-plus/icons-vue";
import {osNameToIcon} from '@/tools'
import {post} from "@/net";
import {ElMessage} from "element-plus";

const props = defineProps({
    clients: Array
})

const emits = defineEmits(['create'])

const form = reactive({
    username: '',
    password: '',
    email: '',
})

const formRef = ref()
const valid = ref(false)
const onValidate = (prop, isValid) => valid.value = isValid

const validateUsername = (rule,value,callback) =>{
    if(value === ''){
        callback(new Error('请输入用户名'))
    }else if(!/^[a-z0-9A-Z\u4e00-\u9fa5]+$/.test(value)){
        callback(new Error('用户名中不能包含除中英文以外的字符'))
    }else {
        callback()
    }
}


const rules = {
    password: [
        {required: true,message: '输入旧密码', trigger: ['change', 'blur']},
        {min: 6,max: 18,message: '密码长度范围为6~18',trigger: ['change', 'blur']},
    ],
    username: [
        {required: true,message: '输入用户名', trigger: ['change', 'blur']},
        {validator: validateUsername,trigger: ['change', 'blur']},
        {min: 2,max: 18,message: '用户名长度范围为2~18',trigger: ['change', 'blur']},
    ],
    email: [
        {required: true,message: '输入邮箱地址', trigger: ['change', 'blur']},
        {type: 'email',message: '请输入合法的邮箱地址', trigger: ['change', 'blur']}
    ]
}

const checkClients = []
function onCheck(state,id) {
    if (state){
        checkClients.push(id)
    }else {
        const index = checkClients.indexOf(id)
        checkClients.splice(index,1)
    }
}

function createSubAccount() {
    formRef.value.validate(isValid => {
        if(checkClients.length === 0){
            ElMessage.warning('请至少选择一个服务器')
            return
        }
        if(isValid){
            post('/api/user/sub/create', {
                ...form,
                clients: checkClients
            },() =>{
                ElMessage.success('子账户创建成功')
                emits('create')
            })
        }
    })
}
</script>

<template>
    <div style="padding: 15px 20px;height: 100%">
        <div style="display: flex;flex-direction: column;height: 100%">
            <div>
                <div class="title">
                    <i class="fa-solid fa-user-plus"/>子账户管理
                </div>
                <div class="desc">
                    子账户同样用于管理服务器，凡是可以指定分配不同的服务器，子账户只能访问被分配的服务器
                </div>
                <el-divider style="margin: 10px 0"/>
            </div>
            <div>
                <el-form :model="form" ref="formRef" :rules="rules"
                         label-position="top" @validate="onValidate">
                    <el-form-item prop="username" label="用户名">
                        <el-input v-model="form.username" :prefix-icon="User" type="text"
                        placeholder="子账户用户名" maxlength="18"/>
                    </el-form-item>
                    <el-form-item prop="password" label="密码">
                        <el-input v-model="form.password" :prefix-icon="Lock" type="password"
                                  placeholder="子账户密码" maxlength="18"/>
                    </el-form-item>
                    <el-form-item prop="email" label='邮箱'>
                        <el-input v-model="form.email" :prefix-icon="Message" type='text'
                                  placeholder="子账户邮箱" maxlength="25"/>
                    </el-form-item>
                </el-form>
                <el-divider style="margin: 10px 0"/>
                <div class="desc">请在下方选择允许子用户访问的服务器列表</div>
            </div>
            <el-scrollbar style="flex: 1">
                <div class="client-card" v-for="item in clients">
                    <el-checkbox @change="state => onCheck(state,item.id)"/>
                    <div style="margin-left: 20px">
                        <div style="font-size: 14px;font-weight: bold">
                            <div :class="`flag-icon flag-icon-${item.location}`"/>
                            <span style="margin: 0 5px">{{ item.name }}</span>
                        </div>
                        <div style="font-size: 12px;color: grey">
                            <span> 操作系统: </span>
                            <i :style="{color: osNameToIcon(item.osName).color}"
                               :class="`fa-brands ${osNameToIcon(item.osName).icon}`"/>
                            <span> {{ `${item.osName} ${item.osVersion}` }}</span>
                        </div>
                        <div style="font-size: 12px;color: grey">
                            <span style="margin-right: 5px">公网IP:{{ item.ip }}</span>
                        </div>
                    </div>
                </div>
            </el-scrollbar>
            <div style="text-align: center;margin-top: 10px">
                <el-button type="success" @click="createSubAccount"
                           :disabled="!valid" palin>创建子账户</el-button>
            </div>
        </div>
    </div>
</template>

<style scoped>
.title {
    font-size: 20px;
    font-weight: bold;
    color: dodgerblue;
}
.desc {
    font-size: 13px;
    line-height: 16px;
    color: grey;
}

.client-card {
    border-radius: 5px;
    background-color: var(--el-bg-color-page);
    padding: 10px;
    display: flex;
    align-items: center;
    margin: 10px;
}
</style>