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

    @Override // androidx.fragment.app.Fragment
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
    }

    @Override // androidx.fragment.app.Fragment
    public View onCreateView(LayoutInflater layoutInflater, ViewGroup viewGroup, Bundle bundle) {
        View inflate = layoutInflater.inflate(R.layout.fragment_custom_d_n_s, viewGroup, false);
        EditText editText = inflate.findViewById(R.id.join_network_dns4_1);
        this.mDNSv4_1 = editText;
        editText.addTextChangedListener(new TextValidator(this.mDNSv4_1) {
            /* class com.zerotier.one.ui.CustomDNSFragment.AnonymousClass1 */

            @Override // com.zerotier.one.ui.CustomDNSFragment.TextValidator
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
        EditText editText2 = inflate.findViewById(R.id.join_network_dns4_2);
        this.mDNSv4_2 = editText2;
        editText2.addTextChangedListener(new TextValidator(this.mDNSv4_2) {
            /* class com.zerotier.one.ui.CustomDNSFragment.AnonymousClass2 */

            @Override // com.zerotier.one.ui.CustomDNSFragment.TextValidator
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
        EditText editText3 = inflate.findViewById(R.id.join_network_dns6_1);
        this.mDNSv6_1 = editText3;
        editText3.addTextChangedListener(new TextValidator(this.mDNSv6_1) {
            /* class com.zerotier.one.ui.CustomDNSFragment.AnonymousClass3 */

            @Override // com.zerotier.one.ui.CustomDNSFragment.TextValidator
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
        EditText editText4 = inflate.findViewById(R.id.join_network_dns6_2);
        this.mDNSv6_2 = editText4;
        editText4.addTextChangedListener(new TextValidator(this.mDNSv6_2) {
            /* class com.zerotier.one.ui.CustomDNSFragment.AnonymousClass4 */

            @Override // com.zerotier.one.ui.CustomDNSFragment.TextValidator
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

    abstract class TextValidator implements TextWatcher {
        private final EditText editText;

        public TextValidator(EditText editText2) {
            this.editText = editText2;
        }

        public final void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {
        }

        public final void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
        }

        public abstract void validate(EditText editText2, String str);

        public final void afterTextChanged(Editable editable) {
            validate(this.editText, this.editText.getText().toString());
        }
    }
}
