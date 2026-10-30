import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

void main() {
  WidgetsFlutterBinding.ensureInitialized();
  runApp(const FamilyGuardChildApp());
}

class FamilyGuardChildApp extends StatelessWidget {
  const FamilyGuardChildApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'FamilyGuard Companion',
      debugShowCheckedModeBanner: false,
      theme: ThemeData.dark().copyWith(
        scaffoldBackgroundColor: const Color(0xFF0B0F19),
        cardColor: const Color(0xFF1E293B),
        colorScheme: const ColorScheme.dark(
          primary: Color(0xFF38BDF8),
          secondary: Color(0xFF10B981),
          error: Color(0xFFEF4444),
          surface: Color(0xFF1E293B),
        ),
      ),
      home: const ChildDashboardScreen(),
    );
  }
}

class ChildDashboardScreen extends StatefulWidget {
  const ChildDashboardScreen({super.key});

  @override
  State<ChildDashboardScreen> createState() => _ChildDashboardScreenState();
}

class _ChildDashboardScreenState extends State<ChildDashboardScreen> with WidgetsBindingObserver {
  static const _channel = MethodChannel('com.familyguard.child/enforcement');

  Map<String, dynamic> _permissions = {
    'hasUsageStats': false,
    'hasOverlay': false,
    'isBatteryIgnored': false,
    'hasAccessibility': false,
  };

  Map<String, dynamic> _deviceInfo = {
    'model': 'Desconocido',
    'manufacturer': 'Android',
    'androidVersion': '14',
  };

  bool _instantLockSimulated = false;
  int _policyVersion = 1;
  String _testPackage = 'com.instagram.android';
  Map<String, dynamic>? _lastDecision;
  final TextEditingController _pairingController = TextEditingController();
  bool _isPaired = false;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addObserver(this);
    _loadDeviceInfo();
    _refreshDiagnostics();
  }

  @override
  void dispose() {
    WidgetsBinding.instance.removeObserver(this);
    _pairingController.dispose();
    super.dispose();
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    if (state == AppLifecycleState.resumed) {
      _refreshDiagnostics();
    }
  }

  Future<void> _loadDeviceInfo() async {
    try {
      final info = await _channel.invokeMapMethod<String, dynamic>('getDeviceInfo');
      if (info != null && mounted) {
        setState(() => _deviceInfo = info);
      }
    } catch (_) {}
  }

  Future<void> _refreshDiagnostics() async {
    try {
      final diag = await _channel.invokeMapMethod<String, dynamic>('diagnosePermissions');
      if (diag != null && mounted) {
        setState(() => _permissions = diag);
      }
    } catch (_) {}
  }

  Future<void> _requestPermission(String method) async {
    try {
      await _channel.invokeMethod(method);
    } catch (_) {}
  }

  Future<void> _toggleInstantLock(bool value) async {
    setState(() {
      _instantLockSimulated = value;
      _policyVersion++;
    });

    try {
      await _channel.invokeMethod('syncPolicy', {
        'version': _policyVersion,
        'instantLockActive': _instantLockSimulated,
        'dailyLimitMinutes': 120,
        'dailyUsageMinutes': 0,
        'bedtimeActive': false,
        'schoolActive': false,
        'blockedPackages': ['com.instagram.android', 'com.zhiliaoapp.musically'],
      });
      _evaluateTestPackage(_testPackage);
    } catch (_) {}
  }

  Future<void> _evaluateTestPackage(String pkg) async {
    setState(() => _testPackage = pkg);
    try {
      final decision = await _channel.invokeMapMethod<String, dynamic>('evaluateApp', {
        'packageName': pkg,
      });
      if (decision != null && mounted) {
        setState(() => _lastDecision = decision);
      }
    } catch (_) {}
  }

  @override
  Widget build(BuildContext context) {
    final allPermissionsOk = _permissions['hasUsageStats'] == true &&
        _permissions['hasOverlay'] == true &&
        _permissions['isBatteryIgnored'] == true &&
        _permissions['hasAccessibility'] == true;

    return Scaffold(
      appBar: AppBar(
        backgroundColor: const Color(0xFF0F172A),
        elevation: 0,
        title: Row(
          children: [
            const Text('🛡️ ', style: TextStyle(fontSize: 22)),
            Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const Text('FamilyGuard Companion', style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold)),
                Text(
                  '${_deviceInfo['manufacturer']} ${_deviceInfo['model']} (Android ${_deviceInfo['androidVersion']})',
                  style: const TextStyle(fontSize: 11, color: Color(0xFF94A3B8)),
                ),
              ],
            ),
          ],
        ),
        actions: [
          IconButton(
            icon: const Icon(Icons.refresh, color: Color(0xFF38BDF8)),
            onPressed: _refreshDiagnostics,
            tooltip: 'Refrescar Permisos',
          ),
        ],
      ),
      body: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          // Banner de Protección
          Container(
            padding: const EdgeInsets.all(16),
            decoration: BoxDecoration(
              gradient: LinearGradient(
                colors: allPermissionsOk
                    ? [const Color(0xFF064E3B), const Color(0xFF022C22)]
                    : [const Color(0xFF7F1D1D), const Color(0xFF450A0A)],
              ),
              borderRadius: BorderRadius.circular(12),
              border: Border.all(
                color: allPermissionsOk ? const Color(0xFF10B981) : const Color(0xFFEF4444),
                width: 1.5,
              ),
            ),
            child: Row(
              children: [
                Icon(
                  allPermissionsOk ? Icons.verified_user : Icons.warning_amber_rounded,
                  color: allPermissionsOk ? const Color(0xFF34D399) : const Color(0xFFF87171),
                  size: 36,
                ),
                const SizedBox(width: 14),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        allPermissionsOk ? 'Protección Activa y Segura' : 'Atención: Permisos Incompletos',
                        style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 15),
                      ),
                      const SizedBox(height: 4),
                      Text(
                        allPermissionsOk
                            ? 'El motor nativo PolicyEngine está activo en segundo plano.'
                            : 'Activa todos los permisos para garantizar la supervisión.',
                        style: const TextStyle(fontSize: 12, color: Color(0xFFE2E8F0)),
                      ),
                    ],
                  ),
                ),
              ],
            ),
          ),
          const SizedBox(height: 20),

          // Sección de Diagnóstico de Permisos
          const Text('DIAGNÓSTICO DE PERMISOS NATIVOS', style: TextStyle(fontSize: 12, fontWeight: FontWeight.bold, color: Color(0xFF94A3B8), letterSpacing: 1.1)),
          const SizedBox(height: 8),
          _buildPermissionTile(
            title: 'Datos de Uso de Apps',
            description: 'Permite identificar qué app está activa.',
            isGranted: _permissions['hasUsageStats'] == true,
            onAction: () => _requestPermission('requestUsageStats'),
          ),
          _buildPermissionTile(
            title: 'Mostrar Sobre Otras Apps (Overlay)',
            description: 'Permite mostrar la pantalla de pausa parental.',
            isGranted: _permissions['hasOverlay'] == true,
            onAction: () => _requestPermission('requestOverlay'),
          ),
          _buildPermissionTile(
            title: 'Exención de Ahorro de Batería',
            description: 'Evita que el sistema cierre la protección.',
            isGranted: _permissions['isBatteryIgnored'] == true,
            onAction: () => _requestPermission('requestBatteryOptimization'),
          ),
          _buildPermissionTile(
            title: 'Servicio de Accesibilidad',
            description: 'Detección instantánea sin consumo excesivo.',
            isGranted: _permissions['hasAccessibility'] == true,
            onAction: () => _requestPermission('requestAccessibility'),
          ),
          const SizedBox(height: 24),

          // Probador Interactivo del Motor Nativo
          const Text('VERIFICACIÓN DEL MOTOR NATIVO (KOTLIN)', style: TextStyle(fontSize: 12, fontWeight: FontWeight.bold, color: Color(0xFF94A3B8), letterSpacing: 1.1)),
          const SizedBox(height: 8),
          Card(
            shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
            child: Padding(
              padding: const EdgeInsets.all(16),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Row(
                    mainAxisAlignment: MainAxisAlignment.spaceBetween,
                    children: [
                      Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          const Text('Pausa Instantánea Remota', style: TextStyle(fontWeight: FontWeight.bold, fontSize: 14)),
                          Text('Versión de Política: v$_policyVersion', style: const TextStyle(fontSize: 12, color: Color(0xFF38BDF8))),
                        ],
                      ),
                      Switch(
                        value: _instantLockSimulated,
                        activeThumbColor: const Color(0xFFEF4444),
                        onChanged: _toggleInstantLock,
                      ),
                    ],
                  ),
                  const Divider(height: 24),
                  const Text('Probar Decisión del Motor para Aplicación:', style: TextStyle(fontSize: 12, color: Color(0xFF94A3B8))),
                  const SizedBox(height: 8),
                  Wrap(
                    spacing: 8,
                    runSpacing: 8,
                    children: [
                      _buildTestAppChip('Instagram', 'com.instagram.android'),
                      _buildTestAppChip('WhatsApp', 'com.whatsapp'),
                      _buildTestAppChip('Marcador SOS', 'com.google.android.dialer'),
                      _buildTestAppChip('YouTube', 'com.google.android.youtube'),
                    ],
                  ),
                  if (_lastDecision != null) ...[
                    const SizedBox(height: 14),
                    Container(
                      padding: const EdgeInsets.all(12),
                      decoration: BoxDecoration(
                        color: _lastDecision!['isBlocked'] == true ? const Color(0xFF450A0A) : const Color(0xFF022C22),
                        borderRadius: BorderRadius.circular(8),
                        border: Border.all(
                          color: _lastDecision!['isBlocked'] == true ? const Color(0xFFEF4444) : const Color(0xFF10B981),
                        ),
                      ),
                      child: Row(
                        children: [
                          Icon(
                            _lastDecision!['isBlocked'] == true ? Icons.block : Icons.check_circle,
                            color: _lastDecision!['isBlocked'] == true ? const Color(0xFFF87171) : const Color(0xFF34D399),
                          ),
                          const SizedBox(width: 10),
                          Expanded(
                            child: Column(
                              crossAxisAlignment: CrossAxisAlignment.start,
                              children: [
                                Text(
                                  _lastDecision!['isBlocked'] == true ? 'DECISIÓN: BLOQUEADO' : 'DECISIÓN: PERMITIDO',
                                  style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 13),
                                ),
                                Text(
                                  'Razón: ${_lastDecision!['reason']} (v${_lastDecision!['policyVersion']})',
                                  style: const TextStyle(fontSize: 11, color: Color(0xFFE2E8F0)),
                                ),
                              ],
                            ),
                          ),
                        ],
                      ),
                    ),
                  ],
                ],
              ),
            ),
          ),
          const SizedBox(height: 24),

          // Vinculación Familiar
          const Text('VINCULACIÓN CON FAMILIA', style: TextStyle(fontSize: 12, fontWeight: FontWeight.bold, color: Color(0xFF94A3B8), letterSpacing: 1.1)),
          const SizedBox(height: 8),
          Card(
            shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
            child: Padding(
              padding: const EdgeInsets.all(16),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  TextField(
                    controller: _pairingController,
                    keyboardType: TextInputType.number,
                    decoration: InputDecoration(
                      labelText: 'Código de Vinculación o PIN Familiar',
                      hintText: 'Ej: 933193',
                      prefixIcon: const Icon(Icons.qr_code, color: Color(0xFF38BDF8)),
                      border: OutlineInputBorder(borderRadius: BorderRadius.circular(8)),
                    ),
                  ),
                  const SizedBox(height: 12),
                  SizedBox(
                    width: double.infinity,
                    child: ElevatedButton.icon(
                      style: ElevatedButton.styleFrom(
                        backgroundColor: const Color(0xFF38BDF8),
                        foregroundColor: const Color(0xFF0F172A),
                        padding: const EdgeInsets.symmetric(vertical: 14),
                        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(8)),
                      ),
                      icon: const Icon(Icons.link),
                      label: Text(_isPaired ? 'Dispositivo Vinculado' : 'Vincular Dispositivo'),
                      onPressed: () {
                        if (_pairingController.text.trim().isNotEmpty) {
                          setState(() => _isPaired = true);
                          ScaffoldMessenger.of(context).showSnackBar(
                            SnackBar(content: Text('Vinculado con éxito con código: ${_pairingController.text.trim()}')),
                          );
                        }
                      },
                    ),
                  ),
                ],
              ),
            ),
          ),
          const SizedBox(height: 32),
        ],
      ),
    );
  }

  Widget _buildPermissionTile({
    required String title,
    required String description,
    required bool isGranted,
    required VoidCallback onAction,
  }) {
    return Card(
      margin: const EdgeInsets.symmetric(vertical: 4),
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(10)),
      child: ListTile(
        leading: Icon(
          isGranted ? Icons.check_circle : Icons.cancel,
          color: isGranted ? const Color(0xFF10B981) : const Color(0xFFEF4444),
          size: 28,
        ),
        title: Text(title, style: const TextStyle(fontSize: 13, fontWeight: FontWeight.bold)),
        subtitle: Text(description, style: const TextStyle(fontSize: 11, color: Color(0xFF94A3B8))),
        trailing: isGranted
            ? const Chip(
                label: Text('Activo', style: TextStyle(fontSize: 10, color: Color(0xFF10B981))),
                backgroundColor: Color(0xFF022C22),
              )
            : OutlinedButton(
                style: OutlinedButton.styleFrom(
                  foregroundColor: const Color(0xFF38BDF8),
                  side: const BorderSide(color: Color(0xFF38BDF8)),
                  padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
                ),
                onPressed: onAction,
                child: const Text('Activar', style: TextStyle(fontSize: 11)),
              ),
      ),
    );
  }

  Widget _buildTestAppChip(String label, String pkg) {
    final isSelected = _testPackage == pkg;
    return ChoiceChip(
      label: Text(label),
      selected: isSelected,
      selectedColor: const Color(0xFF0284C7),
      onSelected: (_) => _evaluateTestPackage(pkg),
    );
  }
}
