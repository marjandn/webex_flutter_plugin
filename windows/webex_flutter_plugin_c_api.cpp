#include "include/webex_flutter_plugin/webex_flutter_plugin_c_api.h"

#include <flutter/plugin_registrar_windows.h>

#include "webex_flutter_plugin.h"

void WebexFlutterPluginCApiRegisterWithRegistrar(
    FlutterDesktopPluginRegistrarRef registrar) {
  webex_flutter_plugin::WebexFlutterPlugin::RegisterWithRegistrar(
      flutter::PluginRegistrarManager::GetInstance()
          ->GetRegistrar<flutter::PluginRegistrarWindows>(registrar));
}
