<script setup>

import {reactive, ref, watch} from "vue";
import {get, post} from "@/net/index.js";
import {ElMessage} from "element-plus";

const props = defineProps({
    id: Number
})

const form = reactive({
    ip: '',
    port: 22,
    username: '',
    password: ''
})
const formRef = ref()
const valid = ref(false)

const onValidate = (isValid) => valid.value = isValid

const rules = {
    port: [
        {required: true, message: '请输入端口号', trigger: ['blur', 'change']},
    ],
    username: [
        {required: true, message: '请输入用户名', trigger: ['blur', 'change']},
    ],
    password: [
        {required: true, message: '请输入密码', trigger: ['blur', 'change']},
    ]
}

function saveConnection() {
    formRef.value.validate((valid) => {
        if(valid) {
            post('api/monitor/ssh-save',{
                ...form,
                id: props.id
            },() => {
                ElMessage.success('正在连接...')
            },(message) => {
                ElMessage.warning(message)
            })
        }
    })
}

watch(() => props.id, id => {
    form.ip = ''
    get(`/api/monitor/ssh?clientId=${id}`,data => Object.assign(form,data))
},{immediate:true})
</script>

<template>
    <div class="terminal-main">
        <div class="login" v-loading="!form.ip">
            <i style="font-size: 50px" class="fa-solid fa-terminal"/>
            <div style="font-size: 20px;font-weight: bold;margin-top: 10px">服务器连接信息</div>
            <el-form :rules="rules" ref="formRef" @validate="onValidate"
                     label-width="100" :model="form" style="width: 400px;margin: 10px auto">
                <div style="display: flex;gap: 10px">
                    <el-form-item style="width: 100%" prop="ip" label="服务器ip地址">
                        <el-input v-model="form.ip" disabled/>
                    </el-form-item>
                    <el-form-item prop="port" label-width="0" style="width: 80px">
                        <el-input v-model="form.port"  placeholder="端口"/>
                    </el-form-item>
                </div>
                <el-form-item prop="username" label="用户名">
                    <el-input v-model="form.username"  placeholder="登录用户名"/>
                </el-form-item>
                <el-form-item prop="password" label="密码">
                    <el-input v-model="form.password"  placeholder="登录密码"/>
                </el-form-item>

                <el-button type="success" plain @click="saveConnection">确认连接</el-button>
            </el-form>
        </div>
    </div>

</template>

<style scoped>
.terminal-main {
    width: 100%;
    height: 100%;

    .login {
        text-align: center;
        padding-top: 50px;
        height: 100%;
        box-sizing: border-box;
    }
}
</style>