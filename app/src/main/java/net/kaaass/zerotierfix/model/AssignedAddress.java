package net.kaaass.zerotierfix.model;

import org.greenrobot.greendao.annotation.Convert;
import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Id;
import org.greenrobot.greendao.converter.PropertyConverter;
import org.greenrobot.greendao.annotation.Generated;

@Entity
public class AssignedAddress {

    @Id
    private Long id;

    private long networkId;

    @Convert(converter = AddressTypeConverter.class, columnType = Integer.class)
    private AddressType type;

    private byte[] addressBytes;

    private String addressString;

    private short prefix;
    

    @Generated(hash = 294212135)
    public AssignedAddress(Long id, long networkId, AddressType type, byte[] addressBytes,
            String addressString, short prefix) {
        this.id = id;
        this.networkId = networkId;
        this.type = type;
        this.addressBytes = addressBytes;
        this.addressString = addressString;
        this.prefix = prefix;
    }

    @Generated(hash = 1516348015)
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

        AddressType(int i) {
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
