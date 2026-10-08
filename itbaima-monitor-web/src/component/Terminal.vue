<script setup>

import {onBeforeUnmount, onMounted, ref} from "vue";
import {ElMessage} from "element-plus";
import {AttachAddon} from "xterm-addon-attach/src/AttachAddon.js";
import {Terminal} from "xterm";
import "xterm/css/xterm.css"
import {takeAccessToken} from "@/net/index.js";
import {WS_BASE} from "@/net/config.js";

const props = defineProps({
    id: Number
})

const emits = defineEmits(['dispose'])

const terminalRef = ref()

// 浏览器无法为 WebSocket 自定义请求头，故令牌经 Sec-WebSocket-Protocol 子协议字段传递，
// 由服务端 TerminalHandshakeConfigurator 在握手阶段完成 JWT 与主机权限校验
const token = takeAccessToken()
const socket = new WebSocket(`${WS_BASE}/terminal/${props.id}`, token ? ['bearer', token] : ['bearer'])
socket.onclose = evt => {
    if (evt.code !== 1000) {
        ElMessage.warning(`连接失败${evt.reason}`)
    } else {
        ElMessage.success("远程ssh连接已断开")
    }
    emits("dispose")
}
const attachAddon = new AttachAddon(socket)
const term = new Terminal({
    lineHeight: 1.2,
    rows: 20,
    fontSize: 13,
    fontFamily: "Monaco,Menlo,Consolas,'Courier New',monospace",
    fontWeight: 'bold',
    theme: {
        background: '#000000'
    },
    cursorBlink: true,
    cursorStyle: 'underline',
    scrollback: 100,
    tabStopWidth: 4
})
term.loadAddon(attachAddon)

// 终端尺寸同步：xterm 默认 80x20，而服务端未设置 PTY 尺寸时 JSch 会回退成 24x80，
// 两者不一致会让 top/vi 等全屏程序错行（见 F10）。以 NUL 开头的控制帧上报尺寸。
const syncTerminalSize = () => {
    if (socket.readyState === WebSocket.OPEN)
        socket.send(`\u0000resize:${term.cols},${term.rows}`)
}
socket.addEventListener('open', syncTerminalSize)
term.onResize(syncTerminalSize)

onMounted(() => {
    term.open(terminalRef.value)
    term.focus()
})

onBeforeUnmount(() => {
    socket.close()
    term.dispose()
})
</script>

<template>
    <div class="xterm" ref="terminalRef"></div>
</template>

<style scoped>

</style>