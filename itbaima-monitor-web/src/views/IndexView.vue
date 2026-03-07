<script setup>
import {get, logout} from '@/net'
import router from "@/router";
import {ref} from "vue";
import {useDark} from "@vueuse/core";
import {Back, Moon, Sunny} from "@element-plus/icons-vue";
import TabItem from "@/component/TabItem.vue";
import {useRoute} from "vue-router";

const route = useRoute()
const dark = ref(useDark());
const tabs =[
    {id: 1,name: "管理",route: "manage"},
    {id: 2,name: "安全",route: "security"}
]
const defaultIndex = () => {
    for (let tab of tabs) {
        if(tab.route === route.name)
            return tab.id;
    }
    return 1
}

const tab = ref(defaultIndex())
function changePage(item) {
    console.log("item.route:" + item.route + "; tab.value:" + tab.value + "; item.id:" + item.id);
    tab.value = item.id;
    router.push({name: item.route})
}

function userLogout() {
    logout(() => router.push("/"))
}


</script>

<template>
    <el-container class="main-container">
        <el-header class="main-header">
            <el-image style="height: 30px"
                      src="https://element-plus.org/images/element-plus-logo.svg"/>
            <div class="tabs">
                <tab-item v-for="item in tabs" :name="item.name"
                          :active="item.id === tab " @click="changePage(item)"/>
                <el-dropdown style="margin: 0 10px 0 15px">
                    <el-avatar
                        src="https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png"/>
                    <template #dropdown>
                        <el-dropdown-menu>
                            <el-dropdown-item @click="userLogout">
                                <el-icon><Back/></el-icon>
                                退出登录
                            </el-dropdown-item>
                        </el-dropdown-menu>
                    </template>
                </el-dropdown>
                <el-switch
                           v-model="dark" active-color="#424242"
                           :active-action-icon="Moon"
                           :inactive-action-icon="Sunny"/>
            </div>
        </el-header>
        <el-main class="main-content">
            <router-view :key="route.fullPath" v-slot="{Component}">
                <transition name="el-fade-in-linear" mode="out-in">
                    <component :is="Component"/>
                </transition>
            </router-view>
        </el-main>
    </el-container>
</template>


<style lang="less" scoped>
.main-container {
    height: 100vh;
    width: 100vw;

    .main-header {
        height: 55px;
        background-color: var(--el-bg-color);
        border-bottom: 1px solid var(--el-border-color);
        display: flex;
        align-items: center;

        .tabs {
            height: 55px;
            gap: 10px;
            align-items: center;
            flex: 1px;
            display: flex;
            justify-content: right;
        }
    }

    .main-content {
        height: 100%;
        background-color: #f5f5f5;
    }
}

.dark .main-container .main-content {
    background-color: #232323;
}
</style>
