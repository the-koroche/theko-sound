package org.theko.sound.codec;

import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;

public class AnyPositionInputStream extends InputStream {
    
    private RandomAccessFile raf;
    private long position;

    public AnyPositionInputStream (RandomAccessFile raf) {
        this.raf = raf;
    }

    public byte[] read (long origin, int length) throws IOException {
        byte[] buffer = new byte[length];
        raf.seek(origin);
        raf.read(buffer, 0, length);
        return buffer;
    }

    @Override
    public int read () throws IOException {
        return read(position, 32)[0] & 0xFF;
    }
}
