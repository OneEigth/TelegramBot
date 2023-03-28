package oneEight.service.enums;

public enum LinkType {
    GET_DOC("file/doc"),
    GET_PHOTO("file/photo");
    private final String link;

    LinkType(String link) {
        this.link = link;
    }

    @Override
    public String toString() {
        return link;
    }
}
