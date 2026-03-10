<script setup lang="ts">
import PreviewCard from "@/component/PreviewCard.vue";
import {ref} from "vue";
import {get} from "@/net";

const list = ref([])

const updateList = () => get('/api/monitor/list', data =>list.value = data)
setInterval(updateList,10000)
updateList()

</script>

<template>
    <div class="manage-main">
        <div class="title"><i class="fa-solid fa-server"></i> 主机管理界面</div>
        <div style="font-size: 13px;color: gray;margin: 5px 0">我是注解管理界面，负责管理主机信息，爱的博阿斯难道你哦呜
        </div>
        <el-divider style="margin: 10px 0"></el-divider>
        <div class="card-list">
            <preview-card v-for="item in list" :data="item"/>
        </div>
    </div>
</template>

<style scoped>
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