package net.kaaass.zerotierfix.ui;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import androidx.fragment.app.Fragment;

import net.kaaass.zerotierfix.R;

import org.apache.commons.validator.routines.InetAddressValidator;

/**
 * 自定义 DNS 片段
 */
public class CustomDNSFragment extends Fragment {
    private EditText mDNSv4_1;
    private EditText mDNSv4_2;
    private EditText mDNSv6_1;
    private EditText mDNSv6_2;
    private CustomDNSListener mListener;

    public static CustomDNSFragment newInstance() {
        return new CustomDNSFragment();
    }

    public void setDNSListener(CustomDNSListener customDNSListener) {
        this.mListener = customDNSListener;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View inflate = inflater.inflate(R.layout.fragment_custom_d_n_s, container, false);
        // 设置 IPv4 DNS
        EditText textDns4_1 = inflate.findViewById(R.id.join_network_dns4_1);
        this.mDNSv4_1 = textDns4_1;
        textDns4_1.addTextChangedListener(new TextValidator(this.mDNSv4_1) {

            @Override
            public void validate(EditText editText, String str) {
                if (InetAddressValidator.getInstance().isValid(str)) {
                    editText.setError(null);
                    CustomDNSFragment.this.mListener.setDNSv4_1(str);
                    return;
                }
                editText.setError(getString(R.string.invalid_ipv4_dns));
                CustomDNSFragment.this.mListener.setDNSv4_1("");
            }
        });
        EditText textDns4_2 = inflate.findViewById(R.id.join_network_dns4_2);
        this.mDNSv4_2 = textDns4_2;
        textDns4_2.addTextChangedListener(new TextValidator(this.mDNSv4_2) {

            @Override
            public void validate(EditText editText, String str) {
                if (InetAddressValidator.getInstance().isValid(str)) {
                    editText.setError(null);
                    CustomDNSFragment.this.mListener.setDNSv4_2(str);
                    return;
                }
                editText.setError(getString(R.string.invalid_ipv4_dns));
                CustomDNSFragment.this.mListener.setDNSv4_2("");
            }
        });
        // 设置 DNS IPv6
        EditText textDns6_1 = inflate.findViewById(R.id.join_network_dns6_1);
        this.mDNSv6_1 = textDns6_1;
        textDns6_1.addTextChangedListener(new TextValidator(this.mDNSv6_1) {

            @Override
            public void validate(EditText editText, String str) {
                if (InetAddressValidator.getInstance().isValid(str)) {
                    editText.setError(null);
                    CustomDNSFragment.this.mListener.setDNSv6_1(str);
                    return;
                }
                editText.setError(getString(R.string.invalid_ipv6_dns));
                CustomDNSFragment.this.mListener.setDNSv6_1("");
            }
        });
        EditText textDns6_2 = inflate.findViewById(R.id.join_network_dns6_2);
        this.mDNSv6_2 = textDns6_2;
        textDns6_2.addTextChangedListener(new TextValidator(this.mDNSv6_2) {

            @Override
            public void validate(EditText editText, String str) {
                if (InetAddressValidator.getInstance().isValid(str)) {
                    editText.setError(null);
                    CustomDNSFragment.this.mListener.setDNSv6_2(str);
                    return;
                }
                editText.setError(getString(R.string.invalid_ipv6_dns));
                CustomDNSFragment.this.mListener.setDNSv6_2("");
            }
        });
        return inflate;
    }

    abstract static class TextValidator implements TextWatcher {
        private final EditText editText;

        public TextValidator(EditText editText) {
            this.editText = editText;
        }

        public final void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        public final void onTextChanged(CharSequence s, int start, int before, int count) {
        }

        public abstract void validate(EditText editText, String str);

        public final void afterTextChanged(Editable s) {
            validate(this.editText, this.editText.getText().toString());
        }
    }
}
