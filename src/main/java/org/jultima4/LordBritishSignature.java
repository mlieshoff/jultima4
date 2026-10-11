package org.jultima4;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Original ordered two-pixel pen strokes stored in the DOS title executable. */
public record LordBritishSignature(List<Point> points) {
    public record Point(int x, int y) { }
    public LordBritishSignature { points = List.copyOf(points); }

    public static LordBritishSignature load() throws IOException {
        byte[] executable = DosGameData.read("TITLE.EXE");
        if (executable.length < 29806 + 533) throw new IOException("TITLE.EXE has no signature table");
        try { return decode(Arrays.copyOfRange(executable, 29806, 29806 + 533)); }
        catch (IllegalArgumentException ex) { throw new IOException("Invalid Lord British signature", ex); }
    }

    public static LordBritishSignature decode(byte[] table) {
        if (table.length != 533) throw new IllegalArgumentException("Signature table must contain 533 bytes");
        List<Point> points = new ArrayList<>();
        for (int i = 0; i < table.length; i += 2) {
            if (table[i] == 0) {
                if (points.isEmpty()) throw new IllegalArgumentException("Empty signature");
                return new LordBritishSignature(points);
            }
            if (i + 1 == table.length) break;
            int x = Byte.toUnsignedInt(table[i]) - 0x4c, y = 0xc0 - Byte.toUnsignedInt(table[i + 1]);
            if (x < 0 || x > 129 || y < 0 || y > 16) throw new IllegalArgumentException("Invalid signature point");
            points.add(new Point(x, y));
        }
        throw new IllegalArgumentException("Unterminated signature table");
    }
}
