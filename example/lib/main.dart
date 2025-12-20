import 'dart:async';
import 'package:flutter/material.dart';
import 'package:geolocator/geolocator.dart';
import 'package:permission_handler/permission_handler.dart';
import 'package:raw_gnss/gnss_status_model.dart';
import 'package:raw_gnss/raw_gnss.dart';

void main() {
  runApp(const MyApp());
}

class MyApp extends StatelessWidget {
  const MyApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Flutter Demo',
      theme: ThemeData(
        primarySwatch: Colors.blue,
        visualDensity: VisualDensity.adaptivePlatformDensity,
      ),
      home: Scaffold(
        appBar: AppBar(
          title: const Text("Demo"),
        ),
        body: const HomeScreen(),
      ),
    );
  }
}

class HomeScreen extends StatefulWidget {
  const HomeScreen({super.key});

  @override
  _HomeScreenState createState() => _HomeScreenState();
}

class _HomeScreenState extends State<HomeScreen> {
  var _hasPermissions = false;
  late RawGnss _gnss;
  Timer? _noFixTimer;
  bool _timedOut = false;
  StreamSubscription<Position>? _posSub;
  @override
  void initState() {
    super.initState();

    _gnss = RawGnss();

    Permission.location.request().then((value) {
      final granted = value.isGranted;
      setState(() => _hasPermissions = granted);
      if (granted) {
        _startNoFixTimer();
        _kickGpsStart();
      }
    });
  }

  @override
  Widget build(BuildContext context) => _hasPermissions
      ? StreamBuilder<GnssStatusModel>(
          stream: _gnss.gnssStatusEvents,
          builder: (context, snapshot) {
            if (!snapshot.hasData) {
              return _timedOut ? _noFixHelp() : _loadingSpinner();
            }

            final model = snapshot.data!;
            final count = model.satelliteCount ?? 0;
            final lresultJSON = model.toJson();
            debugPrint('GNSS event received: satelliteCount=$count');
            debugPrint('GNSS event received: json=$lresultJSON');

            return Column(
              children: [
                Padding(
                  padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
                  child: Row(
                    children: [
                      Chip(label: Text('Satellites: $count')),
                    ],
                  ),
                ),
                const Divider(height: 1),
                Expanded(
                  child: ListView.builder(
                    itemCount: count,
                    itemBuilder: (context, position) {
                      return ListTile(
                        title: Column(
                          children: [
                            Text(
                              "position: $position",
                            ),
                            
                            Text(
                              "json: ${model.status![position].toJson()}",
                            ), 
                          ],
                        ),
                      );
                    },
                  ),
                ),
              ],
            );
          },
        )
      : _loadingSpinner();

  void _startNoFixTimer() {
    _noFixTimer?.cancel();
    _noFixTimer = Timer(const Duration(seconds: 20), () {
      if (mounted) setState(() => _timedOut = true);
    });
  }

  Future<void> _kickGpsStart() async {
    final enabled = await Geolocator.isLocationServiceEnabled();
    if (!enabled) return;

    // Make a one-shot high-accuracy request to wake GNSS hardware.
    try {
      await Geolocator.getCurrentPosition(
        desiredAccuracy: LocationAccuracy.best,
        timeLimit: const Duration(seconds: 15),
      );
    } catch (_) {
      // Ignore; this is only to trigger hardware start.
    }

    // Keep a short-lived stream to sustain GNSS until first fix.
    _posSub?.cancel();
    _posSub = Geolocator.getPositionStream(
      locationSettings: const LocationSettings(
        accuracy: LocationAccuracy.best,
        distanceFilter: 0,
        timeLimit: Duration(minutes: 2),
      ),
    ).listen((_) {}, onError: (_) {});
  }

  Widget _loadingSpinner() => const Center(child: CircularProgressIndicator());

  Widget _noFixHelp() => Center(
        child: Padding(
          padding: const EdgeInsets.all(16.0),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              const Text(
                'Waiting for GNSS fix...\n\n'
                'Tips:\n'
                '• Go outdoors with clear sky view\n'
                '• Ensure Location is ON and Precise\n'
                '• Keep the app in foreground',
                textAlign: TextAlign.center,
              ),
              const SizedBox(height: 16),
              ElevatedButton(
                onPressed: () {
                  openAppSettings();
                },
                child: const Text('Open App Settings'),
              ),
            ],
          ),
        ),
      );

  @override
  void dispose() {
    _noFixTimer?.cancel();
    _posSub?.cancel();
    super.dispose();
  }
}
