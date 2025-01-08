import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';

import 'webex_flutter_plugin_platform_interface.dart';

/// An implementation of [WebexFlutterPluginPlatform] that uses method channels.
class MethodChannelWebexFlutterPlugin extends WebexFlutterPluginPlatform {
  /// The method channel used to interact with the native platform.
  @visibleForTesting
  final methodChannel = const MethodChannel('webex_flutter_plugin');

  @override
  Future<void> startWebexCalling({required String callerId, required String jwtToken}) async {
    await methodChannel
        .invokeMethod<String>('startWebexCalling', {'caller_id': callerId, 'jwt_token': jwtToken});
  }
}
