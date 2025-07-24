package top.zient.haproxyreduce.common

import java.lang.reflect.Method

object ReflectionUtil {
    @Suppress("UNCHECKED_CAST")
    inline fun <reified T> getField(obj: Any, fieldName: String): T {
        val field = obj.javaClass.getDeclaredField(fieldName).apply {
            isAccessible = true
        }
        return field.get(obj) as T
    }

    fun findMethod(clazz: Class<*>, name: String, vararg paramTypes: Class<*>): Method {
        return clazz.getDeclaredMethod(name, *paramTypes).apply {
            isAccessible = true
        }
    }
}