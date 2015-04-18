package org.chrisjr.susurrantutils;

import java.util.Iterator;

import ch.systemsx.cisd.hdf5.IHDF5Reader;
import ch.systemsx.cisd.hdf5.HDF5MDDataBlock;
import ch.systemsx.cisd.base.mdarray.MDFloatArray;

public class Hdf5FloatIterator implements Iterator<float[]> {
	public class MDFloatIterator implements Iterator<float[]> {
		MDFloatArray floats;
		final int sizeX;
		final int sizeY;
		int i = 0;
		
		public MDFloatIterator(MDFloatArray floats) {
			this.floats = floats;

			int[] dimensions = floats.dimensions();
	        sizeX = dimensions[0];
	        sizeY = dimensions[1];
		}

		@Override
		public boolean hasNext() {
			return i < sizeX;
		}

		@Override
		public float[] next() {
			float[] result = new float[sizeY];
			System.arraycopy(floats.getAsFlatArray(), i++ * sizeY, result, 0, sizeY);
			return result;
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException("HDF5 iterator is read only.");
		}
		
	}
	
	IHDF5Reader reader;
	Iterator<HDF5MDDataBlock<MDFloatArray>> inner;
	Iterator<float[]> current = null;
	
	public Hdf5FloatIterator(IHDF5Reader reader, String dataset) {
		this.reader = reader;
		this.inner = this.reader.float32().getMDArrayNaturalBlocks(dataset).iterator();
	}
	
	@Override
	public boolean hasNext() {
		if (current == null || !current.hasNext()) {
			return this.inner.hasNext();
		} else {
			return true;
		}
	}

	@Override
	public float[] next() {
		if (current == null || !current.hasNext()) {
			current = new MDFloatIterator(this.inner.next().getData());
		}
		return current.next();
	}

	@Override
	public void remove() {
		throw new UnsupportedOperationException("HDF5 iterator is read only.");
	}
}
