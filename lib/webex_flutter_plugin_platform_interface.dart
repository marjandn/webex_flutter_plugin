import 'package:plugin_platform_interface/plugin_platform_interface.dart';

import 'webex_flutter_plugin_method_channel.dart';

abstract class WebexFlutterPluginPlatform extends PlatformInterface {
  /// Constructs a WebexFlutterPluginPlatform.
  WebexFlutterPluginPlatform() : super(token: _token);

  static final Object _token = Object();

  static WebexFlutterPluginPlatform _instance = MethodChannelWebexFlutterPlugin();

  /// The default instance of [WebexFlutterPluginPlatform] to use.
  ///
  /// Defaults to [MethodChannelWebexFlutterPlugin].
  static WebexFlutterPluginPlatform get instance => _instance;

  /// Platform-specific implementations should set this with their own
  /// platform-specific class that extends [WebexFlutterPluginPlatform] when
  /// they register themselves.
  static set instance(WebexFlutterPluginPlatform instance) {
    PlatformInterface.verifyToken(instance, _token);
    _instance = instance;
  }

  Future<String?> startWebexCalling() {
    throw UnimplementedError('platformVersion() has not been implemented.');
  }
}
