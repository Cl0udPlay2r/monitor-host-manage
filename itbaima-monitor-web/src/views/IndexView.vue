<script setup>
import {get, logout} from '@/net'
import router from "@/router";


function userLogout() {
    logout(() => router.push("/"))
}

</script>

<template>
    <el-button @click="userLogout" type="danger">退出登录</el-button>
</template>


<style lang="less" scoped>

.main-content {
    height: 100vh;
    width: 100vw;
}

.main-content-page {
    padding: 0;
    background-color: #f7f8fa;
}

.dark .main-content-page {
    background-color: #26282e;
}

.main-content-header {
    border-bottom: solid 1px var(--el-border-color);
    height: 55px;
    display: flex;
    align-items: center;
    box-sizing: border-box;
}

.logo {
    height: 30px;
}

.user-info {
    display: flex;
    justify-content: flex-end;
    align-items: center;

    .el-avatar:hover {
        cursor: pointer;
    }

    .profile {
        text-align: right;
        margin-right: 20px;

        :first-child {
            font-size: 18px;
            font-weight: bold;
            line-height: 20px;
        }

        :last-child {
            font-size: 11px;
            color: gray;
        }
    }
}
</style>
