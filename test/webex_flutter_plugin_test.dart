import 'package:flutter_test/flutter_test.dart';
import 'package:webex_flutter_plugin/webex_flutter_plugin.dart';
import 'package:webex_flutter_plugin/webex_flutter_plugin_platform_interface.dart';
import 'package:webex_flutter_plugin/webex_flutter_plugin_method_channel.dart';
import 'package:plugin_platform_interface/plugin_platform_interface.dart';

class MockWebexFlutterPluginPlatform
    with MockPlatformInterfaceMixin
    implements WebexFlutterPluginPlatform {

  @override
  Future<String?> getPlatformVersion() => Future.value('42');
}

void main() {
  final WebexFlutterPluginPlatform initialPlatform = WebexFlutterPluginPlatform.instance;

  test('$MethodChannelWebexFlutterPlugin is the default instance', () {
    expect(initialPlatform, isInstanceOf<MethodChannelWebexFlutterPlugin>());
  });

  test('getPlatformVersion', () async {
    WebexFlutterPlugin webexFlutterPlugin = WebexFlutterPlugin();
    MockWebexFlutterPluginPlatform fakePlatform = MockWebexFlutterPluginPlatform();
    WebexFlutterPluginPlatform.instance = fakePlatform;

    expect(await webexFlutterPlugin.getPlatformVersion(), '42');
  });
}
