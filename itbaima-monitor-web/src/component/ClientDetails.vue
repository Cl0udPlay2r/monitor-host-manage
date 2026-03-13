<script setup lang="ts">
import {fitByUnit, percentageToStatus, cpuNameToImagePath, osNameToIcon, rename, copyIP} from '@/tools'
import {reactive, watch} from "vue";
import {get,post} from "@/net";
import {ElMessage, ElMessageBox} from "element-plus";

const locations = [
    {node: 'cn',desc: '中国大陆'},
    {node: 'hk',desc: '香港'},
    {node: 'jp',desc: '日本'},
    {node: 'us',desc: '美国'},
    {node: 'sg',desc: '新加坡'},
    {node: 'de',desc: '德国'},
    {node: 'kr',desc: '韩国'},
]

const props = defineProps({
    id: Number,
    update: Function
})
const details = reactive({
    base: {
        name: '',
        id: -1,
        ip: '',
        node: '',
        online: false,
        cpuName: '',
        osName: '',
        osVersion: '',
        cpuCores: -1,
        memory: -1,
        location: '',
        disk: -1
    },
    runtime: {},
    editNode: false
})

const nodeEdit = reactive({
    name: '',
    location: ''
})

const enableNodeEdit = () => {
    details.editNode = true
    nodeEdit.name = details.base.node;
    nodeEdit.location = details.base.location;
}

function updateDetails() {
    props.update()
    init(props.id)
}

function submitNodeEdit() {
    post("/api/monitor/node",{
        id: props.id,
        node: nodeEdit.name,
        location: nodeEdit.location
    },() => {
        details.editNode = false
        updateDetails()
        ElMessage.success("节点名称修改完成")
    })
}

const init = id => {
    if (id !== -1) {
        details.base = {
            name: '',
            id: -1,
            ip: '',
            node: '',
            online: false,
            cpuName: '',
            osName: '',
            osVersion: '',
            cpuCores: -1,
            memory: -1,
            location: '',
            disk: -1
        }
        get(`/api/monitor/details?clientId=${id}`, data => Object.assign(details.base, data))
    }
}
watch(() => props.id, init, {immediate: true})
</script>

<template>
    <div class="client-details" v-loading="Object.keys(details.base).length === 0">
        <div v-if="Object.keys(details.base).length">
            <div class="title">
                <i class="fa-solid fa-server"/>
                服务器信息
            </div>
            <el-divider style="margin: 10px 0"/>
            <div class="details-list">
                <div>
                    <span>服务器ID</span>
                    <span>{{ details.base.id }}</span>
                </div>
                <div>
                    <span>服务器名称</span>
                    <span>{{ details.base.name }}</span>&nbsp;
                    <i class="fa-solid fa-pen-to-square interact-item"
                       @click="rename(details.base.name,details.base.id,updateDetails)"></i>
                </div>
                <div>
                    <span>运行状态</span>
                    <span>
                    <i class="fa-solid fa-circle-play" style="color: #6fed5d" v-if="details.base.online"></i>
                    <i class="fa-solid fa-circle-stop" style="color:grey" v-else></i>
                    {{ details.base.online ? '运行中' : '已离线' }}
                </span>
                </div>
                <div v-if="!details.editNode">
                    <span>服务器节点</span>
                    <div :class="`flag-icon flag-icon-${details.base.location}`"/>&nbsp;
                    <span>{{ details.base.node }}</span>
                    <i class="fa-solid fa-pen-to-square interact-item" @click.stop="enableNodeEdit"/>
                </div>
                <div v-else>
                    <span>服务器节点</span>
                    <div style="display: inline-block;height: 15px">
                        <div style="display: flex">
                            <el-select v-model="nodeEdit.location" placeholder=""
                                       style="width: 53px" size="small">
                                <el-option v-for="item in locations" :value="item.node">
                                    <span :class="`flag-icon flag-icon-${item.node}`"></span>&nbsp;
                                    {{item.desc}}
                                </el-option>
                            </el-select>
                            <el-input v-model="nodeEdit.name" style="width: 200px;margin-left: 5px"
                                      size="small" placeholder="请输入节点名称..."/>
                            <div style="margin-left: 5px">
                                <i @click.stop="submitNodeEdit" class="fa-solid fa-check interact-item"/>
                            </div>
                        </div>
                    </div>
                </div>
                <div>
                    <span>公网IP</span>
                    <span>{{ details.base.ip }}</span>
                    <i class="fa-solid fa-copy interact-item" @click.stop="copyIP(details.base.ip)"
                       style="color: #5fb1f8"></i>
                </div>
                <div style="display: flex">
                    <span>处理器</span>
                    <span>{{ details.base.cpuName }}</span>
                    <el-image style="height: 20px;margin-left: 10px;"
                              :src="`public/cpu-icons/${cpuNameToImagePath(details.base.cpuName)}`"/>
                </div>
                <div>
                    <span>操作系统</span>
                    <i :style="{color: osNameToIcon(details.base.osName).color}"
                       :class="`fa-brands ${osNameToIcon(details.base.osName).icon}`"/>&nbsp;
                    <span> {{ `${details.base.osName} ${details.base.osVersion}` }}</span>
                </div>
                <div>
                    <span>硬件配置信息</span>
                    <span>
                        <i class="fa-solid fa-microchip"></i>
                        <span>{{ details.base.cpuCores }} CPU &numsp;/ &numsp;</span>
                        <i class="fa-solid fa-memory"></i>
                        {{ details.base.memory.toFixed(1) }} GB
                    </span>
                </div>
            </div>
            <div class="title" style="margin-top: 20px">
                <i class="fa-solid fa-gauge-high"/>
                实时监控
            </div>
            <el-divider style="margin: 10px 0"/>
            <div v-if="details.base.online">
                <div style="display: flex">
                    <el-progress style="margin: 0 20px 0 10px"
                                 status="success" :percentage=10 type="dashboard" :width="100">
                        <div style="font-size: 15px">CPU</div>
                        <div style="font-size: 14px;margin-top: 5px">20%</div>
                    </el-progress>
                    <el-progress
                        status="success" :percentage=36 type="dashboard" :width="100">
                        <div style="font-size: 15px">内存</div>
                        <div style="font-size: 14px;margin-top: 5px">12.1GB</div>
                    </el-progress>
                    <div style="margin-left: 20px;flex: 1">
                        <div style="font-size: 13px">
                            <div style="margin-left: 3px">实时网络速度</div>
                            <i class="fa-solid fa-arrow-up" style="color: #ffb10a"></i>
                            <span>{{ `${fitByUnit(10, 'KB')}` }}/s</span>
                            <el-divider direction="vertical"/>
                            <i class="fa-solid fa-arrow-down" style="color: #00e1ff"></i>
                            <span>{{ `${fitByUnit(149, 'KB')}` }}/s</span>
                        </div>
                        <div style="margin-top: 10px;display: flex;justify-content: space-between">
                            <div>
                                <i class="fa-solid fa-hard-drive" style="font-size: 14px"></i>
                                <span style="font-size: 13px">硬盘总容量</span>
                            </div>
                            <div style="flex: 1;text-align: end">
                                <div style="font-size: 13px;align-items: end">68GB/100GB</div>
                            </div>
                        </div>
                        <el-progress
                            type="line" :width="120" :percentage="68" :show-text="false" :status="'success'"/>
                    </div>
                </div>
            </div>
            <el-empty v-else
                      description="服务器处于离线状态，请检查服务器是否正常运行..." />
        </div>
    </div>
</template>

<style scoped>

.client-details {
    .interact-item {
        transition: .3s;

        &:hover {
            cursor: pointer;
            scale: 1.1;
            opacity: 0.8;
        }
    }

    height: 100%;

    .title {
        font-size: 20px;
        color: dodgerblue;
    }

    .details-list {
        font-size: 14px;

        & div {
            margin-bottom: 10px;

            & span:first-child {
                font-size: 13px;
                color: grey;
                display: inline-block;
                font-weight: normal;
                width: 180px;
            }

            & span {
                font-size: 14px;

            }
        }
    }
}
</style>