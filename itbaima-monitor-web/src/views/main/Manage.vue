<script setup lang="ts">
import PreviewCard from "@/component/PreviewCard.vue";
import {reactive, ref} from "vue";
import {get} from "@/net";
import RegisterCard from "@/component/RegisterCard.vue";
import {Plus} from "@element-plus/icons-vue";

const list = ref([])

const updateList = () => get('/api/monitor/list', data => list.value = data)
setInterval(updateList, 10000)
updateList()

const register = reactive({
    show: false,
    token: ''
})

const refreshToken = () => get('api/monitor/register',data => register.token = data)
</script>

<template>
    <div class="manage-main">
        <div style="display: flex;justify-content: space-between">
            <div>
                <div class="title"><i class="fa-solid fa-server"></i> 主机管理界面</div>
                <div style="font-size: 13px;color: gray;margin: 5px 0">
                    我是注解管理界面，负责管理主机信息，爱的博阿斯难道你哦呜
                </div>
            </div>
            <div style="margin-top: 18px">
                <el-button type="primary" :icon="Plus" plain
                           @click="register.show = true">添加主机
                </el-button>
            </div>
        </div>

        <el-divider style="margin: 10px 0"></el-divider>
        <div class="card-list" v-if="list.length">
            <preview-card v-for="item in list" :data="item" :update="updateList"/>
        </div>
        <el-empty description="还没有任何主机哦..." v-else/>
        <el-drawer v-model="register.show" :with-header="false" direction="btt"
                   style="width: 600px;margin: 10px auto" size="350" @open="refreshToken">
            <register-card :token="register.token"/>
        </el-drawer>
    </div>
</template>

<style scoped>
:deep(.el-drawer) {
    margin: 10px;
    height: calc(100% - 20px);
    border-radius: 10px;
}

.manage-main {
    margin: 0 50px;

    .title {
        font-size: 22px;
        font-weight: bold;
    }

    .card-list {
        display: flex;
        gap: 10px;
        flex-wrap: wrap;
    }
}
</style>