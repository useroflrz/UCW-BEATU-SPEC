package com.ucw.beatu.shared.common.reflect

import com.ucw.beatu.shared.common.logger.AppLogger

/**
 * 统一反射桥接能力：
 * - 通过 Companion 调用工厂方法创建对象
 * - 安全调用对象实例方法
 * - 统一输出兜底日志，避免各模块重复实现反射逻辑
 */
object ReflectionBridge {

    private const val TAG = "ReflectionBridge"

    fun <T : Any> createByCompanion(
        className: String,
        methodName: String,
        returnType: Class<T>,
        caller: String,
        vararg args: Any?
    ): T? {
        return runCatching {
            val targetClass = Class.forName(className)
            val companion = targetClass.getDeclaredField("Companion").get(null)
            val method = companion.javaClass.methods.firstOrNull { candidate ->
                candidate.name == methodName && isCompatible(candidate.parameterTypes, args)
            } ?: throw NoSuchMethodException("$className.Companion#$methodName")

            val value = method.invoke(companion, *args)
            if (value == null) {
                AppLogger.w(TAG, "[$caller] 反射返回空对象: $className#$methodName")
                null
            } else if (returnType.isInstance(value)) {
                returnType.cast(value)
            } else {
                AppLogger.e(
                    TAG,
                    "[$caller] 反射返回类型不匹配: expected=${returnType.name}, actual=${value.javaClass.name}"
                )
                null
            }
        }.onFailure { throwable ->
            AppLogger.e(TAG, "[$caller] 反射创建失败: $className#$methodName", throwable)
        }.getOrNull()
    }

    fun invokeIfExists(
        target: Any,
        methodName: String,
        caller: String,
        vararg args: Any?
    ): Boolean {
        val method = target.javaClass.methods.firstOrNull { candidate ->
            candidate.name == methodName && isCompatible(candidate.parameterTypes, args)
        } ?: return false

        return runCatching {
            method.invoke(target, *args)
            true
        }.onFailure { throwable ->
            AppLogger.w(
                TAG,
                "[$caller] 反射调用失败: ${target.javaClass.name}#$methodName",
                throwable
            )
        }.getOrDefault(false)
    }

    private fun isCompatible(parameterTypes: Array<Class<*>>, args: Array<out Any?>): Boolean {
        if (parameterTypes.size != args.size) return false
        return parameterTypes.indices.all { index ->
            val paramType = wrapPrimitive(parameterTypes[index])
            val arg = args[index]
            arg == null || paramType.isAssignableFrom(arg.javaClass)
        }
    }

    private fun wrapPrimitive(type: Class<*>): Class<*> {
        if (!type.isPrimitive) return type
        return when (type) {
            java.lang.Boolean.TYPE -> java.lang.Boolean::class.java
            java.lang.Byte.TYPE -> java.lang.Byte::class.java
            java.lang.Short.TYPE -> java.lang.Short::class.java
            java.lang.Integer.TYPE -> java.lang.Integer::class.java
            java.lang.Long.TYPE -> java.lang.Long::class.java
            java.lang.Float.TYPE -> java.lang.Float::class.java
            java.lang.Double.TYPE -> java.lang.Double::class.java
            java.lang.Character.TYPE -> java.lang.Character::class.java
            else -> type
        }
    }
}
