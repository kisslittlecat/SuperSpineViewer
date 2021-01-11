package com.esotericsoftware.spine32;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.ObjectMap.Entry;
import com.badlogic.gdx.utils.Pool;
import com.esotericsoftware.spine32.attachments.Attachment;

/**
 * Stores attachments by slot index and attachment name.
 */
public class Skin {
    static private final Key lookup = new Key();

    final String name;
    final ObjectMap<Key, Attachment> attachments = new ObjectMap();
    final Pool<Key> keyPool = new Pool(64) {
        protected Object newObject() {
            return new Key();
        }
    };

    public Skin(String name) {
        if (name == null) throw new IllegalArgumentException("name cannot be null.");
        this.name = name;
    }

    public void addAttachment(int slotIndex, String name, Attachment attachment) {
        if (attachment == null) throw new IllegalArgumentException("attachment cannot be null.");
        if (slotIndex < 0) throw new IllegalArgumentException("slotIndex must be >= 0.");
        Key key = keyPool.obtain();
        key.set(slotIndex, name);
        attachments.put(key, attachment);
    }

    /**
     * @return May be null.
     */
    public Attachment getAttachment(int slotIndex, String name) {
        if (slotIndex < 0) throw new IllegalArgumentException("slotIndex must be >= 0.");
        lookup.set(slotIndex, name);
        return attachments.get(lookup);
    }

    public void findNamesForSlot(int slotIndex, Array<String> names) {
        if (names == null) throw new IllegalArgumentException("names cannot be null.");
        if (slotIndex < 0) throw new IllegalArgumentException("slotIndex must be >= 0.");
        for (Key key : attachments.keys())
            if (key.slotIndex == slotIndex) names.add(key.name);
    }

    public void findAttachmentsForSlot(int slotIndex, Array<Attachment> attachments) {
        if (attachments == null) throw new IllegalArgumentException("attachments cannot be null.");
        if (slotIndex < 0) throw new IllegalArgumentException("slotIndex must be >= 0.");
        for (Entry<Key, Attachment> entry : this.attachments.entries())
            if (entry.key.slotIndex == slotIndex) attachments.add(entry.value);
    }

    public void clear() {
        for (Key key : attachments.keys())
            keyPool.free(key);
        attachments.clear();
    }

    public String getName() {
        return name;
    }

    public String toString() {
        return name;
    }

    /**
     * Attach each attachment in this skin if the corresponding attachment in the old skin is currently attached.
     */
    void attachAll(Skeleton skeleton, Skin oldSkin) {
        for (Entry<Key, Attachment> entry : oldSkin.attachments.entries()) {
            int slotIndex = entry.key.slotIndex;
            Slot slot = skeleton.slots.get(slotIndex);
            if (slot.attachment == entry.value) {
                Attachment attachment = getAttachment(slotIndex, entry.key.name);
                if (attachment != null) slot.setAttachment(attachment);
            }
        }
    }

    static class Key {
        int slotIndex;
        String name;
        int hashCode;

        public void set(int slotIndex, String name) {
            if (name == null) throw new IllegalArgumentException("name cannot be null.");
            this.slotIndex = slotIndex;
            this.name = name;
            hashCode = 31 * (31 + name.hashCode()) + slotIndex;
        }

        public int hashCode() {
            return hashCode;
        }

        public boolean equals(Object object) {
            if (object == null) return false;
            Key other = (Key) object;
            if (slotIndex != other.slotIndex) return false;
            return name.equals(other.name);
        }

        public String toString() {
            return slotIndex + ":" + name;
        }
    }
}
