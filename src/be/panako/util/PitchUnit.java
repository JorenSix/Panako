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



package be.panako.util;

import java.util.logging.Logger;

/**
 * Defines the unit of the pitch value.
 */
public enum PitchUnit {	
	/**
	 * Oscillations per second.
	 */
	HERTZ("Hertz"),

	/**
	 * Number of cents compared to the "absolute zero" a configured, low
	 * frequency. By default C0 (16Hz) is used.
	 */
	ABSOLUTE_CENTS("Absolute cents"),

	/**
	 * Number of cents (between 0 and 1200) relative to the start of the octave.
	 * The first octave starts at "absolute zero" a configured, low frequency.
	 */
	RELATIVE_CENTS("Relative cents"),

	/**
	 * An integer from 0 to 127 that represents the closest MIDI key. All Hz
	 * values above 13289.7300 Hz are mapped to 127, all values below a certain
	 * value are mapped to 0.
	 */
	MIDI_KEY("MIDI key"),

	/**
	 * An double from 0 to 127 that represents the closest MIDI key. All values
	 * above (13289.7300 Hz + 99,99... cents) are mapped to 127,99999... All
	 * values below a certain value are mapped to 0.00.
	 */
	MIDI_CENT("MIDI cent");

	private final String humanName;
	
	/**
	 * Creates a new pitch unit with a human name.
	 * 
	 * @param name
	 *            The human name.
	 */
	private PitchUnit(final String name) {
		humanName = name;
	}

	/**
	 * 
	 * @return A nicer description of the name of the unit.
	 */
	public String getHumanName() {
		return humanName;
	}
	
	
	/**
	 * Converts a pitch in Hertz to the current unit.
	 * 
	 * 
	 * <code>
	 * //440Hz is MIDI key 69
	 * PitchUnit.MIDI_KEY.convert(440) == 69;
	 * </code> 
	 * 
	 * @param hertzValue
	 *            The pitch in Hertz.
	 * @return A converted pitch value;
	 */
	
	
	/**
	 * Convert the given pitch in the given unit to this unit. E.g. following
	 * statements should evaluate to true with the default reference frequency
	 * and the default offset octave (for RELATIVE_CENTS).
	 * 
	 * <pre>
	 * PitchUnit.HERZ.convert(6900,PitchUnit.ABSOLUTE_CENTS) == 440
	 * PitchUnit.HERZ.convert(69,PitchUnit.MIDI_KEY) == 440
	 * PitchUnit.HERZ.convert(69.00,PitchUnit.MIDI_CENTS) == 440
	 * PitchUnit.HERZ.convert(700,PitchUnit.RELATIVE_CENTS) == 440
	 * 
	 * PitchUnit.ABSOLUTE_CENTS.convert(440,PitchUnit.HERTZ) == 6900
	 * 
	 * 
	 * 
	 * </pre>
	 * 
	 * @param value
	 *            The value of the given pitch.
	 * @param valueUnit
	 *            The unit of the given pitch.
	 * @return The given pitch converted to this unit.
	 */
	public double convert(final double value, final PitchUnit valueUnit) {
		final double hertzValue = convertToHertz(value, valueUnit);// In Hz
		final double convertedPitch = convertHertz(hertzValue);// In valueUnit
		return convertedPitch;
	}
	
	private double convertToHertz(final double value, final PitchUnit valueUnit){
		final double hertzValue;
		switch (valueUnit) {
		case ABSOLUTE_CENTS:
			hertzValue = PitchUnit.absoluteCentToHertz(value);
			break;
		case HERTZ:
			hertzValue = value;
			break;
		case MIDI_CENT:
			hertzValue = PitchUnit.midiCentToHertz(value);
			break;
		case MIDI_KEY:
			hertzValue = PitchUnit.midiKeyToHertz((int) value);
			break;
		case RELATIVE_CENTS:
			hertzValue = PitchUnit.relativeCentToHertz(value);
			break;
		default:
			throw new AssertionError("Unknown pitch unit: " + getHumanName());
		}
		return hertzValue;
	}
	
	/**
	 * Converts a Hertz value to pitch in this unit.
	 * @param hertzValue The value in Hertz.
	 * @return The pitch in this unit.
	 */
	private double convertHertz(final double hertzValue) {
		final double convertedPitch;
		switch (this) {
		case ABSOLUTE_CENTS:
			convertedPitch = PitchUnit.hertzToAbsoluteCent(hertzValue);
			break;
		case HERTZ:
			convertedPitch = hertzValue;
			break;
		case MIDI_CENT:
			convertedPitch = PitchUnit.hertzToMidiCent(hertzValue);
			break;
		case MIDI_KEY:
			convertedPitch = PitchUnit.hertzToMidiKey(hertzValue);
			break;
		case RELATIVE_CENTS:
			convertedPitch = PitchUnit.hertzToRelativeCent(hertzValue);
			break;
		default:
			throw new AssertionError("Unknown pitch unit: " + getHumanName());
		}
		return convertedPitch;
	}
	
	
	
	
	
	/**
	 * Logging
	 */
	private static final Logger LOG = Logger.getLogger(PitchUnit.class.getName());
	
	

	
	/**
	 * The reference frequency used to calculate absolute cent values. By default it uses the same reference frequency as 
	 * MIDI: C-1 = 16.35/2 Hz = 8.175.
	 */
	private static final double REF_FREQ = 8.175;
	/**
	 * Cache LOG 2 calculation.
	 */
	private static final double LOG_TWO = Math.log(2.0);
	
	/**
	 * Defines where to start when converting a relative cent [0,1200[ value to
	 * Hertz. E.g. if the default octave is 5 then 900 relative cents are
	 * converted to 5 x 1200 + 900 = 6900 = 440Hz with the
	 * {@link PitchUnit.REF_FREQ} at 8.175Hz.
	 */
	private static final int DEFAULT_OCTAVE_OFFSET = 5 * 1200;// Absolute Cents

	/**
	 * Converts a MIDI CENT frequency to a frequency in Hz.
	 * 
	 * @param midiCent
	 *            The pitch in MIDI CENT.
	 * @return The pitch in Hertz.
	 */
	public static double midiCentToHertz(final double midiCent) {
		return 440 * Math.pow(2, (midiCent - 69) / 12d);
	}

	/**
	 * Converts a frequency in Hz to a MIDI CENT value using
	 * <code>(12 ^ log2 (f / 440)) + 69</code> <br>
	 * E.g.<br>
	 * <code>69.168 MIDI CENTS = MIDI NOTE 69  + 16,8 cents</code><br>
	 * <code>69.168 MIDI CENTS = 440Hz + x Hz</code>
	 * 
	 * @param hertzValue
	 *            The pitch in Hertz.
	 * @return The pitch in MIDI cent.
	 */
	public static double hertzToMidiCent(final double hertzValue) {
		double pitchInMidiCent = 0.0;
		if (hertzValue != 0) {
			pitchInMidiCent = 12 * Math.log(hertzValue / 440) / LOG_TWO + 69;
		}
		return pitchInMidiCent;
	}

	/**
	 * Returns the frequency (Hz) of an absolute cent value. This calculation
	 * uses a configured reference frequency.
	 * 
	 * @param absoluteCent
	 *            The pitch in absolute cent.
	 * @return A pitch in Hz.
	 */
	public static double absoluteCentToHertz(final double absoluteCent) {
		return PitchUnit.REF_FREQ * Math.pow(2, absoluteCent / 1200.0);
	}

	/**
	 * The reference frequency is configured. The default reference frequency is
	 * 16.35Hz. This is C0 on a piano keyboard with A4 tuned to 440 Hz. This
	 * means that 0 cents is C0; 1200 is C1; 2400 is C2; ... also -1200 cents is
	 * C-1
	 * 
	 * @param hertzValue
	 *            The pitch in Hertz.
	 * @return The value in absolute cents using the configured reference
	 *         frequency
	 */
	public static double hertzToAbsoluteCent(final double hertzValue) {
		double pitchInAbsCent = 0.0;
		if (hertzValue > 0) {
			pitchInAbsCent = 1200 * Math.log(hertzValue / PitchUnit.REF_FREQ) / PitchUnit.LOG_TWO;
		} else {
			//throw new IllegalArgumentException("Pitch in Hz schould be greater than zero, is " + hertzValue);
		}
		return pitchInAbsCent;
	}

	/**
	 * Converts a Hertz value to relative cents. E.g. 440Hz is converted to 900
	 * if the reference is a C.
	 * 
	 * @param hertzValue
	 *            A value in hertz.
	 * @return A value in relative cents.
	 */
	public static double hertzToRelativeCent(final double hertzValue) {
		double absoluteCentValue = PitchUnit.hertzToAbsoluteCent(hertzValue);
		// make absoluteCentValue positive. E.g. -2410 => 1210
		if (absoluteCentValue < 0) {
			absoluteCentValue = Math.abs(1200 + absoluteCentValue);
		}
		// so it can be folded to one octave. E.g. 1210 => 10
		return absoluteCentValue % 1200.0;
	}
	
	/**
	 * Converts a relative cent value to an absolute Hertz value by using
	 * PitchUnit.REF_FREQ.
	 * 
	 * @param relativeCent
	 *            a value in relative cents.
	 * @return A pitch in Hertz.
	 */
	public static double relativeCentToHertz(double relativeCent) {
		if (relativeCent < 0 || relativeCent >= 1200) {
			LOG.warning("Relative cent values are values from 0 to 1199, inclusive " + relativeCent + " is invalid.");
		}
		return absoluteCentToHertz(relativeCent + DEFAULT_OCTAVE_OFFSET);
	}
	

	/**
	 * Calculates the frequency (Hz) for a MIDI key.
	 * 
	 * @param midiKey
	 *            The MIDI key. A MIDI key is an integer between 0 and 127,
	 *            inclusive.
	 * @return A frequency in Hz corresponding to the MIDI key.
	 * @exception IllegalArgumentException
	 *                If midiKey is not in the valid range between 0 and 127,
	 *                inclusive.
	 */
	public static double midiKeyToHertz(final int midiKey) {
		if (midiKey < 0 || midiKey > 127) {
			LOG.warning("MIDI keys are defined between 0 and 127 or from "
					+ midiKeyToHertz(0) + "Hz to " + midiKeyToHertz(127)
					+ "Hz, MIDI KEY " + midiKey + " is out of this range.");
		}
		return PitchUnit.midiCentToHertz(midiKey);
	}

	/**
	 * A MIDI key is an integer between 0 and 127, inclusive. Within a certain
	 * range every pitch is mapped to a MIDI key. If a value outside the range
	 * is given an IllegalArugmentException is thrown.
	 * 
	 * @param hertzValue
	 *            The pitch in Hertz.
	 * @return An integer representing the closest midi key.
	 * @exception IllegalArgumentException
	 *                if the hertzValue does not fall within the range of valid
	 *                MIDI key frequencies.
	 */
	public static int hertzToMidiKey(final Double hertzValue) {
		final int midiKey = (int) Math.round(PitchUnit
				.hertzToMidiCent(hertzValue));
		if (midiKey < 0 || midiKey > 127) {
			LOG.warning("MIDI keys are defined between 0 and 127 or from "
					+ midiKeyToHertz(0) + "Hz to " + midiKeyToHertz(127)
					+ "Hz, " + hertzValue
					+ "does not map directly to a MIDI key.");
		}
		return midiKey;
	}
	
	
	/**
	 * Converts a ratio to cents.
	 * "Ratios Make Cents: Conversions from ratios to cents and back again" in
	 * the book "Tuning Timbre Spectrum Scale" William A. Sethares
	 * 
	 * @param ratio
	 *            A cent value
	 * @return A ratio containing the same information.
	 */
	public static double ratioToCent(final double ratio) {
		final double cent;
		cent = 1200 / Math.log10(2) * Math.log10(ratio);
		return cent;
	}

	/**
	 * Converts cent values to ratios. See
	 * "Ratios Make Cents: Conversions from ratios to cents and back again" in
	 * the book "Tuning Timbre Spectrum Scale" William A. Sethares.
	 * 
	 * @param cent
	 *            A cent value
	 * @return A ratio containing the same information.
	 */
	public static double centToRatio(final double cent) {
		final double ratio;
		ratio = Math.pow(10, Math.log10(2) * cent / 1200.0);
		return ratio;
	}
}
