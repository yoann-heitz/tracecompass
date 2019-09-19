/**********************************************************************
 * Copyright (c) 2017, 2018 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 **********************************************************************/

package org.eclipse.tracecompass.internal.tmf.core.model.tree;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.common.core.log.TraceCompassLog;
import org.eclipse.tracecompass.common.core.log.TraceCompassLogUtils.FlowScopeLog;
import org.eclipse.tracecompass.common.core.log.TraceCompassLogUtils.FlowScopeLogBuilder;
import org.eclipse.tracecompass.internal.tmf.core.model.AbstractStateSystemAnalysisDataProvider;
import org.eclipse.tracecompass.internal.tmf.core.model.filters.FetchParametersUtils;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystem;
import org.eclipse.tracecompass.statesystem.core.exceptions.StateSystemDisposedException;
import org.eclipse.tracecompass.tmf.core.model.CommonStatusMessage;
import org.eclipse.tracecompass.tmf.core.model.filters.SelectionTimeQueryFilter;
import org.eclipse.tracecompass.tmf.core.model.filters.TimeQueryFilter;
import org.eclipse.tracecompass.tmf.core.model.timegraph.IElementResolver;
import org.eclipse.tracecompass.tmf.core.model.tree.ITmfTreeDataModel;
import org.eclipse.tracecompass.tmf.core.model.tree.ITmfTreeDataProvider;
import org.eclipse.tracecompass.tmf.core.model.tree.TmfTreeModel;
import org.eclipse.tracecompass.tmf.core.response.ITmfResponse;
import org.eclipse.tracecompass.tmf.core.response.TmfModelResponse;
import org.eclipse.tracecompass.tmf.core.statesystem.TmfStateSystemAnalysisModule;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;

/**
 * Class to abstract {@link ITmfTreeDataProvider} methods and fields. Handles
 * the quark to entry id associations, providing the concrete class with the
 * state system when required. Handles logging the time taken to return tree
 * models, encapsulating the model in a response, concurrency and the
 * exceptions.
 *
 * @param <A>
 *            Generic type for the encapsulated
 *            {@link TmfStateSystemAnalysisModule}
 * @param <M>
 *            Generic type for the returned {@link ITmfTreeDataModel}.
 * @author Loic Prieur-Drevon
 * @since 4.0
 */
public abstract class AbstractTreeDataProvider<A extends TmfStateSystemAnalysisModule, M extends ITmfTreeDataModel>
        extends AbstractStateSystemAnalysisDataProvider implements ITmfTreeDataProvider<M> {

    /**
     * Logger for Abstract Tree Data Providers.
     */
    protected static final Logger LOGGER = TraceCompassLog.getLogger(AbstractTreeDataProvider.class);

    private static final AtomicLong ENTRY_ID = new AtomicLong();
    private final A fAnalysisModule;
    private final ReentrantReadWriteLock fLock = new ReentrantReadWriteLock(false);
    private final BiMap<Long, Integer> fIdToQuark = HashBiMap.create();
    private @Nullable TmfModelResponse<TmfTreeModel<M>> fCached;
    private final Map<Long, Multimap<String, Object>> fEntryMetadata = new HashMap<>();

    /**
     * Constructor
     *
     * @param trace
     *            the trace this provider represents
     * @param analysisModule
     *            the analysis encapsulated by this provider
     */
    public AbstractTreeDataProvider(ITmfTrace trace, A analysisModule) {
        super(trace);
        fAnalysisModule = analysisModule;
    }

    /**
     * Getter for the encapsulated analysis module
     *
     * @return the underlying analysis module
     */
    protected A getAnalysisModule() {
        return fAnalysisModule;
    }

    /**
     * Get (and generate if necessary) a unique id for this quark. Should be called
     * inside {@link #getTree(ITmfStateSystem, Map, IProgressMonitor)},
     * where the write lock is held.
     *
     * @param quark
     *            quark to map to
     * @return the unique id for this quark
     */
    protected long getId(int quark) {
        return fIdToQuark.inverse().computeIfAbsent(quark, q -> ENTRY_ID.getAndIncrement());
    }

    /**
     * Get a new unique id, unbound to any quark.
     *
     * @return the unique id
     */
    protected long getEntryId() {
        return ENTRY_ID.getAndIncrement();
    }

    /**
     * Get selected entries from the filter for this provider
     *
     * @param filter
     *            {@link SelectionTimeQueryFilter}.
     * @return a map of the valid entries' ID from the filter to their respective
     *         quark
     */
    protected Map<Long, Integer> getSelectedEntries(SelectionTimeQueryFilter filter) {
        return getSelectedEntries(filter.getSelectedItems());
    }

    /**
     * Get selected entries from the collection of selected items for this provider
     *
     * @param selectedItems
     *            {@link SelectionTimeQueryFilter}.
     * @return a map of the valid entries' ID from the filter to their respective
     *         quark
     */
    protected Map<Long, Integer> getSelectedEntries(Collection<Long> selectedItems) {
        fLock.readLock().lock();
        try {
            Map<Long, Integer> selectedEntries = new HashMap<>();

            for (Long selectedItem : selectedItems) {
                Integer quark = fIdToQuark.get(selectedItem);
                if (quark != null && quark >= 0) {
                    selectedEntries.put(selectedItem, quark);
                }
            }
            return selectedEntries;
        } finally {
            fLock.readLock().unlock();
        }
    }

    /**
     * Get the times from the filter in the desired time range
     *
     * @param filter
     *            {@link TimeQueryFilter}
     * @param start
     *            lower bound
     * @param end
     *            upper bound
     * @return a {@link Set} of times in the time range
     */
    protected static Collection<Long> getTimes(TimeQueryFilter filter, long start, long end) {
        Collection<Long> times = new HashSet<>();
        for (long time : filter.getTimesRequested()) {
            if (start <= time && time <= end) {
                times.add(time);
            }
        }
        return times;
    }

    @Deprecated
    @Override
    public TmfModelResponse<List<M>> fetchTree(TimeQueryFilter filter, @Nullable IProgressMonitor monitor) {
        Map<String, Object> parameters = FetchParametersUtils.timeQueryToMap(filter);
        TmfModelResponse<TmfTreeModel<M>> response = fetchTree(parameters, monitor);
        TmfTreeModel<M> treeModel = response.getModel();
        List<M> listModel = null;
        if (treeModel != null) {
            listModel = treeModel.getEntries();
        }
        return new TmfModelResponse<>(listModel, response.getStatus(), response.getStatusMessage());
    }

    @Override
    public final TmfModelResponse<TmfTreeModel<M>> fetchTree(Map<String, Object> fetchParameters, @Nullable IProgressMonitor monitor) {
        fLock.readLock().lock();
        try {
            if (fCached != null) {
                /*
                 * If the tree depends on the filter, isCacheable should return false (by
                 * contract). If the tree is not cacheable, fCached will always be null, and we
                 * will never enter this block.
                 */
                return fCached;
            }
        } finally {
            fLock.readLock().unlock();
        }

        fLock.writeLock().lock();
        fAnalysisModule.waitForInitialization();
        ITmfStateSystem ss = fAnalysisModule.getStateSystem();
        if (ss == null) {
            return new TmfModelResponse<>(null, ITmfResponse.Status.FAILED, CommonStatusMessage.STATE_SYSTEM_FAILED);
        }

        boolean complete = ss.waitUntilBuilt(0);
        try (FlowScopeLog scope = new FlowScopeLogBuilder(LOGGER, Level.FINE, "AbstractTreeDataProvider#fetchTree") //$NON-NLS-1$
                .setCategory(getClass().getSimpleName()).build()) {
            TmfTreeModel<M> tree = null;
            /* Don't query empty state system */
            if (ss.getNbAttributes() > 0 && ss.getStartTime() != Long.MIN_VALUE) {
                tree = getTree(ss, fetchParameters, monitor);
                for (M model : tree.getEntries()) {
                    if (model instanceof IElementResolver) {
                        fEntryMetadata.put(model.getId(), ((IElementResolver) model).getMetadata());
                    }
                }
            }
            if (complete) {
                TmfModelResponse<TmfTreeModel<M>> response = new TmfModelResponse<>(tree,
                        ITmfResponse.Status.COMPLETED, CommonStatusMessage.COMPLETED);
                if (isCacheable()) {
                    fCached = response;
                }
                return response;
            }
            return new TmfModelResponse<>(tree, ITmfResponse.Status.RUNNING, CommonStatusMessage.RUNNING);
        } catch (StateSystemDisposedException e) {
            return new TmfModelResponse<>(null, ITmfResponse.Status.FAILED, CommonStatusMessage.STATE_SYSTEM_FAILED);
        } finally {
            fLock.writeLock().unlock();
        }
    }

    /**
     * Get the metadata of an entry by ID. This method returns an empty multimap
     * if there is no metadata
     *
     * @param entryId
     *            The ID of the entry
     * @return The metadata map for the entry
     */
    protected Multimap<String, Object> getEntryMetadata(Long entryId) {
        return fEntryMetadata.getOrDefault(entryId, ImmutableMultimap.of());
    }

    /**
     * Abstract method to determine if the trees from a provider are cacheable, to
     * let the {@link AbstractTreeDataProvider} handle caching. Should only return
     * true if the tree does not depend on the filter.
     *
     * @return if the implementation is cacheable
     */
    protected abstract boolean isCacheable();

    /**
     * Abstract method to be implemented by the providers to return trees. Lets the
     * abstract class handle waiting for {@link ITmfStateSystem} initialization and
     * progress, as well as error handling. The write lock to the quark <-> unique
     * id map for this provider is held at this point.
     *
     * @param ss
     *            the {@link TmfStateSystemAnalysisModule}'s {@link ITmfStateSystem}
     * @param fetchParameters
     *            the query's parameters
     * @param monitor
     *            progress monitor
     * @return the tree of entries
     * @throws StateSystemDisposedException
     *             if the state system was closed during the query or could not be
     *             queried.
     */
    protected abstract TmfTreeModel<M> getTree(ITmfStateSystem ss, Map<String, Object> fetchParameters,
            @Nullable IProgressMonitor monitor) throws StateSystemDisposedException;
}
