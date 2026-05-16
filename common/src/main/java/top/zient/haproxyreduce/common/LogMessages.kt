package top.zient.haproxyreduce.common

/**
 * 统一的日志消息常量
 */
object LogMessages {
    // 插件启动相关
    const val PLUGIN_ENABLED = "HAProxyReduce 已启用"
    const val PLUGIN_DISABLED = "HAProxyReduce 已禁用"
    const val SERVER_ENVIRONMENT_DETECTED = "检测到服务端环境: {}"
    const val CONFIG_LOADED = "配置文件已加载，白名单模式: {}"
    const val CONFIG_LOAD_FAILED = "加载配置文件失败，插件将禁用"
    const val PROXY_PROTOCOL_NOT_ENABLED = "HAProxy 支持未启用，插件已自动禁用"
    const val PROXY_PROTOCOL_NOT_ENABLED_ERROR = PROXY_PROTOCOL_NOT_ENABLED
    const val PROTOCOL_LIB_NOT_FOUND = "未找到 ProtocolLib 插件，请安装 ProtocolLib 插件"
    const val UNSUPPORTED_SERVER_TYPE = "不支持的服务器类型: {}"
    const val PAPER_CHANNEL_INIT_FAILED = "Paper 通道初始化失败"
    const val CHANNEL_INJECTION_FAILED = "通道处理器注入失败，插件无法正常工作"
    const val INITIALIZATION_FAILED = "插件初始化失败"
    const val SERVER_STARTUP_COMPLETE = "服务器启动完成，HAProxyReduce 运行中"

    // 连接相关
    const val CONNECTION_ASSOCIATED = "连接已关联: {} -> {}"
    const val PLAYER_CONNECTED_DIRECT = "玩家直接连接: {} ({})"
    const val PLAYER_DISCONNECTED = "玩家已断开连接: {}"
    const val LOGIN_LISTENER_REGISTERED = "登录监听器已注册"

    // 插件注入相关
    const val CHANNEL_INITIALIZER_INJECTED_SUCCESS = "通道初始化器注入成功"
    const val INJECTION_FAILED = "注入失败"

    // 配置相关
    const val WHITELIST_MODE_EMPTY_DENY_ALL_WARNING = "当前模式为 EMPTY_DENY_ALL 且 IP 列表为空，所有 HAProxy 连接将被拒绝（使用原始IP）"
    const val WHITELIST_RULES_LOADED = "已加载 {} 个白名单规则"
    const val BLACKLIST_ENABLED = "黑名单已启用，已加载 {} 个黑名单规则"
    const val BLACKLIST_DISABLED = "黑名单已禁用"
    const val WHITELIST_MODE_EMPTY_ALLOW_ALL = "当前模式为 EMPTY_ALLOW_ALL，空列表将允许所有 HAProxy 连接"
    const val WHITELIST_MODE_DISABLED = "当前模式为 DISABLED，白名单检查已禁用，允许所有 HAProxy 连接"

    // 热重载相关
    const val HOT_RELOAD_ENABLED = "配置热重载已启用，检查间隔: {}秒"
    const val HOT_RELOAD_DISABLED = "配置热重载已禁用"
    const val HOT_RELOAD_STOPPED = "配置热重载已停止"
    const val CONFIG_CHANGE_DETECTED = "检测到配置文件变更，开始重载..."
    const val CONFIG_RELOAD_COMPLETE = "配置重载完成"
    const val CONFIG_RELOAD_FAILED = "配置重载失败"
    const val CONFIG_FILE_NOT_EXISTS = "配置文件不存在: {}"

    // 连接相关
    const val CONNECTION_LOG = "player({}:{} -> {}:{})"
    const val CONNECTION_TRACKED = "记录待处理连接: {} -> {}"
    const val CONNECTION_NOT_FOUND = "未找到连接记录: {}"
    const val CONNECTION_RECORDED = "记录连接: {}:{} -> {} (channel: {})"

    // 协议检测相关
    const val PROTOCOL_VERSION = "协议版本: {}"
    const val PROTOCOL_COMMAND = "命令: {}"
    const val SOURCE_ADDRESS = "源地址: {}:{}"
    const val DESTINATION_ADDRESS = "目标地址: {}:{}"
    const val PROXY_CONNECTION_NOT_WHITELISTED = "代理连接未在白名单中: {}"
    const val HAPROXY_PROTOCOL_ENABLED = "HAProxy 协议已启用：{}"
    const val PROXY_DETECTION_EXCEPTION = "HAProxy 协议检测异常"

    // 错误相关
    const val PROXY_PROTOCOL_DETECTION_FAILED = "检测 proxy-protocol 配置失败，尝试备用方法"
    const val PROXY_PROTOCOL_READ_FAILED = "读取配置文件检测 proxy-protocol 失败"
    const val CHANNEL_INIT_FAILED = "初始化通道失败"
    const val HANDLER_FIELD_NULL = "handlerField 为 null"
    const val CHANNEL_INJECTION_SUCCESS = "通道处理器注入成功"
    const val CHANNEL_CLOSED_SKIP_INJECTION = "通道已关闭，跳过注入"
    const val DETECTOR_ALREADY_EXISTS = "HAProxy 检测器已存在，跳过重复注入"
    const val DECODER_NOT_FOUND = "未找到 HAProxyMessageDecoder，请确保 Velocity 配置中启用了 proxy-protocol"
    const val DECODER_REPLACEMENT_FAILED = "替换 HAProxy 解码器失败"
    const val DETECTOR_INJECTION_SUCCESS = "成功注入 HAProxy 检测器"
    const val CONNECTION_LOG_EXCEPTION = "连接日志处理异常"
    const val HAPROXY_CONNECTION_LOG_EXCEPTION = "HAProxy 连接日志处理异常"
    const val FILE_MODIFICATION_TIME_ERROR = "无法获取配置文件修改时间"
}
