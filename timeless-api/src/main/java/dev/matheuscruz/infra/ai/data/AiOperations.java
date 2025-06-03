package dev.matheuscruz.infra.ai.data;

public enum AiOperations {
    ADD_TRANSACTION("ADD_TRANSACTION"), GET_BALANCE("GET_BALANCE");

    private final String commandName;

    AiOperations(String name) {
        this.commandName = name;
    }

    public String commandName() {
        return commandName;
    }
}
