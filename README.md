# Webex Flutter Plugin


🚀 **Webex Flutter Plugin** is a Flutter plugin designed to enable seamless video calling using the [Webex](https://developer.webex.com/docs) Android SDK. Currently, it supports **Android** and allows developers to initiate video calls by passing a phone number from the Flutter side.

---

## 📢 Demo
<div align:left;display:inline;>
<img height="360" src="https://github.com/user-attachments/assets/9deb62e8-2d84-45c3-b6b6-5797da71a73c"/>

<img height="360" src="https://github.com/user-attachments/assets/fed8dbb4-652c-4f54-98e2-e3d485ec2a9e"/>

<img height="360" src="https://github.com/user-attachments/assets/042ab33d-433e-497e-9c5a-2310061ba65c"/> 

</div> 

---

## Features

- 📞 **Initiate Video Calls**: Simply provide a number from Flutter, and the plugin takes care of starting a Webex video call.
- 💻 **Android Support**: Built for Android using [Webex SDK](https://developer.webex.com/docs/sdks/android).

---

## Installation

Add the plugin to your `pubspec.yaml` file:

```yaml
dependencies:
  webex_flutter_plugin: ^latest version
```

---

## Fast Use:

```
  initPlatformState() async {
    try {
      await _webexFlutterPlugin.startWebexCalling(
          callerId: _phoneController.text.trim(), jwtToken: 'JWT Token');
    } on PlatformException {
      debugPrint('Something went wrong');
    }

    if (!mounted) return;
  }
```
