# MovementMCP

Xinbot 插件，通过 MCP SSE 协议暴露工具，让 AI 客户端控制 Minecraft 机器人。

与 XinClaw 无关，直接使用 MovementSync + Bot API。

## 安装

```bash
mvn clean package
cp target/MovementMCP-1.0-SNAPSHOT.jar /path/to/xinbot/plugins/
```

## 控制台指令

```
mcp start          # 启动 MCP server
mcp stop           # 停止
mcp status         # 查看状态
mcp port <n>       # 修改端口（需 restart）
mcp restart        # 重启
```

## MCP 连接

SSE endpoint: `http://localhost:6735/sse`

Claude Desktop 配置：

```json
{
  "mcpServers": {
    "movement": {
      "url": "http://localhost:6735/sse"
    }
  }
}
```

## 工具列表

**移动**: walkTo, lookAt, jump, pathfindTo, stopWalking, getMovementStatus, addIdleMovement

**感知**: whereAmI, scanSurroundings, getAreaMap, getNearbyEntities, getBlocksInCube, findSpecificBlocks

**动作**: interactEntity, changeSlot, useItem, useItemWithDuration, releaseUseItem, interactBlock, mineBlock

**社交**: getPlayerList, getNearbyPlayers, followPlayer, stopFollowing

**系统**: sendChatMessage, sendCommand

## 技术栈

Java 17 · MCP SDK 0.10.0 · Netty SSE · MovementSync · xinbot

## 协议

GPLv3
