package org.theko.sound;

public class AudioPort {
    private final String name, vendor, version, description;
    private final AudioFlow flow;
    
    public AudioPort (AudioFlow flow, String name, String vendor, String version, String description) {
        this.name = name;
        this.vendor = vendor;
        this.version = version;
        this.description = description;
        this.flow = flow;
    }

    public String getName() {
        return name;
    }

    public String getVendor() {
        return vendor;
    }

    public String getVersion() {
        return version;
    }

    public String getDescription() {
        return description;
    }

    public AudioFlow getFlow() {
        return flow;
    }

    @Override
    public String toString() {
        return "AudioPort {" + flow.toString() + ", Name: " + name + ", Vendor: " + vendor + ", Version: " + version + "}";
    }
}
