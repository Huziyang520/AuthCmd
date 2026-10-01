package com.authcmd.mod.event;

import com.mojang.logging.LogUtils;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;

/**
 * 向客户端重发命令树（平台无关，纯 common 模块）。
 *
 * <p>补全树（{@code ClientboundCommandsPacket}）由服务端在发送那一刻按各节点
 * {@code canUse(source)} 过滤生成。{@link CommandTreeLogic} 已把白名单命令节点的
 * requirement 换成动态谓词，因此 vanilla 的 {@code Commands.sendCommands} 天然会
 * 产出含白名单命令的补全树——本类只需在配置变更、玩家登录等时机重新触发它。
 *
 * <p><b>为什么不自建命令树</b>：早期版本手工复制一棵树并把白名单节点的 requirement 设为
 * {@code s -> true}，作为反射失效时的兜底。该做法有两个无法回避的问题：
 * <ul>
 *   <li>发包必须拿到 {@code ServerPlayer.connection}。若用字符串反射字段名，在正式环境会
 *       因混淆（Forge=SRG、Fabric=intermediary）必然失败，"兜底"只在 dev 有效；</li>
 *   <li>手工复制漏掉了 vanilla {@code fillUsableCommands} 中的 {@code createBuilder()} 与
 *       {@code SuggestionProviders.safelySwap()}——后者负责把未注册的 suggestion provider
 *       替换为 {@code ASK_SERVER}，缺失会导致客户端补全行为不正确；且 redirect 目标一旦被
 *       过滤掉，别名节点的 redirect 会永久留空。</li>
 * </ul>
 * 直接调用 vanilla 可一并规避，且 {@code player.connection} 由编译期符号引用，
 * 两个加载器的 remapper 都会正确处理。
 */
public final class ClientTreeResender {

    private static final Logger LOGGER = LogUtils.getLogger();

    private ClientTreeResender() {
    }

    /** 为指定玩家重发客户端命令树（含白名单指令的补全）。 */
    public static void resend(ServerPlayer player) {
        if (player == null) return;
        MinecraftServer server = player.level().getServer();
        if (server == null) {
            LOGGER.warn("[AuthCmd] ClientTreeResender.resend: null server, skipping");
            return;
        }
        server.getCommands().sendCommands(player);
        LOGGER.debug("[AuthCmd] Command tree resent to {}", player.getName().getString());
    }

    /** 为全部在线玩家重发客户端命令树。 */
    public static void resendAll(MinecraftServer server) {
        if (server == null) return;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            resend(player);
        }
        LOGGER.info("[AuthCmd] Command tree resent to {} player(s)", server.getPlayerList().getPlayers().size());
    }
}
