package ai.dat.agent.agentic.tools.mcp;

public enum McpTransportType {
    STDIO("stdio"),
    HTTP("http");

    private final String value;

    McpTransportType(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return value;
    }
}