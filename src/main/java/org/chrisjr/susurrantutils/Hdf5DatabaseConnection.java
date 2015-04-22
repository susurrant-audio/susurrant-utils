package org.chrisjr.susurrantutils;

import java.io.File;
import java.util.List;

import de.lmu.ifi.dbs.elki.data.FloatVector;
import de.lmu.ifi.dbs.elki.data.type.VectorFieldTypeInformation;
import de.lmu.ifi.dbs.elki.datasource.AbstractDatabaseConnection;
import de.lmu.ifi.dbs.elki.datasource.bundle.MultipleObjectsBundle;
import de.lmu.ifi.dbs.elki.datasource.filter.ObjectFilter;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.FileParameter;

public class Hdf5DatabaseConnection extends AbstractDatabaseConnection {
	File inFile;

	protected Hdf5DatabaseConnection(List<ObjectFilter> arg0, File inFile) {
		super(arg0);
		this.inFile = inFile;
	}

	private static final Logging LOG = Logging
			.getLogger(Hdf5DatabaseConnection.class);

	@Override
	public MultipleObjectsBundle loadData() {
		String h5file = inFile.getPath();
		List<FloatVector> vecs = Hdf5.readH5dset(h5file, "/X");
		int dim = vecs.get(0).getDimensionality();
		VectorFieldTypeInformation<FloatVector> type = new VectorFieldTypeInformation<>(
				FloatVector.FACTORY, dim);

		return MultipleObjectsBundle.makeSimple(type, vecs);
	}

	@Override
	protected Logging getLogger() {
		return LOG;
	}

	public static class Parameterizer extends
			AbstractDatabaseConnection.Parameterizer {
		public static final OptionID INPUT_ID = new OptionID("h5.input",
				"The HDF5 file to input (contents in dataset '/X').");
		protected File infile;

		@Override
		protected void makeOptions(Parameterization config) {
			// Add the input file first, for usability reasons.
			final FileParameter inputParam = new FileParameter(INPUT_ID,
					FileParameter.FileType.INPUT_FILE);
			if (config.grab(inputParam)) {
				infile = inputParam.getValue();
			}
			super.makeOptions(config);
			configFilters(config);
		}

		@Override
		protected Hdf5DatabaseConnection makeInstance() {
			return new Hdf5DatabaseConnection(filters, infile);
		}
	}
}
