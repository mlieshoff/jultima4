package org.jultima4;

import java.io.IOException;
import java.util.Arrays;

/** Original 320x200, four-bit DOS title pictures, with fixed 12-bit hashed LZW codes. */
public final class EgaPicture {
    public static final int WIDTH = 320, HEIGHT = 200;
    private final byte[] pixels;
    private EgaPicture(byte[] pixels) { this.pixels = pixels; }

    public static EgaPicture load(String name) throws IOException {
        try { return decode(DosGameData.read(name + ".EGA")); }
        catch (IllegalArgumentException ex) { throw new IOException("Invalid picture: " + name, ex); }
    }

    public static EgaPicture decode(byte[] compressed) {
        return new EgaPicture(decompress(compressed, WIDTH * HEIGHT / 2));
    }

    public int rgba(int x, int y) {
        if (x < 0 || y < 0 || x >= WIDTH || y >= HEIGHT) throw new IllegalArgumentException("Invalid picture coordinate");
        int packed = Byte.toUnsignedInt(pixels[y * WIDTH / 2 + x / 2]);
        return EgaTiles.palette(x % 2 == 0 ? packed >>> 4 : packed & 15);
    }

    static byte[] decompress(byte[] compressed, int expectedSize) {
        if (compressed.length < 2 || compressed.length % 3 == 1 || expectedSize <= 0) throw new IllegalArgumentException("Invalid LZW input");
        byte[][] dictionary = new byte[4096][];
        int[] parents = new int[4096], suffixes = new int[4096];
        reset(dictionary);
        byte[] output = new byte[expectedSize];
        int bits = 0, written = 0, entries = 0;
        int previous = code(compressed, bits); bits += 12;
        if (previous > 255) throw new IllegalArgumentException("LZW must begin with a literal");
        output[written++] = (byte) previous;
        while (bits + 12 <= compressed.length * 8) {
            int current = code(compressed, bits); bits += 12;
            byte[] word = dictionary[current];
            boolean unknown = word == null;
            byte[] old = dictionary[previous];
            if (unknown) word = append(old, old[0]);
            if (word.length > output.length - written) throw new IllegalArgumentException("LZW picture exceeds expected size");
            System.arraycopy(word, 0, output, written, word.length); written += word.length;
            int suffix = Byte.toUnsignedInt(word[0]);
            int slot = slot(previous, suffix, dictionary, parents, suffixes);
            if (unknown && slot != current) throw new IllegalArgumentException("Invalid LZW dictionary reference");
            dictionary[slot] = append(old, word[0]); parents[slot] = previous; suffixes[slot] = suffix;
            previous = current;
            if (++entries > 0xccc) {
                reset(dictionary); entries = 0;
                if (bits + 12 > compressed.length * 8) break;
                previous = code(compressed, bits); bits += 12;
                if (previous > 255 || written == output.length) throw new IllegalArgumentException("Invalid LZW reset literal");
                output[written++] = (byte) previous;
            }
        }
        if (written != expectedSize) throw new IllegalArgumentException("Incomplete LZW picture");
        return output;
    }

    private static void reset(byte[][] dictionary) {
        Arrays.fill(dictionary, null);
        for (int i = 0; i < 256; i++) dictionary[i] = new byte[]{(byte) i};
    }
    private static byte[] append(byte[] word, byte suffix) {
        byte[] result = Arrays.copyOf(word, word.length + 1); result[word.length] = suffix; return result;
    }
    private static int code(byte[] bytes, int bits) {
        int pair = Byte.toUnsignedInt(bytes[bits / 8]) << 8 | Byte.toUnsignedInt(bytes[bits / 8 + 1]);
        return pair >>> (4 - bits % 8) & 0xfff;
    }
    private static boolean available(int slot, int parent, int suffix, byte[][] dictionary, int[] parents, int[] suffixes) {
        return slot > 255 && (dictionary[slot] == null || parents[slot] == parent && suffixes[slot] == suffix);
    }
    private static int slot(int parent, int suffix, byte[][] dictionary, int[] parents, int[] suffixes) {
        int slot = (suffix << 4 ^ parent) & 0xfff;
        if (available(slot, parent, suffix, dictionary, parents, suffixes)) return slot;
        long value = ((suffix << 1) + parent) | 0x800;
        long square = value * value;
        // Two rotations through the carry bit of the original 16-bit multiply result.
        long ring = square << 1 | (square >>> 16 == 0 ? 0 : 1);
        ring = (ring << 2 | ring >>> 31) & ((1L << 33) - 1);
        slot = (int) (ring >>> 9) & 0xfff;
        for (int probe = 0; probe < 4096; probe++) {
            if (available(slot, parent, suffix, dictionary, parents, suffixes)) return slot;
            slot = (slot + 0x1fd) & 0xfff;
        }
        throw new IllegalArgumentException("Full LZW dictionary");
    }
}
