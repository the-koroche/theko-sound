/*
 * Copyright 2025-present Alex Soloviov (aka Theko)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.theko.sound.codecs;

/**
 * The {@code AudioTag} class represents a key-value pair for audio metadata tags.
 * It provides predefined constants for common audio metadata fields such as title,
 * album, artist, and more. This class allows you to store and manipulate metadata
 * information for audio files.
 * 
 * <p>Each {@code AudioTag} instance consists of a key and a value, where the key
 * represents the metadata field (e.g., "Title") and the value represents the
 * corresponding data (e.g., "Song Name").
 * 
 * @since 1.3.1
 * @author Theko
 */
public class AudioTag {

    protected final String key;
    protected final String value;

    public static final String TITLE = "Title";
    public static final String ALBUM = "Album";
    public static final String ARTIST = "Artist";
    public static final String YEAR = "Year";
    public static final String TRACK = "Track";
    public static final String COMMENT = "Comment";
    public static final String GENRE = "Genre";
    public static final String ENGINEER = "Engineer";
    public static final String SOURCE = "SRC";
    public static final String SOFTWARE = "Software";
    public static final String TECHNICIAN = "Technician";

    public AudioTag(String key, String value) {
        this.key = key;
        this.value = value;
    }

    /**
     * Retrieves the key of the audio metadata tag.
     * 
     * @return Key of the audio metadata tag.
     */
    public String getKey() {
        return key;
    }

    /**
     * Retrieves the value associated with the audio metadata tag.
     * 
     * @return Value associated with the audio metadata tag.
     */
    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return key + ": " + value;
    }
}
