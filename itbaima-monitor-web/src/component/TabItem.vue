<script setup lang="ts">

defineProps({
    name: String,
    active: Boolean
})

</script>

<template>
    <div :class="`tab-item ${active ? 'active' : ''}`">
        {{name}}
    </div>
</template>

<style scoped>
.tab-item {
    margin: 0 15px;
    font-size: 14px;
    box-sizing: border-box;
    width: 45px;
    align-items: center;
    text-align: center;
    line-height: 55px;
    transition: 0.3s color;

    &:hover{
        cursor: pointer;
        font-size: 18px;
        transition: 0.5s font-size;
        color: var(--el-color-primary);
    }
}

.active{
    border-bottom: 2px solid var(--el-color-primary);
    font-size: 18px;
    color: var(--el-color-primary);
}
</style>