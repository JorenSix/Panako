/***************************************************************************
*                                                                          *                     
* Panako - acoustic fingerprinting                                         *   
* Copyright (C) 2014 - Joren Six / IPEM                                    *   
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


package be.panako.strategy.nfft;

/**
 * An event point is a key point is a spectral representation of a signal. It is defined by a
 * time and frequency, using indexes, and it has an energy (magnitude).
 *
 */
public class NFFTEventPoint {
	
	/**
	 * The time expressed using an analysis frame index.
	 */
	public int t;
	
	/**
	 * The frequency expressed using the bin number in the FFT-transform.
	 */
	public int f;
	
	
	public float contrast;
	
	/**
	 * The frequency expressed in Hz.
	 */
	public float frequencyEstimate;
	
	
	/**
	 * Create a new event point with a time, frequency and energy and contrast..
	 * @param t The time expressed using an analysis frame index.
	 * @param f The frequency expressed using the bin number in the constant Q transform.
	 * @param frequencyEstimate A more detailed estimate of the frequency in Hz (using phase information).
	 * @param energy The energy value of the element.
	 * @param contrast How much contrast there is between this point and the surrounding environment
	 */
	public NFFTEventPoint(int t,int f,float frequencyEstimate, float energy, float contrast){
		this.t = t;
		this.f = f;
		this.contrast = contrast;
		this.frequencyEstimate = frequencyEstimate;
	}
}
