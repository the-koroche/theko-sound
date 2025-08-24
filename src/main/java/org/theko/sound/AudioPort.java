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

package org.theko.sound;

/**
 * The {@code AudioPort} class represents an audio port with specific attributes
 * such as its name, vendor, version, description, flow direction (input or output),
 * activity status, audio format, and a link object.
 * It provides methods to retrieve these attributes and a string representation
 * of the audio port.
 * 
 * <p>This class is immutable, meaning its state cannot be changed after it is created.</p>
 * 
 * @since 1.0.0
 * @author Theko
 */
public class AudioPort {
    
    private final String name;        // The name of the audio port
    private final String vendor;      // The vendor associated with the port
    private final String version;     // The version of the audio port
    private final String description; // A description of the port's functionality
    private final AudioFlow flow;     // The flow direction (input/output) of the audio port

    private final boolean isActive;   // Whether the port is active or not
    private final AudioFormat mixFormat;    // The audio format used for mixing

    private final Object link;        // The link object associated with the port
    
    /**
     * Constructor to create an AudioPort instance.
     * 
     * @param link The link object associated with the port.
     * @param flow The flow type of the port (input or output).
     * @param isActive Whether the port is active or not.
     * @param mixFormat The audio format used for mixing audio data.
     * @param name The name of the port.
     * @param vendor The vendor name of the port.
     * @param version The version of the port.
     * @param description A description providing details about the port.
     */
    public AudioPort(
            Object link,
            AudioFlow flow,
            boolean isActive,
            AudioFormat mixFormat,
            String name,
            String vendor,
            String version,
            String description) {
        this.name = name;
        this.vendor = vendor;
        this.version = version;
        this.description = description;
        this.flow = flow;
        this.isActive = isActive;
        this.mixFormat = mixFormat;
        this.link = link;
    }

    /**
     * Constructor to create an AudioPort instance with an active state set to true.
     * 
     * @param link The link object associated with the port.
     * @param flow The flow type of the port (input or output).
     * @param mixFormat The audio format used for mixing audio data.
     * @param name The name of the port.
     * @param vendor The vendor name of the port.
     * @param version The version of the port.
     */
    public AudioPort(
            Object link,
            AudioFlow flow,
            AudioFormat mixFormat,
            String name,
            String vendor,
            String version,
            String description) {
        this(link, flow, true, mixFormat, name, vendor, version, description);
    }

    /**
     * Constructor to create an AudioPort instance with an active state set to true and a description set to "Unknown".
     * 
     * @param link The link object associated with the port.
     * @param flow The flow type of the port (input or output).
     * @param mixFormat The audio format used for mixing audio data.
     * @param name The name of the port.
     * @param vendor The vendor name of the port.
     */
    public AudioPort(
            Object link,
            AudioFlow flow,
            boolean isActive,
            AudioFormat mixFormat,
            String name,
            String vendor) {
        this(link, flow, isActive, mixFormat, name, vendor, "Unknown", "Unknown");
    }

    /**
     * Returns the link object associated with the audio port.
     * 
     * @return The link object.
     */
    public Object getLink() {
        return link;
    }

    /**
     * Returns the flow direction of the audio port (input or output).
     * 
     * @return The flow type of the port (AudioFlow).
     */
    public AudioFlow getFlow() {
        return flow;
    }

    /**
     * Returns whether the audio port is active or not.
     * 
     * @return True if the port is active, false otherwise.
     */
    public boolean isActive() {
        return isActive;
    }

    /**
     * Returns the audio format associated with the audio port.
     * 
     * @return The audio format.
     */
    public AudioFormat getMixFormat() {
        return mixFormat;
    }

    /**
     * Returns the name of the audio port.
     * 
     * @return The name of the port.
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the vendor of the audio port.
     * 
     * @return The vendor of the port.
     */
    public String getVendor() {
        return vendor;
    }

    /**
     * Returns the version of the audio port.
     * 
     * @return The version of the port.
     */
    public String getVersion() {
        return version;
    }

    /**
     * Returns the description of the audio port.
     * 
     * @return The description of the port.
     */
    public String getDescription() {
        return description;
    }

    /**
     * Compares two AudioPort objects based on their link objects.
     * 
     * @param obj The object to compare with.
     * @return True if the objects are equal, false otherwise.
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        AudioPort other = (AudioPort) obj;
        return link.equals(other.link);
    }

    /**
     * Returns a string representation of the link object associated with the audio port.
     * 
     * @return A string that represents the link object.
     */
    public String getLinkAsString() {
        return link.getClass().getSimpleName() + "@" + link.hashCode();
    }

    /**
     * Provides a string representation of the AudioPort object.
     * 
     * @return A string that represents the audio port.
     */
    @Override
    public String toString() {
        return String.format(
            "AudioPort{Name: %s, Vendor: %s, Flow: %s, Active: %s}",
            name, vendor, flow, isActive
        );
    }
}
