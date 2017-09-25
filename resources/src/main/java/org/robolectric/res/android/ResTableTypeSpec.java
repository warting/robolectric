package org.robolectric.res.android;

import org.robolectric.res.android.ResourceTypes.ResChunk_header;

/**
 * A specification of the resources defined by a particular type.
 * <p>
 * There should be one of these chunks for each resource type.
 * <p>
 * This structure is followed by an array of integers providing the set of
 * configuration change flags (ResTable_config::CONFIG_*) that have multiple
 * resources for that configuration.  In addition, the high bit is set if that
 * resource has been made public.
 */
public class ResTableTypeSpec {
    public ResChunk_header header;

    // The type identifier this chunk is holding.  Type IDs start
    // at 1 (corresponding to the value of the type bits in a
    // resource identifier).  0 is invalid.
    public byte id;

    // Must be 0.
    public byte res0;
    // Must be 0.
    public short res1;

    // Number of uint32_t entry configuration masks that follow.
    public int entryCount;

    // Additional flag indicating an entry is public.
    final static int SPEC_PUBLIC = 0x40000000;

    /**
     * The payload contains an array of integers. Each integer represents a mask of configurations for
     * which a resource entry has values for. The high bit is set if the entry is public.
     */
    public int[] configMasks;
}