<script setup>
import {Delete, Lock, Message, Plus, Refresh, Switch} from '@element-plus/icons-vue'
import {reactive, ref} from "vue";
import {get, logout, post} from "@/net";
import router from "@/router";
import CreateSubAccount from "@/component/CreateSubAccount.vue";
import {ElMessage, ElMessageBox} from "element-plus";
import {useStore} from "@/store/index.js";

const store = useStore()
const formRef = ref()
const valid = ref(false)

const onValidate = (prop, isValid) => valid.value = isValid

const form = reactive({
    old_password: '',
    new_password: '',
    new_password_repeat: ''
})

const validatePassword = (rule, value, callback) => {
    if (value === '') {
        callback(new Error('请输入重复密码'))
    } else if (value !== form.new_password) {
        callback(new Error('新密码前后输入不一致'))
    } else {
        callback()
    }
}

const emailForm = reactive({
    email: store.user.email,
    code: ''
})

const coldTime = ref(0)
const isEmailValid = ref(true)

const onEmailValidate = (prop, isValid) => {
    if (prop === 'email')
        return isEmailValid.value = isValid
}

const validateEmail = () => {
    coldTime.value = 60
    let handle
    get(`/api/auth/ask-code?email=${emailForm.email}&type=modify`, () => {
        ElMessage.success(`邮件发送至${emailForm.email}，请注意查收`)
        handle = setInterval(() => {
            coldTime.value--
            if(coldTime.value === 0) {
                clearInterval(handle)
            }
        },1000)
    },(message) => {
        ElMessage.warning(message)
        coldTime.value = 0
    })
}

function modifyEmail() {
    post(`/api/user/modify-email`,emailForm,() => {
        logout(router.push('/'),'邮件修改成功,请重新登录')
    })
}

const rules = {
    password: [
        {required: true, message: '输入旧密码', trigger: ['change', 'blur']},
        {min: 6, max: 18, message: '密码长度范围为6~18', trigger: ['change', 'blur']}
    ],
    old_password: [
        {required: true, message: '输入新密码', trigger: ['change', 'blur']},
        {min: 6, max: 18, message: '密码长度范围为6~18', trigger: ['change', 'blur']}
    ],
    new_password_repeat: [
        {required: true, message: '输入重复密码', trigger: ['change', 'blur']},
        {validator: validatePassword, trigger: ['change', 'blur']},
    ],
    email: [
        {required: true, message: '输入电子邮件地址', trigger: ['change', 'blur']},
        {type: 'email', message: '输入合法邮件地址', trigger: ['change', 'blur']},
    ]
}


function resetPassword() {
    formRef.value.validate(isValid => {
        if (isValid) {
            post('/api/user/change-password', form, () => {
                logout(() => router.push('/'),
                    '重置密码成功,请重新登录')
            })
        }
    })
}

const simpleList = ref([])
if (store.isAdmin) {
    get('/api/monitor/simple-list', list => {
        simpleList.value = list
        initAccounts()
    })
}


const accounts = ref([])
const initAccounts = () => {
    get('api/user/sub/list', list => accounts.value = list)
}

function deleteSubAccount(uid) {
    ElMessageBox.confirm('确定要删除该子用户吗？删除后该子用户数据无法恢复', '删除该子用户', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: "warning"
    }).then(() => {
        get(`/api/user/sub/delete?uid=${uid}`, () => {
            ElMessage.success('成功删除该子用户')
            initAccounts()
        })
    }).catch(() => {
    })

}

const subAccountDetail = ref(false)
</script>

<template>
    <div style="display: flex;gap: 10px">
        <div style="flex: 50%">
            <div class="info-card">
                <div class="title">
                    <i class="fa-solid fa-lock"/>修改密码
                </div>
                <el-divider style="margin: 10px 0"/>
                <el-form @validate="onValidate" :model="form" label-width="100"
                         ref="formRef" :rules="rules">
                    <el-form-item prop="old_password" label="旧密码" style="margin-top: 20px">
                        <el-input v-model="form.old_password" type="password"
                                  :prefix-icon="Lock" maxlength="18" placeholder="旧密码"/>
                    </el-form-item>
                    <el-form-item prop="new_password" label="新密码">
                        <el-input v-model="form.new_password" type="password"
                                  :prefix-icon="Lock" maxlength="18" placeholder="新密码"/>
                    </el-form-item>
                    <el-form-item prop="new_password_repeat" label="重复新密码">
                        <el-input v-model="form.new_password_repeat" type="password"
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
                <div class="title">
                    <i class="fa-regular fa-envelope"/>修改电子邮件
                </div>
                <el-divider style="margin: 10px 0"/>
                <el-form @validate="onValidate" :model="emailForm" :rules="rules" label-position="top">
                    <el-form-item prop="email" label="电子邮件">
                        <el-input :prefix-icon="Message" type="text"
                                  v-model="emailForm.email" placeholder="输入邮件地址"/>
                    </el-form-item>
                    <el-form-item prop="code">
                        <el-row style="width: 100%" :gutter="10">
                            <el-col :span="18">
                                <el-input v-model="emailForm.code"
                                          type="text" maxlength="6" placeholder="验证码"/>
                            </el-col>
                            <el-col :span="6">
                                <el-button type="success" @click="validateEmail"
                                           :disabled="!isEmailValid || coldTime > 0">
                                    {{coldTime > 0 ? `在${coldTime}后再试` : '获取验证码'}}
                                </el-button>
                            </el-col>
                        </el-row>
                    </el-form-item>
                    <div>
                        <el-button type="warning" plain :icon="Refresh" :disabled="!emailForm.email"
                                   @click="modifyEmail">确定重置邮件</el-button>
                    </div>
                </el-form>
            </div>
        </div>
        <div class="info-card" style="flex: 50%">
            <div class="title">
                <i class="fa-solid fa-users"/>子用户管理
            </div>
            <el-divider style="margin: 10px 0"/>
            <div v-if="accounts.length" style="text-align: center">
                <div class="accounts-card" v-for="item in accounts">
                    <el-avatar :size="35"
                               src="https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png"/>
                    <div style="line-height: 18px;margin-left: 15px;flex: 1">
                        <div>
                            <span>{{ item.username }}</span>
                            <span style="font-size: 13px;color: grey;margin-left: 5px">
                                管理了{{ item.clientList.length }}个服务器
                            </span>
                        </div>
                        <div style="font-size: 13px;color: grey">
                            {{ item.email }}
                        </div>
                    </div>
                    <div>
                        <el-button size="small" type="danger" plain :icon="Delete"
                                   @click="deleteSubAccount(item.id)">
                            删除该子用户
                        </el-button>
                    </div>
                </div>
                <div style="margin-top: 18px">
                    <el-button :icon="Plus" type="primary" plain @click="subAccountDetail=true">
                        添加更多子用户
                    </el-button>
                </div>
            </div>
            <div v-else>
                <el-empty :image-size="100" description="还没有任何子用户哦" v-if="store.isAdmin">
                    <el-button :icon="Plus" type="primary" plain @click="subAccountDetail=true">添加子用户</el-button>
                </el-empty>
                <el-empty :image-size="100" description="子账户只能由管理员进行操作" v-else/>
            </div>

            <el-drawer v-model="subAccountDetail" :with-header="false" size="350">
                <create-sub-account :clients="simpleList"
                                    @create="subAccountDetail = false;initAccounts()"/>
            </el-drawer>
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

.accounts-card {
    border-radius: 5px;
    background-color: var(--el-bg-color-page);
    display: flex;
    padding: 10px;
    align-items: center;
    text-align: left;
    margin: 10px 0;
}

:deep(.el-drawer) {
    margin: 10px;
    height: calc(100% - 20px);
    border-radius: 10px;
}

:deep(.el-drawer-body) {
    padding: 0;
}
</style>