<script setup lang="ts">
import {fitByUnit} from '@/tools'
import {useClipboard} from "@vueuse/core";
import {ElMessage, ElMessageBox} from "element-plus";
import {post} from '@/net';

const props = defineProps({
    data: Object,
    update: Function
})

const {copy} = useClipboard()
const copyIP = () => copy(props.data.ip).then(() => {
    ElMessage.success("IP已成功复制到剪切板")
})

function rename() {
    ElMessageBox.prompt('请确认新的服务器主机名称', '修改名称', {
        confirmButtonText: "确认",
        cancelButtonText: '取消',
        inputValue: props.data.name,
        inputPattern:
            /^[A-Za-z0-9_\u4e00-\u9fa5]{1,10}$/,
        inputErrorMessage: '名称中只能包含中英文字符、数字和下划线',
    }).then(({value}) => {
        post('api/monitor/rename', {
            id: props.data.id,
            name: value
        },()=>{
            ElMessage.success('主机名称已修改成功')
            props.update()
        })
    })
}
</script>

<template>
    <div class="instance-card">
        <div style="display: flex;justify-content: space-between">
            <div class="name">
                <div :class="`flag-icon flag-icon-${data.location}`"/>
                <span style="margin: 0 5px">{{ data.name }}</span>
                <i class="fa-solid fa-pen-to-square interact-item" @click="rename"></i>
            </div>
            <div class="status" v-if="data.online">
                <i class="fa-solid fa-circle-play" style="color: #6fed5d"></i>
                <span style="margin: 0 5px">运行中</span>
            </div>
            <div class="status" v-else>
                <i class="fa-solid fa-circle-stop" style="color:grey"></i>
                <span style="margin: 0 5px">离线</span>
            </div>
        </div>
        <div class="os">
            <span>操作系统: {{ `${data.osName} ${data.osVersion}` }}</span>
        </div>
        <el-divider style="margin: 10px 0"></el-divider>
        <div class="network">
            <span style="margin: 0 5px">公网IP:{{ data.ip }}</span>
            <i class="fa-solid fa-copy interact-item" @click.stop="copyIP" style="color: #5fb1f8"></i>
        </div>
        <div class="cpu">
            <span>处理器: {{ data.cpuName }}</span>
        </div>
        <div class="hardware">
            <div>
                <i class="fa-solid fa-microchip"></i>
                <span> {{ data.cpuCores }} CPU</span>
            </div>
            <div>
                <i class="fa-solid fa-memory"></i>
                <span> {{ data.memory.toFixed(1) }} GB</span>
            </div>
        </div>
        <div class="process">
            <span>CPU: {{ (data.cpuUsage).toFixed(1) }}%</span>
            <el-progress status="success" :percentage=data.cpuUsage :show-text="false"/>
            <span>内存:{{ data.memoryUsage.toFixed(1) }} GB</span>
            <el-progress status="success" :percentage=(data.memoryUsage/data.memory*100) :show-text="false"/>
        </div>
        <div class="network-flow">
            <div>网络流量</div>
            <div>
                <i class="fa-solid fa-arrow-up"></i>
                <span>{{ `${fitByUnit(data.networkUpload, 'KB')}` }}/s</span>
                <el-divider direction="vertical"/>
                <i class="fa-solid fa-arrow-down"></i>
                <span>{{ `${fitByUnit(data.networkDownload, 'KB')}` }}/s</span>
            </div>
        </div>
    </div>
</template>

<style scoped>
:deep(.el-progress-bar__inner) {
    background-color: #6fed5d;
}

:deep(.el-progress-bar__outer) {
    background-color: #18cb1822;
}

.dark .instance-card {
    color: var(--el-text-color);
}

.interact-item {
    cursor: pointer;
}

.instance-card {
    width: 300px;
    padding: 10px 12px;
    background-color: var(--el-bg-color);
    border-radius: 5px;
    box-sizing: border-box;

    .name {
        font-size: 16px;
        font-weight: bold;
    }

    .status {
        font-size: 14px;
    }

    .os {
        font-size: 13px;
        color: grey;
    }

    .network {
        font-size: 14px;
        margin: 5px 0;
    }

    .cpu {
        font-size: 13px;
    }

    .hardware {
        font-size: 14px;
        display: flex;
        gap: 25px;
    }

    .process {
        margin: 10px 0;
        font-size: 13px;
        display: flex;
        flex-direction: column;
        gap: 8px;
    }

    .network-flow {
        font-size: 13px;
        display: flex;
        justify-content: space-between;

    }
}
</style>