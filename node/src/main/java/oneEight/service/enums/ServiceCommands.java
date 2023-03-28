package oneEight.service.enums;

public enum ServiceCommands {
    HELP("/help"),
    REGISTRATION("/registration"),
    CANCEL("/cancel"),
    START("/start");
    private final String value;

    ServiceCommands(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return value;
    }

    public static ServiceCommands fromString(String v) {
        for (ServiceCommands c : ServiceCommands.values()) {
            if (c.value.equalsIgnoreCase(v)) {
                return c;
            }
        }
        return null;
    }
}
