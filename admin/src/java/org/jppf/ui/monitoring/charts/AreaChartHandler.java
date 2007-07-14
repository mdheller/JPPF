/*
 * Java Parallel Processing Framework.
 * Copyright (C) 2005-2007 JPPF Team.
 * http://www.jppf.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 	 http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jppf.ui.monitoring.charts;

import java.util.Map;

import org.jfree.chart.*;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.labels.CategorySeriesLabelGenerator;
import org.jfree.chart.plot.*;
import org.jfree.chart.renderer.category.AreaRenderer;
import org.jfree.data.category.*;
import org.jppf.ui.monitoring.charts.config.ChartConfiguration;
import org.jppf.ui.monitoring.data.*;
import org.jppf.utils.StringUtils;

/**
 * Instances of this class are used to create and update 3D bar charts with a horizontal orientation.
 * @author Laurent Cohen
 */
public class AreaChartHandler implements ChartHandler
{
	/**
	 * The stats formatter that provides the data.
	 */
	private StatsHandler statsHandler = null;

	/**
	 * Initialize this chart handler with a specified stats formatter.
	 * @param statsHandler the stats formatter that provides the data.
	 */
	public AreaChartHandler(StatsHandler statsHandler)
	{
		this.statsHandler = statsHandler;
	}

	/**
	 * Create a plot XY chart based on a chart configuration.
	 * @param config holds the configuration parameters for the chart created, modified by this method.
	 * @return a <code>ChartConfiguration</code> instance.
	 * @see org.jppf.ui.monitoring.charts.ChartHandler#createChart(org.jppf.ui.monitoring.charts.config.ChartConfiguration)
	 */
	public ChartConfiguration createChart(ChartConfiguration config)
	{
		DefaultCategoryDataset ds = createDataset(config);
		JFreeChart chart = ChartFactory.createAreaChart(null, null, config.name, ds, PlotOrientation.VERTICAL, true, true, false);
		CategoryPlot plot = chart.getCategoryPlot();
    plot.setForegroundAlpha(0.5f);
		final CategoryAxis axis = plot.getDomainAxis();
		axis.setTickLabelsVisible(false);
		AreaRenderer rend = (AreaRenderer) plot.getRenderer();
		rend.setLegendItemLabelGenerator(new LegendLabelGenerator());

		config.chart = chart;
		return config;
	}

	/**
	 * Create and populate a dataset with the values of the specified fields.
	 * @param config the names of the fields whose values populate the dataset.
	 * @return a <code>DefaultCategoryDataset</code> instance.
	 */
	private DefaultCategoryDataset createDataset(ChartConfiguration config)
	{
		DefaultCategoryDataset ds = new DefaultCategoryDataset();
		config.dataset = ds;
		populateDataset(config);
		return ds;
	}
	
	/**
	 * Populate a dataset based on a chart configuration.
	 * @param config the chart configuration containing the dataset to populate.
	 * @return a <code>ChartConfiguration</code> instance.
	 * @see org.jppf.ui.monitoring.charts.ChartHandler#populateDataset(org.jppf.ui.monitoring.charts.config.ChartConfiguration)
	 */
	public ChartConfiguration populateDataset(ChartConfiguration config)
	{
		DefaultCategoryDataset dataset = (DefaultCategoryDataset) config.dataset;
		dataset.clear();
		int start = Math.max(0, statsHandler.getTickCount() - statsHandler.getStatsCount());
		for (int j=0; j<statsHandler.getStatsCount(); j++)
		{
			Map<Fields, Double> valueMap = statsHandler.getDoubleValues(j);
			for (Fields key: config.fields)
			{
				dataset.setValue(valueMap.get(key), key, new Integer(j + start));
			}
		}
		return config;
	}

	/**
	 * Update a dataset based on a chart configuration.
	 * @param config the chart configuration containing the dataset to update.
	 * @return a <code>ChartConfiguration</code> instance.
	 * @see org.jppf.ui.monitoring.charts.ChartHandler#updateDataset(org.jppf.ui.monitoring.charts.config.ChartConfiguration)
	 */
	public ChartConfiguration updateDataset(ChartConfiguration config)
	{
		DefaultCategoryDataset dataset = (DefaultCategoryDataset) config.dataset;
		Map<Fields, Double> valueMap = statsHandler.getLatestDoubleValues();
		for (Fields key: config.fields)
			dataset.setValue(valueMap.get(key), key, new Integer(statsHandler.getTickCount()));
		if (dataset.getRowCount() > statsHandler.getRolloverPosition()) dataset.removeRow(0);
		return config;
	}

	/**
	 * A label generator that builds value labels with a specified precision and unit. 
	 */
	public class LegendLabelGenerator implements CategorySeriesLabelGenerator
	{
		/**
		 * Generate a label for a value of a specified dataset at the specified row and column.
		 * @param dataset the dataset that contains the value to format.
		 * @param seriesIndex the data series to create a label for.
		 * @return a string containing the formatted value.
		 */
		public String generateLabel(CategoryDataset dataset, int seriesIndex)
		{
			String key = (String) dataset.getRowKey(seriesIndex);
			return StringUtils.shortenLabel(key);
		}
	}
}
