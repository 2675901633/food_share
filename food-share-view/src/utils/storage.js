const TOKEN_KEY = "token"
/**
 * 获取Token
 */
export function getToken() {
    return localStorage.getItem(TOKEN_KEY);
}
/**
 * 设置Token
 */
export function setToken(token) {
    localStorage.setItem(TOKEN_KEY, token);
}
/**
 * 清除Token
 */
export function clearToken() {
    localStorage.removeItem(TOKEN_KEY);
}