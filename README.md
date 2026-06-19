# MovementMCP

English | [简体中文](README.zh-CN.md)

**Drive a Minecraft bot with any MCP client.** MovementMCP is an [Xinbot](https://github.com/huangdihd/xinbot) plugin that runs an [MCP](https://modelcontextprotocol.io) server over SSE, exposing a Minecraft bot's movement, perception, and actions as tools. Point Claude (or any MCP-capable agent) at it and let the model walk, mine, scan its surroundings, follow players, and chat — on real survival/anarchy servers, in vanilla physics.

It is the bridge between a large language model and an in-game character: the LLM sees the world through the perception tools and acts through the movement/action tools, with no custom glue code.

## Requirements

- Java 17+
- A running [Xinbot](https://github.com/huangdihd/xinbot) instance (with a connected bot)
- [MovementSync](https://github.com/huangdihd/movementsync) plugin (provides the vanilla-physics movement engine)

## Install

```bash
mvn clean package
cp target/MovementMCP-1.0-SNAPSHOT.jar /path/to/xinbot/plugins/
```

Restart Xinbot (or reload plugins) to load it.

## Console commands

```
mcp start          # start the MCP server
mcp stop           # stop it
mcp status         # show status
mcp port <n>       # change port (requires restart)
mcp restart        # restart
```

## Connect an MCP client

SSE endpoint: `http://localhost:6735/sse`

Claude Desktop (`claude_desktop_config.json`):

```json
{
  "mcpServers": {
    "movement": {
      "url": "http://localhost:6735/sse"
    }
  }
}
```

Any MCP client that supports an SSE transport works the same way — just point it at the endpoint above.

## Tools

| Category | Tools |
| --- | --- |
| **Movement** | `walkTo`, `lookAt`, `jump`, `pathfindTo`, `stopWalking`, `getMovementStatus`, `addIdleMovement` |
| **Perception** | `whereAmI`, `scanSurroundings`, `getAreaMap`, `getNearbyEntities`, `getBlocksInCube`, `findSpecificBlocks` |
| **Action** | `interactEntity`, `changeSlot`, `useItem`, `useItemWithDuration`, `releaseUseItem`, `interactBlock`, `mineBlock` |
| **Social** | `getPlayerList`, `getNearbyPlayers`, `followPlayer`, `stopFollowing` |
| **System** | `sendChatMessage`, `sendCommand` |

## Tech stack

Java 17 · MCP SDK 0.10.0 · Netty SSE · [MovementSync](https://github.com/huangdihd/movementsync) · [Xinbot](https://github.com/huangdihd/xinbot)

## Related projects

- [Xinbot](https://github.com/huangdihd/xinbot) — the modular Minecraft bot framework this plugs into
- [XinClaw](https://github.com/huangdihd/xinclaw) — an autonomous in-game LLM agent for Xinbot, if you want the agent loop built in rather than driving the tools yourself

## License

GPLv3
