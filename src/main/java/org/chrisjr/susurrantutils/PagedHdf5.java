package org.chrisjr.susurrantutils;

import java.util.AbstractList;
import java.util.Iterator;

import ch.systemsx.cisd.base.mdarray.MDFloatArray;
import ch.systemsx.cisd.hdf5.*;

public class PagedHdf5 extends AbstractList<float[][]>{
	IHDF5Reader reader;
	IHDF5FloatReader readerFloat;
	String path;
	int blockSizeX = -1, blockSizeY; 
	long rowBlocks;
	
	public PagedHdf5(IHDF5Reader reader, String path) {
		this.reader = reader;
		this.readerFloat = reader.float32();
		this.path = path;
		Iterator<HDF5MDDataBlock<MDFloatArray>> it = readerFloat.getMDArrayNaturalBlocks(path).iterator();
		while (it.hasNext()) {
			HDF5MDDataBlock<MDFloatArray> block = it.next();
			if (blockSizeX == -1) {
				MDFloatArray a = block.getData();
				int[] dimensions = a.dimensions();
				blockSizeX = dimensions[0];
				blockSizeY = dimensions[1];
//				System.out.format("%d x %d\n", blockSizeX, blockSizeY);
			}
			rowBlocks = block.getIndex()[0] + 1;
		}
	}

	@Override
	public float[][] get(int index) {
		if (index < 0 || index >= size())
			throw new IndexOutOfBoundsException();
 		return readerFloat.readMatrixBlock(path, blockSizeX, blockSizeY, index, 0);
	}

	@Override
	public int size() {
		return (int) rowBlocks;
	}
}
