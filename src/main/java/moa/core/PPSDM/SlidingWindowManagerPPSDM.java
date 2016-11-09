/*
 *    SlidingWindowManagerPPSDM.java
 *    Copyright (C) 2012 Universitat Politècnica de Catalunya
 *    @author Massimo Quadrana <max.square@gmail.com>
 *
 *    This program is free software; you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation; either version 2 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program; if not, write to the Free Software
 *    Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

package moa.core.PPSDM;

import java.io.Serializable;
import java.util.List;
import java.util.Observable;
import com.yahoo.labs.samoa.instances.Instance;
import moa.core.SemiFCI;

/*
   I Made little change to notifyIncMine method (Tomas Chovanak).
*/
public abstract class SlidingWindowManagerPPSDM extends Observable implements Serializable{
    
    private static final long serialVersionUID = 1L;
    protected SegmentPPSDM currentSegment;
    protected double minSupport;
    protected int maxItemsetLength;
    protected int windowSize;
    
    /**
     * Default constructor.
     * @param minSupport
     * @param maxItemsetLength
     * @param windowSize
     */
    public SlidingWindowManagerPPSDM(double minSupport, int maxItemsetLength, 
            int windowSize){
        this.windowSize = windowSize;
        this.currentSegment = new SegmentPPSDM(minSupport, maxItemsetLength);
        this.minSupport = minSupport;
        this.maxItemsetLength = maxItemsetLength;
    }
    
    public abstract void addInstance(Instance inst);

    /**
     * Returns the last list of FCIs that have been computed.
     * @return list of FCIs
     * @throws java.lang.Exception
     */
    public List<SemiFCI> getFCI() throws Exception{
        return currentSegment.getFCI(windowSize);
    }

    /**
     * Notifies the IncMine instance associated to the segment manager
     * (Tomas Chovanak) I created wrapper object for parameters. This already has groupid
     *  and in this method also segment length is set and sent to incmine.
     * @param param
     */
    protected void notifyIncMine(ObserverParamWrapper param)
    {
        param.setSegmentLength(currentSegment.size());
        setChanged();
        notifyObservers(param);
        clearChanged();
    }
     
}
