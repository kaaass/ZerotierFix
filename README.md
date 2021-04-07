<h1 align="center">
  <img src="https://github.com/kaaass/ZerotierFix/blob/master/app/src/main/ic_launcher-playstore.png?raw=true" alt="Zerotier Fix" width="200">
  <br>Zerotier Fix<br>
</h1>

<h4 align="center">An unofficial Zerotier Android client patched from official client.</h4>

## Features

- Self-hosted Moon Support
- Chinese translation

## Download

Check [Releases page](https://github.com/kaaass/ZerotierFix/releases) for newest apk built.

## Copyright

The code for this repository is based on the reverse engineering of the official Android client. The
original author is Grant Limberg (glimberg@gmail.com). See [AUTHORS.md](https://github.com/zerotier/ZeroTierOne/blob/master/AUTHORS.md#primary-authors) for more details.

- Zerotier JNI Sdk is located in `com.zerotier.sdk`
- Original Android client code is located in `net.kaaass.zerotierfix` (renamed from `com.zerotier.one`)
- Pre-built JNI library binary is located in `app/src/main/jniLibs`. This will be replaced by source code in the future
- App logo is a trademark of `ZeroTier, Inc.` and made by myself. 


## Roadmap

- [ ] Add moon config persistent & file config
- [ ] Add peer list view
- [ ] Support planet config
- [ ] Rewrite & update UI to fit Material Design
- [ ] Replace pre-built JNI library
- [ ] Clear up code (remove decompiler tag and refine)