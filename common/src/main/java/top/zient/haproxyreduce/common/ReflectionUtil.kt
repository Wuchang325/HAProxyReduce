package top.zient.haproxyreduce.common

import java.lang.reflect.Method

object ReflectionUtil {
    @Suppress("UNCHECKED_CAST")
    inline fun <reified T> getField(obj: Any, fieldName: String): T {
        val field = obj.javaClass.getDeclaredField(fieldName).apply {
            try {
                isAccessible = true
            } catch (e: Exception) {
                throw IllegalStateException("无法访问字段，请添加 JVM 参数: --add-opens java.base/java.lang.reflect=ALL-UNNAMED", e)
            }
        }
        return field.get(obj) as T
    }

    fun findMethod(clazz: Class<*>, name: String, vararg paramTypes: Class<*>): Method {
        return clazz.getDeclaredMethod(name, *paramTypes).apply {
            isAccessible = true
        }
    }
}
