import { ElMessage } from 'element-plus'

/**
 * 统一错误处理: 从 Axios 错误响应中提取后端返回的 message,并显示 ElMessage。
 *
 * 用法:
 *   catch (e) { showError(e, '加载失败') }
 *
 * @param {Error} e - 捕获的错误(通常是 AxiosError)
 * @param {string} fallback - 后端未返回 message 时的默认提示
 */
export function showError(e, fallback = '操作失败') {
  const msg = e.response?.data?.message || e.message || fallback
  ElMessage.error(msg)
}

/**
 * 统一成功提示。
 */
export function showSuccess(msg) {
  ElMessage.success(msg)
}

/**
 * 统一警告提示。
 */
export function showWarning(msg) {
  ElMessage.warning(msg)
}
