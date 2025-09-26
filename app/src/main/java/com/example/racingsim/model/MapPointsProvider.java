package com.example.racingsim.model;

import com.example.racingsim.track.TrackData;

/**
 * Provides 2D map points for the 3D preview based on the generated track data.
 */
public interface MapPointsProvider {

    MapPoints provideMapPoints(TrackData trackData);
}
