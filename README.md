# MovementMCP

Xinbot 插件，通过 MCP (Model Context Protocol) SSE 协议暴露 XinClaw 的工具，让外部 AI 客户端可以控制 Minecraft 机器人。

## 安装

1. `mvn clean package`
2. 把 `target/MovementMCP-1.0-SNAPSHOT.jar` 放入 xinbot 的 `plugins/` 目录（需与 XinClaw 同级）
3. 启动 bot，插件自动开启 MCP server（默认端口 3000）

## 控制台指令

```
mcp start          # 启动 MCP server
mcp stop           # 停止 MCP server
mcp status         # 查看运行状态
mcp port <port>    # 修改端口（需 restart 生效）
mcp restart        # 重启
```

## MCP 连接

SSE endpoint: `http://localhost:3000/sse`

客户端配置示例（Claude Desktop 等）:

```json
{
  "mcpServers": {
    "movement": {
      "url": "http://localhost:3000/sse"
    }
  }
}
```

## 暴露的 Tools

| 分类 | 工具 |
|------|------|
| 移动 | `walkTo` `lookAt` `jump` `pathfindTo` `stopWalking` `getMovementStatus` `addIdleMovement` |
| 感知 | `scanSurroundings` `getAreaMap` `whereAmI` `getVitals` `getCurrentWorld` `getBlocksInCube` `findSpecificBlocks` `getNearbyEntities` |
| 动作 | `interactEntity` `changeSlot` `useItem` `useItemWithDuration` `releaseUseItem` `interactBlock` `mineBlock` |
| 社交 | `getBotOwner` `getRecentChat` `getPlayerList` `getNearbyPlayers` `followPlayer` `stopFollowing` `getPlayerNameByEntityId` |
| 背包 | `getInventory` `closeContainer` `clickInventorySlot` |
| 系统 | `sendChatMessage` `sendCommand` `getCommandCompletions` |

## 技术栈

- Java 17
- Netty (HTTP/SSE)
- MCP Java SDK 0.10.0
- 通过反射桥接 XinClaw 的 `@Tool` 注解

## 协议

GPLv3
