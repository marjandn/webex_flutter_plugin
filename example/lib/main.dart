import 'package:flutter/material.dart';
import 'dart:async';

import 'package:flutter/services.dart';
import 'package:webex_flutter_plugin/webex_flutter_plugin.dart';

void main() {
  runApp(const MyApp());
}

class MyApp extends StatefulWidget {
  const MyApp({super.key});

  @override
  State<MyApp> createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  final _webexFlutterPlugin = WebexFlutterPlugin();

  final _phoneController = TextEditingController();

  @override
  void initState() {
    super.initState();
  }

  initPlatformState() async {
    try {
      await _webexFlutterPlugin.startWebexCalling(
          callerId: _phoneController.text.trim(), jwtToken: 'JWT Token');
    } on PlatformException {
      debugPrint('Something went wrong');
    }

    if (!mounted) return;
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
          appBar: AppBar(
            title: const Text('Webex Plugin example app'),
          ),
          body: Padding(
            padding: const EdgeInsets.all(30),
            child: Column(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                TextField(
                  controller: _phoneController,
                ),
                ElevatedButton(onPressed: initPlatformState, child: const Text("Call Now ...!"))
              ],
            ),
          )),
    );
  }
}
