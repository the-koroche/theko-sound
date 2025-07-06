package org.theko.sound;

/**
 * The {@code AudioPort} class represents an audio port with specific attributes
 * such as its name, vendor, version, description, and flow direction (input or output).
 * It provides methods to retrieve these attributes and a string representation
 * of the audio port.
 * 
 * <p>This class is immutable, meaning its state cannot be changed after it is created.</p>
 * 
 * <p>Example usage:</p>
 * <pre>
 *     AudioPort port = new AudioPort(AudioFlow.INPUT, "Mic Input", "Default", "1.0", "Microphone input port");
 *     System.out.println(port.getName()); // Outputs: Mic Input
 * </pre>
 * 
 * @since v1.0.0
 * 
 * @author Theko
 */
public class AudioPort {
    
    private final String name;        // The name of the audio port
    private final String vendor;      // The vendor associated with the port
    private final String version;     // The version of the audio port
    private final String description; // A description of the port's functionality
    private final AudioFlow flow;     // The flow direction (input/output) of the audio port
    
    /**
     * Constructor to create an AudioPort instance.
     * 
     * @param flow The flow type of the port (input or output).
     * @param name The name of the port.
     * @param vendor The vendor name of the port.
     * @param version The version of the port.
     * @param description A description providing details about the port.
     */
    public AudioPort (AudioFlow flow, String name, String vendor, String version, String description) {
        this.name = name;
        this.vendor = vendor;
        this.version = version;
        this.description = description;
        this.flow = flow;
    }

    /**
     * Returns the name of the audio port.
     * 
     * @return The name of the port.
     */
    public String getName () {
        return name;
    }

    /**
     * Returns the vendor of the audio port.
     * 
     * @return The vendor of the port.
     */
    public String getVendor () {
        return vendor;
    }

    /**
     * Returns the version of the audio port.
     * 
     * @return The version of the port.
     */
    public String getVersion () {
        return version;
    }

    /**
     * Returns the description of the audio port.
     * 
     * @return The description of the port.
     */
    public String getDescription () {
        return description;
    }

    /**
     * Returns the flow direction of the audio port (input or output).
     * 
     * @return The flow type of the port (AudioFlow).
     */
    public AudioFlow getFlow () {
        return flow;
    }

    /**
     * Provides a string representation of the AudioPort object, including 
     * flow type, name, vendor, and version.
     * 
     * @return A string that represents the audio port.
     */
    @Override
    public String toString () {
        return "AudioPort {" + flow.toString() + ", Name: " + name + ", Vendor: " + vendor + ", Version: " + version + "}";
    }
}
