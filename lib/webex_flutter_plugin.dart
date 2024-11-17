
import 'webex_flutter_plugin_platform_interface.dart';

class WebexFlutterPlugin {
  Future<String?> startWebexCalling() {
    return WebexFlutterPluginPlatform.instance.startWebexCalling();
  }
}
