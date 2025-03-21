package org.theko.sound.codec;

public class AudioTag {
    protected String key;
    protected String value;

    public static final String TITLE = "Title";
    public static final String ALBUM = "Album";
    public static final String ARTIST = "Artist";
    public static final String YEAR = "Year";
    public static final String TRACK = "Track";
    public static final String COMMENT = "Comment";
    public static final String GENRE = "Genre";
    public static final String ENGINEER = "Engineer";
    public static final String WEBSITE = "SRC";
    public static final String SOURCE = "SRC";
    public static final String SOFTWARE = "Software";
    public static final String TECHNICIAN = "Technician";

    public AudioTag (String key, String value) {
        this.key = key;
        this.value = value;
    }

    public String getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return "AudioTag {" + key + ": " + value + "}";
    }
    
    /*private String mapInfoIdToTag(String id) {
        switch (id) {
            case "INAM": return "Title";
            case "IART": return "Artist";
            case "IPRD": return "Album";
            case "ICRD": return "Year"; // Для даты можно использовать "Year"
            case "ITRK": return "Track";
            case "ICMT": return "Comment";
            case "IGNR": return "Genre";
            case "IENG": return "Engineer";  // Добавлено
            case "ISRC": return "SRC";      // Добавлено
            case "ICOP": return "Copyright"; // Добавить поддержку авторских прав
            case "ISFT": return "Software"; // Информация о программе (например, FL Studio)
            case "ITCH": return "Technician"; 
            case "ALBM": return "Album";   // Добавление вариаций
            case "TITL": return "Title";   // Добавить титул (в случае вариации)
            default: return id.startsWith("I") ? id.substring(1) : null;
        }
    }*/
}
