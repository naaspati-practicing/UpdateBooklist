package sam.books;

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.READ;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static java.nio.file.StandardOpenOption.WRITE;
import static sam.console.ANSI.cyan;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.List;
import java.util.PrimitiveIterator.OfLong;
import java.util.function.Consumer;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import sam.collection.MappedIterator;
import sam.io.BufferConsumer;
import sam.io.BufferSupplier;
import sam.io.IOUtils;
import sam.io.serilizers.StringIOUtils;
import sam.nopkg.Junk;

class WrapSerializer {
	public void write(List<FileWrap> data, Path path) throws IOException {
		try(OutputStream _is = Files.newOutputStream(path, WRITE, TRUNCATE_EXISTING, CREATE);
				GZIPOutputStream gos = new GZIPOutputStream(_is);
				) {
			if(data.isEmpty()) {
				IOUtils.write(ByteBuffer.allocate(4).putInt(0), gos, true);
				return;
			}

			ByteBuffer buffer = ByteBuffer.allocate(4 * 1024);
			
			BitSet set = new BitSet(data.size());
			for (int i = 0; i < data.size(); i++) 
				set.set(i, data.get(i).isDir());
			
			buffer.putInt(data.size());
			long[] array = set.toLongArray();
			buffer.putInt(array.length);
			
			writeLongArray(new OfLong() {
				int n = 0;
				@Override
				public boolean hasNext() {
					return n < array.length;
				}
				@Override
				public long nextLong() {
					return array[n++];
				}
			}, buffer, gos);
			
			writeLongArray(new OfLong() {
				int n = 0;
				
				@Override
				public boolean hasNext() {
					return n < data.size();
				}
				@Override
				public long nextLong() {
					return data.get(n++).lastModified();
				}
			}, buffer, gos);

			IOUtils.write(buffer, gos, true);
			StringIOUtils.writeJoining(new MappedIterator<>(data.iterator(), w -> w.subpath().toString()), "\n", BufferConsumer.of(gos, false), buffer, null, null);
		}
	}

	private static void writeLongArray(OfLong data, ByteBuffer buffer, OutputStream os) throws IOException {
		while (data.hasNext()) {
			if(buffer.remaining() < Long.BYTES)
				IOUtils.write(buffer, os, true);
			buffer.putLong(data.nextLong());
		}
	}

	public List<FileWrap> read(Path path) throws IOException {
		if(Files.notExists(path))
			return null;

		ByteBuffer buffer = ByteBuffer.allocate(4 * 1024);

		try(InputStream _is = Files.newInputStream(path, READ);
				GZIPInputStream gos = new GZIPInputStream(_is);
				) {

			IOUtils.read(buffer, gos, true);
			
			if(buffer.remaining() < 4)
				return null;
			
			final int size = buffer.getInt();
			
			if(size == 0)
				return Collections.emptyList();
			if(size < 0)
				throw new IOException("size("+size+") < 0");

			System.out.println(cyan("reading cache"));

			BitSet isDir = BitSet.valueOf(longArray(buffer, gos, buffer.getInt()));
			long[] lastModified = longArray(buffer, gos, size);
			ArrayList<FileWrap> sink = new ArrayList<>(size + 10);
			
			System.out.println(size);

			Consumer<String> reader = new Consumer<String>() {
				int n = 0;
				@Override
				public void accept(String t) {
					Junk.notYetImplemented();
					// sink.add(new FileWrap(t, isDir.get(n), lastModified[n]));
					n++;
				}
			};

			BufferSupplier supplier = new BufferSupplier() {
				boolean first = true;
				int n = 0;

				@Override
				public ByteBuffer next() throws IOException {
					if(first) {
						first = false;
						return buffer;
					}
					
					IOUtils.compactOrClear(buffer);
					n = IOUtils.read(buffer, gos, true);
					return buffer;
				}
				@Override
				public boolean isEndOfInput() throws IOException {
					return n == -1;
				}
			};

			StringIOUtils.collect(supplier, '\n', reader, null, null, null);
			
			if(sink.size() != size)
				throw new IOException("sink.size("+sink.size()+") != size("+size+")");

			return sink;
		}
	}
	
	private static long[] longArray(ByteBuffer buffer, InputStream is, int size) throws IOException {
		long[] array = new long[size];
		for (int i = 0; i < size; i++) {
			readIf(buffer, is, Long.BYTES);
			array[i] = buffer.getLong();
		}
		return array;
	}

	private static void readIf(ByteBuffer buffer, InputStream gos, int bytes) throws IOException {
		if(buffer.remaining() < bytes) {
			IOUtils.compactOrClear(buffer);
			IOUtils.read(buffer, gos, true);
		}
	}
}
