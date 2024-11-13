#ifndef FLUTTER_PLUGIN_WEBEX_FLUTTER_PLUGIN_H_
#define FLUTTER_PLUGIN_WEBEX_FLUTTER_PLUGIN_H_

#include <flutter/method_channel.h>
#include <flutter/plugin_registrar_windows.h>

#include <memory>

namespace webex_flutter_plugin {

class WebexFlutterPlugin : public flutter::Plugin {
 public:
  static void RegisterWithRegistrar(flutter::PluginRegistrarWindows *registrar);

  WebexFlutterPlugin();

  virtual ~WebexFlutterPlugin();

  // Disallow copy and assign.
  WebexFlutterPlugin(const WebexFlutterPlugin&) = delete;
  WebexFlutterPlugin& operator=(const WebexFlutterPlugin&) = delete;

  // Called when a method is called on this plugin's channel from Dart.
  void HandleMethodCall(
      const flutter::MethodCall<flutter::EncodableValue> &method_call,
      std::unique_ptr<flutter::MethodResult<flutter::EncodableValue>> result);
};

}  // namespace webex_flutter_plugin

#endif  // FLUTTER_PLUGIN_WEBEX_FLUTTER_PLUGIN_H_
