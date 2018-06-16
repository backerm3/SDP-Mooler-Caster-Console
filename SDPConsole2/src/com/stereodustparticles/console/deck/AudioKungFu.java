/*
 * SDP Mooler Caster Console - version 2
 * Simple DJ software for "Mooler Casting" operations
 * 
 * Written by Ben Ackerman (IfYouLikeGoodIdeas) for Stereo Dust Particles, 2017
 * 
 * AudioKungFu: Various audio processing functions
 */
package com.stereodustparticles.console.deck;

import javax.sound.sampled.AudioFormat;

public class AudioKungFu {
	public static float getPeak(byte[] buffer, int blen, AudioFormat format) {
		float[] samples = new float[blen / (format.getSampleSizeInBits() / 8)];
		int samplesRead = SimpleAudioConversion.unpack(buffer, samples, blen, format);
		int channels = format.getChannels();
		float peak = 0f;
		
		for ( int i = 0; i < samplesRead; i += channels ) {
			float thisSample = 0f;
			for ( int j = 0; j < channels; j++ ) {
				if ( samples[i + j] > thisSample ) {
					thisSample = samples[i + j];
				}
			}
			
			if ( thisSample > peak ) {
				peak = thisSample;
			}
		}
		
		return peak;
	}
}
