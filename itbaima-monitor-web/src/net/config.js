/**
 * 后端服务地址配置。
 * 原先 HTTP 基址硬编码在 main.js、WebSocket 地址硬编码在 Terminal.vue，
 * 部署到任何非本机环境都会失效（见测试记录 F8）。
 * 统一从这里取值，并支持用 .env 的 VITE_API_BASE 覆盖。
 */
const API_BASE = import.meta.env.VITE_API_BASE ?? 'http://localhost:8080'

/** WebSocket 基址：由 HTTP 基址推导，http→ws、https→wss */
export const WS_BASE = API_BASE.replace(/^http/, 'ws')

export default API_BASE
