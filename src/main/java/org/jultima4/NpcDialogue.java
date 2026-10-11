package org.jultima4;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** One 288-byte TLK record; strings are bounded by the current record. */
public record NpcDialogue(int questionTrigger, List<String> fields) {
    public NpcDialogue {
        fields = List.copyOf(fields);
        if (fields.size() != 12) throw new IllegalArgumentException("Expected twelve dialogue fields");
    }

    public static List<NpcDialogue> decode(byte[] data) {
        if (data.length != 16 * 288) throw new IllegalArgumentException("TLK must contain sixteen 288-byte records");
        List<NpcDialogue> result = new ArrayList<>();
        for (int record = 0; record < 16; record++) {
            int position = record * 288 + 3, end = (record + 1) * 288;
            List<String> fields = new ArrayList<>();
            for (int field = 0; field < 12; field++) {
                int start = position;
                while (position < end && data[position] != 0) position++;
                if (position == end) throw new IllegalArgumentException("Unterminated TLK string in record " + record);
                fields.add(new String(data, start, position - start, StandardCharsets.ISO_8859_1));
                position++;
            }
            result.add(new NpcDialogue(Byte.toUnsignedInt(data[record * 288]), fields));
        }
        return List.copyOf(result);
    }

    public String name() { return fields.get(0); }
    public String description() { return fields.get(2); }
    public String question() { return fields.get(7); }
    public String answer(boolean yes) { return fields.get(yes ? 8 : 9); }
    public String keyword(int index) { return fields.get(10 + index); }

    public int topic(String text) {
        String input = prefix(text);
        if (input.equals("name")) return 0;
        if (input.equals("look")) return 2;
        if (input.equals("job")) return 3;
        if (input.equals("heal")) return 4;
        if (!keyword(0).isBlank() && input.equals(prefix(keyword(0)))) return 5;
        if (!keyword(1).isBlank() && input.equals(prefix(keyword(1)))) return 6;
        return -1;
    }

    public String response(int topic) {
        return switch (topic) {
            case 0 -> name();
            case 2 -> description();
            case 3 -> fields.get(3);
            case 4 -> fields.get(4);
            case 5 -> fields.get(5);
            case 6 -> fields.get(6);
            default -> "I cannot help thee with that.";
        };
    }

    private static String prefix(String text) {
        String value = text.strip().toLowerCase(Locale.ROOT);
        return value.substring(0, Math.min(4, value.length()));
    }
}
