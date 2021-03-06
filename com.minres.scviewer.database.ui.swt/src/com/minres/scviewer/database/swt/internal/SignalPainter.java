/*******************************************************************************
 * Copyright (c) 2015 MINRES Technologies GmbH and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     MINRES Technologies GmbH - initial API and implementation
 *******************************************************************************/
package com.minres.scviewer.database.swt.internal;

import java.util.Map.Entry;
import java.util.NavigableMap;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Rectangle;

import com.minres.scviewer.database.ISignal;
import com.minres.scviewer.database.ISignalChange;
import com.minres.scviewer.database.ISignalChangeMulti;
import com.minres.scviewer.database.ISignalChangeSingle;
import com.minres.scviewer.database.ui.TrackEntry;
import com.minres.scviewer.database.ui.WaveformColors;

public class SignalPainter extends TrackPainter  {
	
	/**
	 * 
	 */
	private final WaveformCanvas waveCanvas;
	private ISignal<? extends ISignalChange> signal;

	public SignalPainter(WaveformCanvas txDisplay, boolean even, TrackEntry trackEntry) {
		super(trackEntry, even);
		this.waveCanvas = txDisplay;
		this.signal=trackEntry.getSignal();
	}

	public void paintArea(GC gc, Rectangle area) {	
		if(trackEntry.selected)
			gc.setBackground(this.waveCanvas.colors[WaveformColors.TRACK_BG_HIGHLITE.ordinal()]);
		else
			gc.setBackground(this.waveCanvas.colors[even?WaveformColors.TRACK_BG_EVEN.ordinal():WaveformColors.TRACK_BG_ODD.ordinal()]);
		gc.setFillRule(SWT.FILL_EVEN_ODD);
		gc.fillRectangle(area);
		Entry<Long, ? extends ISignalChange> firstChange=signal.getEvents().floorEntry(area.x*this.waveCanvas.getScaleFactor());
		Entry<Long, ? extends ISignalChange> lastTx=signal.getEvents().ceilingEntry((area.x+area.width)*this.waveCanvas.getScaleFactor());
		if(firstChange==null){
			if(lastTx==null) return;
			firstChange = signal.getEvents().firstEntry();
		} else if(lastTx==null){
			lastTx=signal.getEvents().lastEntry();
		}
		gc.setForeground(this.waveCanvas.colors[WaveformColors.LINE.ordinal()]);
		gc.setLineStyle(SWT.LINE_SOLID);
		gc.setLineWidth(1);
		Entry<Long, ? extends ISignalChange> left=firstChange;
		if(left.getValue() instanceof ISignalChangeSingle){
			NavigableMap<Long, ? extends ISignalChange> entries=signal.getEvents().subMap(firstChange.getKey(), false, lastTx.getKey(), true);
			for(Entry<Long, ? extends ISignalChange> right:entries.entrySet()){
			    int xEnd= (int)(right.getKey()/this.waveCanvas.getScaleFactor());
			    int xBegin= (int)(left.getKey()/this.waveCanvas.getScaleFactor());
			    if(xEnd>xBegin){
			        int yOffset = this.waveCanvas.getTrackHeight()/2;
			        Color color = this.waveCanvas.colors[WaveformColors.SIGNALX.ordinal()];
			        switch(((ISignalChangeSingle) left.getValue()).getValue()){
			        case '1':
			            color=this.waveCanvas.colors[WaveformColors.SIGNAL1.ordinal()];
			            yOffset = this.waveCanvas.getTrackHeight()/5;
			            break;
			        case '0':
			            color=this.waveCanvas.colors[WaveformColors.SIGNAL0.ordinal()];
			            yOffset = 4*this.waveCanvas.getTrackHeight()/5;
			            break;
			        case 'Z':
			            color=this.waveCanvas.colors[WaveformColors.SIGNALZ.ordinal()];
			            break;
			        default:	
			        }
			        yOffset+=area.y;
			        gc.setForeground(color);
			        gc.drawLine(xBegin, yOffset, xEnd, yOffset);
			        int yNext =  this.waveCanvas.getTrackHeight()/2;
			        switch(((ISignalChangeSingle) right.getValue()).getValue()){
			        case '1':
			            yNext = this.waveCanvas.getTrackHeight()/5+area.y;
			            break;
			        case '0':
			            yNext = 4*this.waveCanvas.getTrackHeight()/5+area.y;
			            break;
			        default:	
			        }
			        gc.drawLine(xEnd, yOffset, xEnd, yNext);
			    }
			    left=right;
			}
		} else if(left.getValue() instanceof ISignalChangeMulti){
			NavigableMap<Long,? extends ISignalChange> entries=signal.getEvents().subMap(firstChange.getKey(), false, lastTx.getKey(), true);
			for(Entry<Long, ? extends ISignalChange> right:entries.entrySet()){
				int yOffsetT = this.waveCanvas.getTrackHeight()/5+area.y;
				int yOffsetM = this.waveCanvas.getTrackHeight()/2+area.y;
				int yOffsetB = 4*this.waveCanvas.getTrackHeight()/5+area.y;
				Color colorBorder = this.waveCanvas.colors[WaveformColors.SIGNAL0.ordinal()];
				ISignalChangeMulti last = (ISignalChangeMulti) left.getValue();
				if(last.getValue().toString().contains("X")){
					colorBorder=this.waveCanvas.colors[WaveformColors.SIGNALX.ordinal()];
				}else if(last.getValue().toString().contains("Z")){
					colorBorder=this.waveCanvas.colors[WaveformColors.SIGNALZ.ordinal()];
				}
				int beginTime= (int)(left.getKey()/this.waveCanvas.getScaleFactor());
				int endTime= (int)(right.getKey()/this.waveCanvas.getScaleFactor());
				int[] points = {
						beginTime,yOffsetM, 
						beginTime+1,yOffsetT, 
						endTime-1,yOffsetT, 
						endTime,yOffsetM, 
						endTime-1,yOffsetB, 
						beginTime+1,yOffsetB
				};
				gc.setForeground(colorBorder);
				gc.drawPolygon(points);
				gc.setForeground(this.waveCanvas.colors[WaveformColors.SIGNAL_TEXT.ordinal()]);
				int size = gc.getDevice().getDPI().y * gc.getFont().getFontData()[0].getHeight()/72;
				if(beginTime<area.x) beginTime=area.x;
				int width=endTime-beginTime;
				if(width>6) {
					Rectangle old = gc.getClipping();
					gc.setClipping(beginTime+3, yOffsetT, endTime-beginTime-5, yOffsetB-yOffsetT);
					gc.drawText("h'"+last.getValue().toHexString(), beginTime+3, yOffsetM-size/2-1);
					gc.setClipping(old);
				}
				left=right;
			}
		}
	}


	public ISignal<? extends ISignalChange> getSignal() {
		return signal;
	}

}