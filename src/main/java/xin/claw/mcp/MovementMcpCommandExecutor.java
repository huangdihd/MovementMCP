package xin.claw.mcp;

import xin.bbtt.mcbot.command.Command;
import xin.bbtt.mcbot.command.CommandExecutor;

public class MovementMcpCommandExecutor extends CommandExecutor {

    @Override
    public void onCommand(Command command, String label, String[] args) {
        if (args.length == 0) {
            printUsage();
            return;
        }

        switch (args[0].toLowerCase()) {
            case "start" -> {
                MovementMcpPlugin.INSTANCE.startServer();
            }
            case "stop" -> {
                MovementMcpPlugin.INSTANCE.stopServer();
            }
            case "status" -> {
                boolean running = MovementMcpPlugin.INSTANCE.isRunning();
                System.out.println("[MovementMCP] Server " + (running
                        ? "is running on port " + MovementMcpPlugin.INSTANCE.getPort()
                        : "is not running"));
            }
            case "port" -> {
                if (args.length < 2) {
                    System.out.println("[MovementMCP] Current port: " + MovementMcpPlugin.INSTANCE.getPort());
                    return;
                }
                try {
                    int port = Integer.parseInt(args[1]);
                    MovementMcpPlugin.INSTANCE.setPort(port);
                    System.out.println("[MovementMCP] Port set to " + port + ". Use 'mcp restart' to apply.");
                } catch (NumberFormatException e) {
                    System.out.println("[MovementMCP] Invalid port number: " + args[1]);
                }
            }
            case "restart" -> {
                MovementMcpPlugin.INSTANCE.stopServer();
                MovementMcpPlugin.INSTANCE.startServer();
            }
            default -> printUsage();
        }
    }

    private void printUsage() {
        System.out.println("[MovementMCP] Usage: mcp <start|stop|status|restart|port <port>>");
    }
}
