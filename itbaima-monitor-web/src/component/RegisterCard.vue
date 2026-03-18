<script setup lang="ts">

import {ref} from "vue";
import axios from "axios";

const props = defineProps({
    token: String
})

const text = ref()
</script>

<template>
    <div class="register-card">
        <div class="title"><i class="fa-regular fa-square-plus"/> 添加新的主机</div>
        <div class="desc">
            请按照以下步骤完成主机的注册，注册后即可实时管理和监视服务器以及检测运行状态
        </div>
        <el-divider style="margin: 10px 0 10px 0"/>
        <div class="sub-title">1.部署客户端</div>
        <div class="desc">
            在需要监控的服务端上运行监控客户端程序，客户端程序依赖于Java17运行环境，请提前安装好，启动完成即可进入下一步骤
        </div>
        <div class="sub-title">2.输入监控服务地址</div>
        <div class="desc">
            此地址用于实时上报客户端运行时数据，请务必正确填写
        </div>
        <el-input v-model="axios.defaults.baseURL" readonly />
        <div class="sub-title" style="margin-top: 5px">3.输入授权码</div>
        <div class="desc">
            客户端所有请求必须携带授权码才能被服务端正确识别
        </div>
        <el-input :model-value="token" readonly/>
    </div>

</template>

<style scoped>
.register-card {

    .title {
        color: #4dacf6;
        font-size: 18px;
        font-weight: bold
    }

    .sub-title {
        color: #6facec;
        font-size: 16px;
        font-weight: bold
    }

    .desc {
        margin-bottom: 10px;
        font-size: 12px;
        color: grey;

    }
}
</style>