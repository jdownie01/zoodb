/*
 * Copyright 2009-2020 Tilmann Zaeschke
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.zoodb.internal.server;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.function.ToIntFunction;

import org.zoodb.internal.util.DBLogger;

/**
 * This class manages all IO channels for a one session.
 * 
 * 
 * @author Tilmann Zaeschke
 *
 */
public final class StorageChannelImpl implements StorageChannel, IOResourceProvider {

	public static final int POOL_SIZE_READER = 1;
	
	//Reader pool without auto paging
	//The reader pool is multithreaded, because session allow multi-threaded reading.
	//TODO just use an array list and 'synchronized' or a Lock. 
	private final ArrayBlockingQueue<StorageChannelInput> readerPoolAPFalse = 
			new ArrayBlockingQueue<>(POOL_SIZE_READER, false);
	private final ArrayList<StorageChannelInput> viewsIn = new ArrayList<>();
	private final ArrayList<StorageChannelOutput> viewsOut = new ArrayList<>();
	private final StorageChannelOutput privateIndexWriter;
	
	private final StorageRoot root;
	private long txId;
	private boolean isClosed = false;

	public StorageChannelImpl(StorageRoot root) {
		this.root = root;
		for (int i = 0; i < POOL_SIZE_READER; i++) {
			readerPoolAPFalse.add(new StorageReader(this, false));
		}
		privateIndexWriter = new StorageWriter(this, false);
		viewsOut.add(privateIndexWriter);
	}

	@Override
	public long getTxId() {
		return this.txId;
	}
	
	@Override
	public final void close() {
		if (isClosed) {
			DBLogger.LOGGER.warn("Session is already closed.");
			return;
		}
		isClosed = true;
		flush();
		root.close(this);
	}

	@Override
	public StorageChannelInput getInputChannel() {
		try {
			return readerPoolAPFalse.take();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
		return null;
	}

	@Override
	public void returnInputChannel(StorageChannelInput in) {
		readerPoolAPFalse.add(in);
	}

	@Override
	public final StorageChannelInput createReader(boolean autoPaging) {
		StorageChannelInput in = new StorageReader(this, autoPaging);
		viewsIn.add(in);
		return in;
	}
	
	@Override
	public final void dropReader(StorageChannelInput in) {
		if (!viewsIn.remove(in)) {
			throw new IllegalArgumentException();
		}
	}
	
	@Override
	public final StorageChannelOutput createWriter(boolean autoPaging) {
		StorageChannelOutput out = new StorageWriter(this, autoPaging);
		viewsOut.add(out);
		return out;
	}
	
	/**
	 * Not a true flush, just writes the stuff...
	 */
	@Override
	public final void flush() {
		flushNoForce();
		root.force();
	}

	public void flushNoForce() {
		//flush associated splits.
		for (StorageChannelOutput paf: viewsOut) {
			//flush() only writers
			paf.flush();
		}
		for (StorageChannelInput paf: viewsIn) {
			paf.reset();
		}
	}

	@Override
	public int getNextPage(int prevPage) {
		return root.getNextPage(prevPage);
	}

	@Override
	public final void readPage(ByteBuffer buf, long pageId) {
		root.readPage(buf, pageId);
	}

	@Override
	public final void write(ByteBuffer buf, long pageId) {
		root.write(buf, pageId);
	}

	@Override
	@Deprecated //use root.xyz() 
	public final int statsGetReadCount() {
		return root.statsGetReadCount();
	}

	@Override
	@Deprecated //use root.xyz() 
	public int statsGetReadCountUnique() {
		return root.statsGetReadCountUnique();
	}

	@Override
	@Deprecated //use root.xyz() 
	public final int statsGetWriteCount() {
		return root.statsGetWriteCount();
	}

	@Override
	@Deprecated //use root.xyz() 
	public final int getPageSize() {
		return root.getPageSize();
	}

	@Override
	public void reportFreePage(int pageId) {
		root.reportFreePage(pageId);
	}

	@Override
	@Deprecated //use root.xyz() 
	public int statsGetPageCount() {
		return root.statsGetPageCount();
	}

	@Override
	public int writeIndex(ToIntFunction<StorageChannelOutput> writer) {
		synchronized (writer) {
			return writer.applyAsInt(privateIndexWriter);
		}
	}

	@Override
	public void startWriting(long txId) {
		this.txId = txId;
	}

	/**
	 * This is used in the zoodb-server-btree tests.
	 */
	@Override
	public boolean debugIsPageIdInFreeList(int pageId) {
		return root.debugIsPageIdInFreeList(pageId);
	}
}
