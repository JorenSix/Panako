/***************************************************************************
*                                                                          *
* Panako - acoustic fingerprinting                                         *
* Copyright (C) 2014 - 2017 - Joren Six / IPEM                             *
*                                                                          *
* This program is free software: you can redistribute it and/or modify     *
* it under the terms of the GNU Affero General Public License as           *
* published by the Free Software Foundation, either version 3 of the       *
* License, or (at your option) any later version.                          *
*                                                                          *
* This program is distributed in the hope that it will be useful,          *
* but WITHOUT ANY WARRANTY; without even the implied warranty of           *
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the            *
* GNU Affero General Public License for more details.                      *
*                                                                          *
* You should have received a copy of the GNU Affero General Public License *
* along with this program.  If not, see <http://www.gnu.org/licenses/>     *
*                                                                          *
****************************************************************************
*    ______   ________   ___   __    ________   ___   ___   ______         *
*   /_____/\ /_______/\ /__/\ /__/\ /_______/\ /___/\/__/\ /_____/\        *
*   \:::_ \ \\::: _  \ \\::\_\\  \ \\::: _  \ \\::.\ \\ \ \\:::_ \ \       *
*    \:(_) \ \\::(_)  \ \\:. `-\  \ \\::(_)  \ \\:: \/_) \ \\:\ \ \ \      *
*     \: ___\/ \:: __  \ \\:. _    \ \\:: __  \ \\:. __  ( ( \:\ \ \ \     *
*      \ \ \    \:.\ \  \ \\. \`-\  \ \\:.\ \  \ \\: \ )  \ \ \:\_\ \ \    *
*       \_\/     \__\/\__\/ \__\/ \__\/ \__\/\__\/ \__\/\__\/  \_____\/    *
*                                                                          *
****************************************************************************
*                                                                          *
*                              Panako                                      *
*                       Acoustic Fingerprinting                            *
*                                                                          *
****************************************************************************/

package be.panako.util.cufft;

import be.tarsos.dsp.util.fft.FFT;
import be.tarsos.dsp.util.fft.WindowFunction;
import jcuda.jcufft.JCufft;
import jcuda.jcufft.cufftHandle;
import jcuda.jcufft.cufftType;

public class CudaFFT extends FFT {

	private final WindowFunction windowFunction;
	//private final int fftSize;
	private final float[] window; 
	private final cufftHandle plan;
	
	public CudaFFT(int size) {
		this(size,null);
	}
	
	public CudaFFT(int size,WindowFunction windowFunction) {
		super(size, windowFunction);
		this.windowFunction = windowFunction;
		//this.fftSize = size;
		if(windowFunction==null)
			window = null;
		else
		   window = windowFunction.generateCurve(size);
		
		plan = new cufftHandle();
		JCufft.cufftPlan1d(plan, size, cufftType.CUFFT_R2C, 1);
		
	}
	
	public void forwardTransform(final float[] data) {
		if(windowFunction!=null){
			for(int i = 0 ; i < data.length ; i++){
				data[i] = data[i] * window[i];
			}
		}
		JCufft.cufftExecR2C(plan, data, data);
	}

	public void destroy() {
		 JCufft.cufftDestroy(plan);
	}

}
