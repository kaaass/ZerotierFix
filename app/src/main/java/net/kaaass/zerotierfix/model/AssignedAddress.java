package net.kaaass.zerotierfix.model;

import org.greenrobot.greendao.converter.PropertyConverter;

public class AssignedAddress {
    private byte[] addressBytes;
    private String addressString;
    private Long id;
    private long networkId;
    private short prefix;
    private AddressType type;

    public AssignedAddress(Long l, long j, AddressType addressType, byte[] bArr, String str, short s) {
        this.id = l;
        this.networkId = j;
        this.type = addressType;
        this.addressBytes = bArr;
        this.addressString = str;
        this.prefix = s;
    }

    public AssignedAddress() {
    }

    public Long getId() {
        return this.id;
    }

    public void setId(Long l) {
        this.id = l;
    }

    public AddressType getType() {
        return this.type;
    }

    public void setType(AddressType addressType) {
        this.type = addressType;
    }

    public short getPrefix() {
        return this.prefix;
    }

    public void setPrefix(short s) {
        this.prefix = s;
    }

    public long getNetworkId() {
        return this.networkId;
    }

    public void setNetworkId(long j) {
        this.networkId = j;
    }

    public byte[] getAddressBytes() {
        return this.addressBytes;
    }

    public void setAddressBytes(byte[] bArr) {
        this.addressBytes = bArr;
    }

    public String getAddressString() {
        return this.addressString;
    }

    public void setAddressString(String str) {
        this.addressString = str;
    }

    public enum AddressType {
        UNKNOWN(0),
        IPV4(1),
        IPV6(2);
        
        final int id;

        private AddressType(int i) {
            this.id = i;
        }

        public String toString() {
            int i = this.id;
            if (i != 1) {
                return i != 2 ? "Unknown" : "IPv6";
            }
            return "IPv4";
        }
    }

    public static class AddressTypeConverter implements PropertyConverter<AddressType, Integer> {
        public AddressType convertToEntityProperty(Integer num) {
            if (num == null) {
                return null;
            }
            AddressType[] values = AddressType.values();
            for (AddressType addressType : values) {
                if (addressType.id == num) {
                    return addressType;
                }
            }
            return AddressType.UNKNOWN;
        }

        public Integer convertToDatabaseValue(AddressType addressType) {
            if (addressType == null) {
                return null;
            }
            return addressType.id;
        }
    }
}
