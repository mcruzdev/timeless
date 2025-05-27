package dev.matheuscruz.infra.ai.data;

public enum AiCommands {
    ADD_TRANSACTION("ADD_TRANSACTION"), GET_BALANCE("GET_BALANCE");

    private final String commandName;

    AiCommands(String name) {
        this.commandName = name;
    }

    public String commandName() {
        return commandName;
    }
}
