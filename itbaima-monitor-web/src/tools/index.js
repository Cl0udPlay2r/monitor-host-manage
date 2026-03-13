import {useClipboard} from "@vueuse/core";
import {ElMessage, ElMessageBox} from "element-plus";
import {post} from "@/net/index.js";

function fitByUnit(value, unit) {
    const units = ['B', 'KB', 'MB', 'GB', 'TB', 'PB'];
    let index = units.indexOf(unit);
    while (((value < 1 && value !== 0) || value > 1024) && (index >= 0 || index <= units.length)) {
        if (value < 1024) {
            value *= 1024
            index--
        } else {
            value /= 1024
            index++
        }
    }
    return `${parseInt(value)}${units[index]}`
}

function percentageToStatus(percentage) {
    if (percentage < 50) {
        return 'success'
    } else if (percentage < 80) {
        return 'warning'
    } else {
        return 'exception'
    }
}

function cpuNameToImagePath(name) {
    if (name.indexOf('Apple') >= 0) {
        return 'Apple.png'
    } else if (name.indexOf('AMD') >= 0) {
        return 'AMD.png'
    } else {
        return 'Intel.png'
    }
}

function osNameToIcon(name) {
    if (name.indexOf('Ubuntu') >= 0) {
        return {icon: 'fa-ubuntu', color: '#db4c1a'}
    } else if (name.indexOf('CentOS') >= 0) {
        return {icon: 'fa-centos', color: '#9dcd30'}
    } else if (name.indexOf('macOS') >= 0) {
        return {icon: 'fa-apple', color: 'grey'}
    } else if (name.indexOf('Windows') >= 0) {
        return {icon: 'fa-windows', color: '#228fff'}
    } else if (name.indexOf('Debian') >= 0) {
        return {icon: 'fa-debian', color: '#a80836'}
    } else {
        return {icon: 'fa-linux', color: 'grey'}
    }
}

const {copy} = useClipboard()
const copyIP = (ip) => copy(ip).then(() => {
    ElMessage.success("IP已成功复制到剪切板")
})

function rename(name,id,after) {
    ElMessageBox.prompt('请确认新的服务器主机名称', '修改名称', {
        confirmButtonText: "确认",
        cancelButtonText: '取消',
        inputValue: name,
        inputPattern:
            /^[A-Za-z0-9_\u4e00-\u9fa5]{1,10}$/,
        inputErrorMessage: '名称中只能包含中英文字符、数字和下划线',
    }).then(({value}) => {
        post('api/monitor/rename', {
            id: id,
            name: value
        }, () => {
            ElMessage.success('主机名称已修改成功')
            after()
        })
    })
}

export {fitByUnit, percentageToStatus, cpuNameToImagePath, osNameToIcon, copyIP, rename};