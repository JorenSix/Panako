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
