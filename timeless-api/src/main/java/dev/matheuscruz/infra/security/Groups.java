package dev.matheuscruz.infra.security;

public enum Groups {
    USER("USER");

    private final String group;

    Groups(String group) {
        this.group = group;
    }

    public String groupName() {
        return group;
    }
}
