/*******************************************************************************
 * Copyright (c) 2011, 2012 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Patrick Tasse - Initial API and implementation
 * Francois Chouinard - Put in shape for 1.0
 *******************************************************************************/

package org.eclipse.linuxtools.internal.tmf.core.trace;

import java.util.Arrays;

import org.eclipse.linuxtools.tmf.core.trace.ITmfLocation;

/**
 * A convenience class to store trace location arrays. The main purpose is to
 * provide a Comparable implementation for TmfExperimentLocation.
 *
 * @version 1.0
 * @author Patrick Tasse
 */
public class TmfLocationArray implements Comparable<TmfLocationArray>, Cloneable {

    // ------------------------------------------------------------------------
    // Attributes
    // ------------------------------------------------------------------------

    private final ITmfLocation[] fLocations;

    // ------------------------------------------------------------------------
    // Constructors
    // ------------------------------------------------------------------------

    /**
     * The standard constructor
     *
     * @param locations the locations
     */
    public TmfLocationArray(ITmfLocation[] locations) {
        fLocations = locations;
    }

    // ------------------------------------------------------------------------
    // Getters
    // ------------------------------------------------------------------------

    /**
     * Get the locations inside this array
     *
     * @return the locations
     */
    public ITmfLocation[] getLocations() {
        return fLocations;
    }

    // ------------------------------------------------------------------------
    // Cloneable
    // ------------------------------------------------------------------------

    /* (non-Javadoc)
     * @see java.lang.Object#clone()
     */
    @Override
    public TmfLocationArray clone() {
        ITmfLocation[] clones = new ITmfLocation[fLocations.length];
        for (int i = 0; i < fLocations.length; i++) {
            ITmfLocation location = fLocations[i];
            clones[i] = (location != null) ? location.clone() : null;
        }
        return new TmfLocationArray(clones);
    }

    // ------------------------------------------------------------------------
    // Comparable
    // ------------------------------------------------------------------------

    @Override
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public int compareTo(TmfLocationArray o) {
        for (int i = 0; i < fLocations.length; i++) {
            Comparable l1 = fLocations[i].getLocationInfo();
            Comparable l2 = o.fLocations[i].getLocationInfo();
            int result = l1.compareTo(l2);
            if (result != 0) {
                return result;
            }
        }
        return 0;
    }

    // ------------------------------------------------------------------------
    // Object
    // ------------------------------------------------------------------------

    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + Arrays.hashCode(fLocations);
        return result;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        TmfLocationArray other = (TmfLocationArray) obj;
        if (!Arrays.equals(fLocations, other.fLocations)) {
            return false;
        }
        return true;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    @SuppressWarnings("nls")
    public String toString() {
        return "TmfLocationArray [locations=" + Arrays.toString(fLocations) + "]";
    }

}
