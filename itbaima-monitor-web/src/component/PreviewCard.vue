<script setup>
import {copyIP, cpuNameToImagePath, fitByUnit, osNameToIcon, percentageToStatus, rename} from '@/tools'
import {reactive} from "vue";
import ClientDetails from "@/component/ClientDetails.vue";
import TerminalWindow from "@/component/TerminalWindow.vue";

const props = defineProps({
    data: Object,
    update: Function
})

const detail = reactive({
    show: false,
    id: -1
})
const displayClientDrawer = (id) => {
    detail.show = true
    detail.id = id
}

function terminalShow(id) {
    terminal.show = true
    terminal.id = id
    detail.show = false
}

const terminal = reactive({
    show: false,
    id: -1
})
</script>

<template>
    <div>
        <div class="instance-card">
            <div style="display: flex;justify-content: space-between">
                <div class="name">
                    <div :class="`flag-icon flag-icon-${data.location}`"/>
                    <span style="margin: 0 5px">{{ data.name }}</span>
                    <i class="fa-solid fa-pen-to-square interact-item" @click="rename(data.name,data.id,update)"></i>
                </div>
                <div class="status" v-if="data.online">
                    <i class="fa-solid fa-circle-play" style="color: #6fed5d"></i>
                    <span style="margin: 0 5px">运行中</span>
                </div>
                <div class="status" v-else>
                    <i class="fa-solid fa-circle-stop" style="color:grey"></i>
                    <span style="margin: 0 5px">已离线</span>
                </div>
            </div>
            <div class="os">
                <span> 操作系统: </span>
                <i :style="{color: osNameToIcon(data.osName).color}"
                   :class="`fa-brands ${osNameToIcon(data.osName).icon}`"/>
                <span> {{ `${data.osName} ${data.osVersion}` }}</span>
            </div>
            <el-divider style="margin: 10px 0"></el-divider>
            <div @click="displayClientDrawer(data.id)">
                <div class="network">
                    <span style="margin-right: 5px">公网IP:{{ data.ip }}</span>
                    <i class="fa-solid fa-copy interact-item" @click.stop="copyIP(data.ip)" style="color: #5fb1f8"></i>
                </div>
                <div class="cpu">
                    <span>处理器: {{ data.cpuName }}</span>
                    <el-image style="height: 20px;margin-left: 10px;"
                              :src="`public/cpu-icons/${cpuNameToImagePath(data.cpuName)}`"/>
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
                    <el-progress :status="percentageToStatus(data.cpuUsage)" :percentage=data.cpuUsage
                                 :show-text="false"/>
                    <span>内存:{{ data.memoryUsage.toFixed(1) }} GB</span>
                    <el-progress :status="percentageToStatus(data.memoryUsage/data.memory*100)"
                                 :percentage=(data.memoryUsage/data.memory*100) :show-text="false"/>
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
        </div>
        <el-drawer size="520" :show-close="false" v-model="detail.show"
                   :with-header="false" @close="detail.id = -1">
            <client-details :id="detail.id" :update="update" @delete="props.update"
                            @terminal="terminalShow"/>
        </el-drawer>
        <el-drawer style="width: 800px" :size="500" direction="btt"
                   @close="terminal.id=-1"
                   v-model="terminal.show" :close-on-click-modal="false">
            <template #header>
                <div>
                    <div style="font-size: 18px;color: dodgerblue;font-weight: bold">SSH远程连接</div>
                    <div style="font-size: 13px;color: grey">
                        远程连接的建立将由服务端完成，因此在内网环境下也能使用
                    </div>
                </div>
            </template>
            <template #default>
                <terminal-window :id="terminal.id"/>
            </template>
        </el-drawer>
    </div>
</template>

<style scoped>
:deep(.el-drawer) {
    margin: 10px;
    height: calc(100% - 20px);
    border-radius: 10px;
}

.dark .instance-card {
    color: var(--el-text-color);
}

.interact-item {
    transition: .3s;

    &:hover {
        cursor: pointer;
        scale: 1.1;
        opacity: 0.8;
    }
}

.instance-card {
    width: 300px;
    padding: 10px 12px;
    background-color: var(--el-bg-color);
    border-radius: 5px;
    box-sizing: border-box;
    transition: .3s;

    &:hover {
        cursor: pointer;
        scale: 1.01;
    }

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