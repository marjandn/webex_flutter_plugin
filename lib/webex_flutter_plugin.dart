import 'webex_flutter_plugin_platform_interface.dart';

class WebexFlutterPlugin {
  Future<void> startWebexCalling({required String callerId}) async {
    WebexFlutterPluginPlatform.instance.startWebexCalling(callerId: callerId);
  }
}
