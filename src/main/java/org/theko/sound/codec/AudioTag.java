/*
 * Copyright 2025 Alex Soloviov (aka Theko)
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

package org.theko.sound.codec;

import java.util.List;

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
 * <p>Predefined constants include:
 * <ul>
 *   <li>{@link #TITLE} - Represents the title of the audio.</li>
 *   <li>{@link #ALBUM} - Represents the album name.</li>
 *   <li>{@link #ARTIST} - Represents the artist name.</li>
 *   <li>{@link #YEAR} - Represents the release year.</li>
 *   <li>{@link #TRACK} - Represents the track number.</li>
 *   <li>{@link #COMMENT} - Represents comments or notes.</li>
 *   <li>{@link #GENRE} - Represents the genre of the audio.</li>
 *   <li>{@link #ENGINEER} - Represents the audio engineer.</li>
 *   <li>{@link #WEBSITE} - Represents the source website.</li>
 *   <li>{@link #SOURCE} - Represents the source of the audio.</li>
 *   <li>{@link #SOFTWARE} - Represents the software used.</li>
 *   <li>{@link #TECHNICIAN} - Represents the technician involved.</li>
 * </ul>
 * 
 * <p>Example usage:
 * <pre>
 *     AudioTag tag = new AudioTag(AudioTag.TITLE, "My Song");
 *     System.out.println(tag); // Output: Title: My Song
 * </pre>
 * 
 * @since v1.3.1
 * @author Theko
 */
public class AudioTag {

    protected String key;
    protected String value;

    public static final String TITLE = "Title";
    public static final String ALBUM = "Album";
    public static final String ARTIST = "Artist";
    public static final String YEAR = "Year";
    public static final String TRACK = "Track";
    public static final String COMMENT = "Comment";
    public static final String GENRE = "Genre";
    public static final String ENGINEER = "Engineer";
    public static final String WEBSITE = "SRC";
    public static final String SOURCE = "SRC";
    public static final String SOFTWARE = "Software";
    public static final String TECHNICIAN = "Technician";

    public AudioTag(String key, String value) {
        this.key = key;
        this.value = value;
    }

    public String getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public static String getValue(List<AudioTag> tags, String name) {
        for (AudioTag tag : tags) {
            if (tag.getKey().equalsIgnoreCase(name)) {
                return tag.getValue();
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return key + ": " + value;
    }
}
