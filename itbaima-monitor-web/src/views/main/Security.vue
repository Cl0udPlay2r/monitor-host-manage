<script setup lang="ts">
import {Lock, Plus, Switch} from '@element-plus/icons-vue'
import {reactive, ref} from "vue";
import {post,logout} from "@/net";
import router from "@/router";

const formRef = ref()
const valid = ref(false)

const onValidate = (prop, isValid) => valid.value = isValid

const form = reactive({
    new_password: '',
    old_password: '',
    new_password_repeat: ''
})

const validatePassword = (rule,value,callback) =>{
    if(value === ''){
        callback(new Error('请输入重复密码'))
    }else if(value !== form.new_password){
        callback(new Error('新密码前后输入不一致'))
    }else {
        callback()
    }
}

const rules = {
    password: [
        {required: true,message: '输入旧密码', trigger: ['change', 'blur']}
    ],
    old_password: [
        {required: true,message: '输入新密码', trigger: ['change', 'blur']},
        {min: 6,max: 18 ,message: '密码长度范围为6~18',trigger: ['change', 'blur']}
    ],
    new_password_repeat: [
        {required: true,message: '输入重复密码', trigger: ['change', 'blur']},
        {validator: validatePassword,trigger: ['change', 'blur']},
    ]
}


function resetPassword(){
    formRef.value.validate(isValid => {
        if(isValid){
            post('/api/user/change-password',form,() => {
                logout(() => router.push('/'),
                    '重置密码成功,请重新登录')
            })
        }
    })
}
</script>

<template>
    <div style="display: flex;gap: 10px">
        <div style="flex: 50%">
            <div class="info-card">
                <div class="title">
                    <i class="fa-solid fa-lock"/>修改密码
                </div>
                <el-divider style="margin: 10px 0"/>
                <el-form @validate="onValidate" :model="form" ref="formRef" :rules="rules">
                    <el-form-item prop="old_password" label="旧密码" style="margin-top: 20px">
                        <el-input v-model="form.old_password" type="password"
                                  :prefix-icon="Lock" maxlength="18" placeholder="旧密码"/>
                    </el-form-item>
                    <el-form-item prop="new_password" label="新密码">
                        <el-input v-model="form.new_password" type="password"
                                  :prefix-icon="Lock" maxlength="18" placeholder="新密码"/>
                    </el-form-item>
                    <el-form-item prop="new_password_repeat" label="重复新密码">
                        <el-input  v-model="form.new_password_repeat" type="password"
                                  :prefix-icon="Lock" maxlength="18" placeholder="重复新密码"/>
                    </el-form-item>
                    <div style="text-align:center">
                        <el-button type="primary" :icon="Switch"
                                   :disabled="!valid" @click="resetPassword">
                            立即重置密码
                        </el-button>
                    </div>

                </el-form>
            </div>
            <div class="info-card" style="margin-top: 10px">
            </div>
        </div>
        <div class="info-card" style="flex: 50%">
            <div class="title">
                <i class="fa-solid fa-users"/>子用户管理
            </div>
            <el-divider style="margin: 10px 0"/>
            <el-empty :image-size="100" description="还没有任何子用户哦">
                <el-button :icon="Plus" type="primary" plain>添加子用户</el-button>
            </el-empty>
        </div>
    </div>

</template>

<style scoped>
.info-card {
    border-radius: 7px;
    padding: 15px 20px;
    background-color: var(--el-bg-color);
    height: fit-content;

    .title {
        font-size: 18px;
        font-weight: bold;
        color: dodgerblue;
    }
}


</style>