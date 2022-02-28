/*******************************************************************************
 * Copyright (c) 2020 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.examples.core.analysis;

import static org.eclipse.tracecompass.common.core.NonNullUtils.checkNotNull;

import java.util.ArrayList;
import java.util.Collection;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystemBuilder;
import org.eclipse.tracecompass.statesystem.core.exceptions.StateSystemDisposedException;
import org.eclipse.tracecompass.statesystem.core.interval.ITmfStateInterval;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.statesystem.AbstractTmfStateProvider;
import org.eclipse.tracecompass.tmf.core.statesystem.ITmfStateProvider;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;

/**
 * An example of a simple state provider for a simple state system analysis
 *
 * This module is also in the developer documentation of Trace Compass. If it is
 * modified here, the doc should also be updated.
 *
 * @author Alexandre Montplaisir
 * @author Geneviève Bastien
 */
public class ExampleStateProvider extends AbstractTmfStateProvider {

    private boolean intervalWritten = false;
    private long firstTimestamp = 1641396521146040576L;
    private long secondTimestamp = 1641396521146041344L;
    private double intervalValue = 10.0;
    private int quarkToQuery;


    private static final @NonNull String PROVIDER_ID = "org.eclipse.tracecompass.examples.state.provider"; //$NON-NLS-1$
    private static final int VERSION = 0;

    /**
     * Constructor
     *
     * @param trace
     *            The trace for this state provider
     */
    public ExampleStateProvider(@NonNull ITmfTrace trace) {
        super(trace, PROVIDER_ID);
    }

    @Override
    public int getVersion() {
        return VERSION;
    }

    @Override
    public @NonNull ITmfStateProvider getNewInstance() {
        return new ExampleStateProvider(getTrace());
    }

    @Override
    protected void eventHandle(ITmfEvent event) {
        if(!intervalWritten) {
            final ITmfStateSystemBuilder ss = checkNotNull(getStateSystemBuilder());
            int level0 = ss.getQuarkAbsoluteAndAdd("0");
            int level1 = ss.getQuarkRelativeAndAdd(level0, "1");
            int level2 = ss.getQuarkRelativeAndAdd(level1, "2");
            int level3 = ss.getQuarkRelativeAndAdd(level2, "3");
            quarkToQuery = ss.getQuarkRelativeAndAdd(level3, "quarkToQuery");
            ss.modifyAttribute(firstTimestamp, intervalValue, quarkToQuery);
            ss.modifyAttribute(secondTimestamp, 0.0, quarkToQuery);
            intervalWritten = true;
        }
    }

    @Override
    public void done() {
        final ITmfStateSystemBuilder ss = checkNotNull(getStateSystemBuilder());
        //Querying states one by one

        try {
            System.out.println("First single query");
            ITmfStateInterval interval1 = ss.querySingleState(firstTimestamp - 1, quarkToQuery);
            System.out.println("First interval : " + interval1);
            System.out.println("Second single query");
            ITmfStateInterval interval2 = ss.querySingleState(firstTimestamp + 1, quarkToQuery);
            System.out.println("Second interval : " + interval2);
            System.out.println("Third single query");
            ITmfStateInterval interval3 = ss.querySingleState(secondTimestamp + 1, quarkToQuery);
            System.out.println("Third interval : " + interval3);


            System.out.println("Now querying with query2D()");
            Collection<Integer> quarksToQuery = new ArrayList<>();
            quarksToQuery.add(quarkToQuery);
            Iterable<ITmfStateInterval> intervals = ss.query2D(quarksToQuery, firstTimestamp - 1, secondTimestamp + 1);
            System.out.println("Starting to print intervals");
            for(ITmfStateInterval interval : intervals) {
                System.out.println(interval);
            }
            System.out.println("Finished to print intervals");


        } catch (StateSystemDisposedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

}
