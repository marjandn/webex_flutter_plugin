
import 'webex_flutter_plugin_platform_interface.dart';

class WebexFlutterPlugin {
  Future<String?> getPlatformVersion() {
    return WebexFlutterPluginPlatform.instance.getPlatformVersion();
  }
}
