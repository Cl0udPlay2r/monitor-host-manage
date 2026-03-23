<script setup >
import PreviewCard from "@/component/PreviewCard.vue";
import {computed, reactive, ref} from "vue";
import {get} from "@/net";
import RegisterCard from "@/component/RegisterCard.vue";
import {Plus} from "@element-plus/icons-vue";
import {useRoute} from "vue-router";
import {useStore} from '@/store';

const store = useStore()
const list = ref([])

const locations = [
    {node: 'cn', desc: '中国大陆'},
    {node: 'hk', desc: '香港'},
    {node: 'jp', desc: '日本'},
    {node: 'us', desc: '美国'},
    {node: 'sg', desc: '新加坡'},
    {node: 'de', desc: '德国'},
    {node: 'kr', desc: '韩国'},
]
const checkNodes = ref([])

const route = useRoute()

const clientList = computed(() => {
    if (checkNodes.value.length === 0)
        return list.value
    else return list.value.filter(item => checkNodes.value.indexOf(item.location) >= 0)
})

const updateList = () => {
    if(route.name === 'manage'){
        get('/api/monitor/list', data => list.value = data)
    }
}
setInterval(updateList, 10000)
updateList()

const register = reactive({
    show: false,
    token: ''
})

const refreshToken = () => get('api/monitor/register', data => register.token = data)


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
                <el-button type="primary" :icon="Plus" plain :disabled="!store.isAdmin"
                           @click="register.show = true">添加主机
                </el-button>
            </div>
        </div>
        <el-divider style="margin: 10px 0"></el-divider>
        <el-checkbox-group v-model="checkNodes">
            <el-checkbox v-for="item in locations" :key="item" :value="item.node" border>
                <span :class="`flag-icon flag-icon-${item.node}`"/>
                <span style="font-size: 13px;margin: 3px">
                    {{ item.desc }}
                </span>
            </el-checkbox>
        </el-checkbox-group>
        <div class="card-list" v-if="list.length">
            <preview-card v-for="item in clientList" :data="item" :update="updateList"/>
        </div>
        <el-empty description="还没有任何主机哦..." v-else/>
        <el-drawer v-model="register.show" :with-header="false" direction="btt"
                   style="width: 600px;margin: 10px auto" size="350" @open="refreshToken">
            <register-card :token="register.token"/>
        </el-drawer>

    </div>
</template>

<style scoped>
:deep(.el-checkbox-group .el-checkbox) {
    margin-right: 10px;
}

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
        margin-top: 10px;
        display: flex;
        gap: 10px;
        flex-wrap: wrap;
    }
}
</style>