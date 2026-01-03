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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

/**
 * Represents a collection of audio metadata tags.
 * 
 * @since 2.5.0
 * @author Theko
 */
public class AudioTags implements Iterable<AudioTag> {

    private final ArrayList<AudioTag> tags;

    public AudioTags(AudioTags tags) {
        this.tags = new ArrayList<>(tags.tags);
    }

    public AudioTags(AudioTag[] tags) {
        this.tags = new ArrayList<>();
        for (AudioTag tag : tags) {
            this.tags.add(tag);
        }
    }

    public AudioTags(Collection<AudioTag> tags) {
        this.tags = new ArrayList<>(tags);
    }

    public AudioTags() {
        this.tags = new ArrayList<>();
    }

    /**
     * Adds the given audio metadata tag to the collection.
     * 
     * @param tag The audio metadata tag to add
     * @return This instance, for method chaining
     */
    public AudioTags add(AudioTag tag) {
        tags.add(tag);
        return this;
    }

    /**
     * Retrieves the audio metadata tag at the given index.
     * 
     * @param index Index of the audio metadata tag to retrieve
     * @return The audio metadata tag at the given index if found
     * @throws IndexOutOfBoundsException if the index is out of bounds
     */
    public AudioTag get(int index) {
        return tags.get(index);
    }

    /**
     * Removes the audio metadata tag at the given index from the collection.
     * 
     * @param index Index of the audio metadata tag to remove
     * @return This instance, for method chaining
     * @throws IndexOutOfBoundsException if the index is out of bounds
     */
    public AudioTags remove(int index) {
        tags.remove(index);
        return this;
    }

    /**
     * Removes the given audio metadata tag from the collection.
     * 
     * @param tag The audio metadata tag to remove
     * @return This instance, for method chaining
     */
    public AudioTags remove(AudioTag tag) {
        tags.remove(tag);
        return this;
    }

    /**
     * Returns the number of audio metadata tags in the collection.
     * 
     * @return Number of audio metadata tags
     */
    public int size() {
        return tags.size();
    }

    /**
     * Retrieves the value associated with the given key.
     * 
     * @param key Key of the audio metadata tag to retrieve
     * @return Value associated with the given key if found, or null otherwise
     */
    public String getValue(String key) {
        for (AudioTag tag : this.tags) {
            if (tag.getKey().equalsIgnoreCase(key)) {
                return tag.getValue();
            }
        }
        return null;
    }

    /**
     * Retrieves the audio metadata tag associated with the given key.
     * 
     * @param key Key of the audio metadata tag to retrieve
     * @return The audio metadata tag associated with the given key if found, or null otherwise
     */
    public AudioTag getTag(String key) {
        for (AudioTag tag : this.tags) {
            if (tag.getKey().equalsIgnoreCase(key)) {
                return tag;
            }
        }
        return null;
    }

    /**
     * Determines if the given key is present in the list of audio metadata tags.
     * 
     * @param key Key of the audio metadata tag to search for
     * @return true if the given key is present, false otherwise
     */
    public boolean containsKey(String key) {
        for (AudioTag tag : tags) {
            if (tag.getKey().equalsIgnoreCase(key)) return true;
        }
        return false;
    }

    @Override
    public Iterator<AudioTag> iterator() {
        return tags.iterator();
    }

    @Override
    public String toString() {
        return tags.toString();
    }
}
