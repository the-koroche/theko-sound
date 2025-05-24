package org.theko.sound.util;

/**
 * Interface for tracking the number of instances of a class.
 * <p>
 * Implementing classes should provide mechanisms to retrieve the total number of instances created
 * and the identifier of the current instance.
 * </p>
 * 
 * @since v1.6.0
 * 
 * @author Theko
 */
public interface InstanceCounter {
    /**
     * Returns the total number of instances created for the implementing class.
     * @return The total number of instances created.
     */
    int getInstanceCount();

    /**
     * Returns the identifier of the current instance.
     * @return The identifier of the current instance.
     */
    int getCurrentInstance();
}
