import 'dart:async';
import 'dart:convert';
import 'package:flutter/material.dart';
import 'package:http/http.dart' as http;

void main() {
  WidgetsFlutterBinding.ensureInitialized();
  runApp(const FamilyGuardParentApp());
}

class FamilyGuardParentApp extends StatelessWidget {
  const FamilyGuardParentApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'FamilyGuard 2.0 — Panel de Padres',
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
      home: const ParentDashboardScreen(),
    );
  }
}

class ChildProfile {
  String id;
  String name;
  int age;
  String avatar;
  String pairingCode;
  bool instantLockActive;
  int dailyLimitMinutes;
  int usedTodayMinutes;
  bool bedtimeActive;
  bool schoolActive;
  List<AppRule> appRules;
  List<DeviceItem> devices;

  ChildProfile({
    required this.id,
    required this.name,
    required this.age,
    required this.avatar,
    required this.pairingCode,
    this.instantLockActive = false,
    this.dailyLimitMinutes = 120,
    this.usedTodayMinutes = 0,
    this.bedtimeActive = false,
    this.schoolActive = false,
    required this.appRules,
    required this.devices,
  });
}

class AppRule {
  String name;
  String packageName;
  String category;
  bool isBlocked;

  AppRule({
    required this.name,
    required this.packageName,
    required this.category,
    this.isBlocked = false,
  });
}

class DeviceItem {
  String name;
  String osVersion;
  int battery;
  bool isOnline;

  DeviceItem({
    required this.name,
    required this.osVersion,
    required this.battery,
    required this.isOnline,
  });
}

class ParentDashboardScreen extends StatefulWidget {
  const ParentDashboardScreen({super.key});

  @override
  State<ParentDashboardScreen> createState() => _ParentDashboardScreenState();
}

class _ParentDashboardScreenState extends State<ParentDashboardScreen> {
  int _selectedTabIndex = 0;
  int _policyVersion = 1;
  bool _isActionInProgress = false;
  Timer? _refreshTimer;

  final List<ChildProfile> _children = [
    ChildProfile(
      id: 'child_vicky_diego',
      name: 'Vicky',
      age: 10,
      avatar: '👧',
      pairingCode: '933193',
      instantLockActive: false,
      dailyLimitMinutes: 120,
      bedtimeActive: false,
      schoolActive: false,
      appRules: [
        AppRule(name: 'Instagram', packageName: 'com.instagram.android', category: 'Redes Sociales', isBlocked: true),
        AppRule(name: 'TikTok', packageName: 'com.zhiliaoapp.musically', category: 'Redes Sociales', isBlocked: true),
        AppRule(name: 'YouTube', packageName: 'com.google.android.youtube', category: 'Videos', isBlocked: false),
        AppRule(name: 'Roblox', packageName: 'com.roblox.client', category: 'Juegos', isBlocked: false),
        AppRule(name: 'WhatsApp', packageName: 'com.whatsapp', category: 'Comunicación', isBlocked: false),
      ],
      devices: [
        DeviceItem(name: 'OPPO CPH2743', osVersion: 'Android 16', battery: 84, isOnline: true),
      ],
    ),
  ];

  int _selectedChildIndex = 0;

  ChildProfile get _currentChild {
    if (_children.isEmpty) {
      return ChildProfile(
        id: 'child_default',
        name: 'Vicky',
        age: 10,
        avatar: '👧',
        pairingCode: '933193',
        appRules: [],
        devices: [],
      );
    }
    if (_selectedChildIndex >= _children.length) {
      _selectedChildIndex = 0;
    }
    return _children[_selectedChildIndex];
  }

  @override
  void initState() {
    super.initState();
    _fetchFamilyData();
    _refreshTimer = Timer.periodic(const Duration(seconds: 4), (_) {
      if (mounted) _fetchFamilyData(silent: true);
    });
  }

  @override
  void dispose() {
    _refreshTimer?.cancel();
    super.dispose();
  }

  Future<void> _fetchFamilyData({bool silent = false}) async {
    try {
      final res = await http.get(Uri.parse('/api/data'));
      if (res.statusCode == 200) {
        final data = jsonDecode(res.body) as Map<String, dynamic>;
        final rawChildren = data['children'] as List<dynamic>?;
        final policyVer = data['policyVersion'] as int? ?? _policyVersion;
        final clientPairingCode = (data['clientInfo'] as Map<String, dynamic>?)?['pairingCode']?.toString() ?? '933193';

        if (rawChildren != null && rawChildren.isNotEmpty) {
          final List<ChildProfile> updatedChildren = [];

          for (final c in rawChildren) {
            final childMap = c as Map<String, dynamic>;
            final childId = childMap['id']?.toString() ?? 'child_1';
            final childName = childMap['name']?.toString() ?? 'Hijo';
            final childAge = (childMap['age'] as num?)?.toInt() ?? 10;
            final childAvatar = childMap['avatar']?.toString() ?? '👧';
            final isLocked = childMap['instantLockActive'] == true;
            final dailyLimit = (childMap['dailyLimitMinutes'] as num?)?.toInt() ?? 120;
            final usedToday = (childMap['usedTodayMinutes'] as num?)?.toInt() ?? 0;
            final bedtime = childMap['bedtimeActive'] == true;
            final school = childMap['schoolTimeActive'] == true;

            // Dispositivos
            final rawDevices = childMap['devices'] as List<dynamic>? ?? [];
            final List<DeviceItem> parsedDevices = [];
            for (final d in rawDevices) {
              final dMap = d as Map<String, dynamic>;
              parsedDevices.add(DeviceItem(
                name: dMap['name']?.toString() ?? 'Smartphone Android',
                osVersion: dMap['os']?.toString() ?? 'Android',
                battery: (dMap['batteryLevel'] as num?)?.toInt() ?? 85,
                isOnline: dMap['isOnline'] == true,
              ));
            }

            // Aplicaciones
            final rawApps = (childMap['monitoredApps'] as List<dynamic>?) ?? (data['monitoredApps'] as List<dynamic>?) ?? [];
            final List<AppRule> parsedApps = [];
            for (final a in rawApps) {
              final aMap = a as Map<String, dynamic>;
              parsedApps.add(AppRule(
                name: aMap['name']?.toString() ?? 'App',
                packageName: aMap['packageName']?.toString() ?? '',
                category: aMap['category']?.toString() ?? 'General',
                isBlocked: aMap['action'] == 'blocked',
              ));
            }

            // Si el servidor aún no tiene apps cargadas, mantener las apps comunes
            if (parsedApps.isEmpty) {
              parsedApps.addAll([
                AppRule(name: 'Instagram', packageName: 'com.instagram.android', category: 'Redes Sociales', isBlocked: true),
                AppRule(name: 'TikTok', packageName: 'com.zhiliaoapp.musically', category: 'Redes Sociales', isBlocked: true),
                AppRule(name: 'YouTube', packageName: 'com.google.android.youtube', category: 'Videos', isBlocked: false),
                AppRule(name: 'Roblox', packageName: 'com.roblox.client', category: 'Juegos', isBlocked: false),
                AppRule(name: 'WhatsApp', packageName: 'com.whatsapp', category: 'Comunicación', isBlocked: false),
              ]);
            }

            updatedChildren.add(ChildProfile(
              id: childId,
              name: childName,
              age: childAge,
              avatar: childAvatar,
              pairingCode: clientPairingCode,
              instantLockActive: isLocked,
              dailyLimitMinutes: dailyLimit,
              usedTodayMinutes: usedToday,
              bedtimeActive: bedtime,
              schoolActive: school,
              appRules: parsedApps,
              devices: parsedDevices,
            ));
          }

          if (mounted) {
            setState(() {
              _children.clear();
              _children.addAll(updatedChildren);
              _policyVersion = policyVer;
            });
          }
        }
      }
    } catch (_) {
      // Si falla en modo silencioso de polling, no interrumpir la interfaz
    }
  }

  Future<void> _toggleInstantLock() async {
    if (_isActionInProgress) return;
    _isActionInProgress = true;

    final targetLock = !_currentChild.instantLockActive;
    final childId = _currentChild.id;
    final childName = _currentChild.name;

    setState(() {
      _currentChild.instantLockActive = targetLock;
    });

    try {
      final res = await http.post(
        Uri.parse('/api/instant-lock'),
        headers: {'Content-Type': 'application/json'},
        body: jsonEncode({
          'childId': childId,
          'locked': targetLock,
        }),
      );

      if (res.statusCode == 200) {
        final resData = jsonDecode(res.body) as Map<String, dynamic>;
        final finalLockState = resData['instantLockActive'] == true;
        final newVersion = resData['policyVersion'] as int? ?? (_policyVersion + 1);

        if (mounted) {
          setState(() {
            _currentChild.instantLockActive = finalLockState;
            _policyVersion = newVersion;
          });

          ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(
              backgroundColor: finalLockState ? const Color(0xFFEF4444) : const Color(0xFF10B981),
              duration: const Duration(seconds: 4),
              content: Text(
                finalLockState
                    ? '🚨 Bloqueo Remoto ENVIADO al celular de $childName (v$_policyVersion)'
                    : '✅ Acceso Reanudado para $childName (v$_policyVersion)',
                style: const TextStyle(fontWeight: FontWeight.bold),
              ),
            ),
          );
        }
      } else {
        // Revertir en caso de respuesta fallida
        if (mounted) {
          setState(() {
            _currentChild.instantLockActive = !targetLock;
          });
          ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(
              backgroundColor: const Color(0xFFEF4444),
              content: Text('Error del servidor: HTTP ${res.statusCode}'),
            ),
          );
        }
      }
    } catch (e) {
      if (mounted) {
        setState(() {
          _currentChild.instantLockActive = !targetLock;
        });
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            backgroundColor: const Color(0xFFEF4444),
            content: Text('Error de conexión con el servidor: $e'),
          ),
        );
      }
    } finally {
      _isActionInProgress = false;
    }
  }

  Future<void> _toggleAppRule(AppRule app, bool isBlocked) async {
    setState(() {
      app.isBlocked = isBlocked;
    });

    try {
      final res = await http.post(
        Uri.parse('/api/apps/action'),
        headers: {'Content-Type': 'application/json'},
        body: jsonEncode({
          'childId': _currentChild.id,
          'packageName': app.packageName,
          'action': isBlocked ? 'blocked' : 'allowed',
        }),
      );

      if (res.statusCode == 200) {
        final resData = jsonDecode(res.body) as Map<String, dynamic>;
        if (resData['policyVersion'] != null && mounted) {
          setState(() => _policyVersion = resData['policyVersion'] as int);
        }
      }
    } catch (_) {}
  }

  void _showAddChildDialog() {
    final nameCtrl = TextEditingController();
    final ageCtrl = TextEditingController(text: '10');
    String selectedAvatar = '👧';

    showDialog(
      context: context,
      builder: (ctx) => StatefulBuilder(
        builder: (context, setDlgState) => AlertDialog(
          backgroundColor: const Color(0xFF1E293B),
          title: const Text('Registrar Nuevo Hijo/a', style: TextStyle(fontWeight: FontWeight.bold)),
          content: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              TextField(
                controller: nameCtrl,
                decoration: const InputDecoration(labelText: 'Nombre del Hijo/a'),
              ),
              const SizedBox(height: 12),
              TextField(
                controller: ageCtrl,
                keyboardType: TextInputType.number,
                decoration: const InputDecoration(labelText: 'Edad'),
              ),
              const SizedBox(height: 16),
              const Text('Seleccionar Avatar:', style: TextStyle(fontSize: 12, color: Color(0xFF94A3B8))),
              const SizedBox(height: 8),
              Row(
                mainAxisAlignment: MainAxisAlignment.spaceEvenly,
                children: ['👧', '👦', '🧒', '👶'].map((emoji) {
                  final isSel = selectedAvatar == emoji;
                  return InkWell(
                    onTap: () => setDlgState(() => selectedAvatar = emoji),
                    child: Container(
                      padding: const EdgeInsets.all(8),
                      decoration: BoxDecoration(
                        color: isSel ? const Color(0xFF0284C7) : Colors.transparent,
                        borderRadius: BorderRadius.circular(8),
                      ),
                      child: Text(emoji, style: const TextStyle(fontSize: 28)),
                    ),
                  );
                }).toList(),
              ),
            ],
          ),
          actions: [
            TextButton(
              onPressed: () => Navigator.pop(ctx),
              child: const Text('Cancelar', style: TextStyle(color: Color(0xFF94A3B8))),
            ),
            ElevatedButton(
              style: ElevatedButton.styleFrom(backgroundColor: const Color(0xFF38BDF8), foregroundColor: Colors.black),
              onPressed: () async {
                if (nameCtrl.text.trim().isNotEmpty) {
                  final newName = nameCtrl.text.trim();
                  final newAge = int.tryParse(ageCtrl.text) ?? 10;
                  try {
                    await http.post(
                      Uri.parse('/api/children/create'),
                      headers: {'Content-Type': 'application/json'},
                      body: jsonEncode({
                        'name': newName,
                        'age': newAge,
                        'avatar': selectedAvatar,
                      }),
                    );
                    await _fetchFamilyData();
                  } catch (_) {}
                  if (ctx.mounted) Navigator.pop(ctx);
                }
              },
              child: const Text('Guardar'),
            ),
          ],
        ),
      ),
    );
  }

  void _showPairingCodeDialog() {
    showDialog(
      context: context,
      builder: (ctx) => AlertDialog(
        backgroundColor: const Color(0xFF1E293B),
        title: Text('Vincular Celular de ${_currentChild.name}', style: const TextStyle(fontWeight: FontWeight.bold)),
        content: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            const Text(
              'En el teléfono del menor, abre FamilyGuard Companion e introduce este código de vinculación:',
              style: TextStyle(fontSize: 13, color: Color(0xFF94A3B8)),
              textAlign: TextAlign.center,
            ),
            const SizedBox(height: 20),
            Container(
              padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 16),
              decoration: BoxDecoration(
                color: const Color(0xFF0F172A),
                borderRadius: BorderRadius.circular(12),
                border: Border.all(color: const Color(0xFF38BDF8), width: 1.5),
              ),
              child: Text(
                _currentChild.pairingCode,
                style: const TextStyle(fontSize: 32, fontWeight: FontWeight.bold, letterSpacing: 8, color: Color(0xFF38BDF8)),
              ),
            ),
            const SizedBox(height: 16),
            const Text(
              'PIN Familiar de Emergencia: 1234',
              style: TextStyle(fontSize: 12, color: Color(0xFF10B981), fontWeight: FontWeight.bold),
            ),
          ],
        ),
        actions: [
          ElevatedButton(
            style: ElevatedButton.styleFrom(backgroundColor: const Color(0xFF38BDF8), foregroundColor: Colors.black),
            onPressed: () => Navigator.pop(ctx),
            child: const Text('Cerrar'),
          ),
        ],
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        backgroundColor: const Color(0xFF0F172A),
        elevation: 0,
        title: Row(
          children: [
            const Text('🛡️ ', style: TextStyle(fontSize: 22)),
            const Text('FamilyGuard 2.0', style: TextStyle(fontWeight: FontWeight.bold, fontSize: 18)),
            const SizedBox(width: 8),
            Container(
              padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 2),
              decoration: BoxDecoration(color: const Color(0xFFF59E0B), borderRadius: BorderRadius.circular(4)),
              child: const Text('PRO', style: TextStyle(fontSize: 10, fontWeight: FontWeight.bold, color: Colors.black)),
            ),
          ],
        ),
        actions: [
          // Selector de Hijo
          DropdownButtonHideUnderline(
            child: DropdownButton<int>(
              value: _selectedChildIndex,
              dropdownColor: const Color(0xFF1E293B),
              icon: const Icon(Icons.arrow_drop_down, color: Color(0xFF38BDF8)),
              items: [
                ...List.generate(_children.length, (index) {
                  final c = _children[index];
                  return DropdownMenuItem(
                    value: index,
                    child: Row(
                      children: [
                        Text(c.avatar, style: const TextStyle(fontSize: 18)),
                        const SizedBox(width: 6),
                        Text('${c.name} (${c.age}a)', style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 13)),
                      ],
                    ),
                  );
                }),
              ],
              onChanged: (val) {
                if (val != null) {
                  setState(() => _selectedChildIndex = val);
                }
              },
            ),
          ),
          IconButton(
            icon: const Icon(Icons.person_add_alt_1, color: Color(0xFF38BDF8), size: 20),
            tooltip: 'Añadir Hijo',
            onPressed: _showAddChildDialog,
          ),
          IconButton(
            icon: const Icon(Icons.qr_code, color: Color(0xFF38BDF8), size: 20),
            tooltip: 'Código de Vinculación',
            onPressed: _showPairingCodeDialog,
          ),
          const SizedBox(width: 8),
        ],
      ),
      bottomNavigationBar: NavigationBar(
        backgroundColor: const Color(0xFF0F172A),
        selectedIndex: _selectedTabIndex,
        indicatorColor: const Color(0xFF0284C7),
        onDestinationSelected: (idx) => setState(() => _selectedTabIndex = idx),
        destinations: const [
          NavigationDestination(icon: Icon(Icons.dashboard_outlined), selectedIcon: Icon(Icons.dashboard), label: 'Resumen'),
          NavigationDestination(icon: Icon(Icons.apps_outlined), selectedIcon: Icon(Icons.apps), label: 'Aplicaciones'),
          NavigationDestination(icon: Icon(Icons.schedule_outlined), selectedIcon: Icon(Icons.schedule), label: 'Horarios'),
          NavigationDestination(icon: Icon(Icons.devices_outlined), selectedIcon: Icon(Icons.devices), label: 'Dispositivos'),
        ],
      ),
      body: _buildCurrentTab(),
    );
  }

  Widget _buildCurrentTab() {
    switch (_selectedTabIndex) {
      case 0:
        return _buildOverviewTab();
      case 1:
        return _buildAppsTab();
      case 2:
        return _buildSchedulesTab();
      case 3:
        return _buildDevicesTab();
      default:
        return _buildOverviewTab();
    }
  }

  Widget _buildOverviewTab() {
    final isLocked = _currentChild.instantLockActive;

    return ListView(
      padding: const EdgeInsets.all(16),
      children: [
        // Tarjeta de Pausa Remota Instantánea
        Container(
          padding: const EdgeInsets.all(20),
          decoration: BoxDecoration(
            gradient: LinearGradient(
              colors: isLocked
                  ? [const Color(0xFF7F1D1D), const Color(0xFF450A0A)]
                  : [const Color(0xFF064E3B), const Color(0xFF022C22)],
            ),
            borderRadius: BorderRadius.circular(16),
            border: Border.all(
              color: isLocked ? const Color(0xFFEF4444) : const Color(0xFF10B981),
              width: 1.5,
            ),
          ),
          child: Row(
            children: [
              Icon(
                isLocked ? Icons.lock : Icons.lock_open,
                color: isLocked ? const Color(0xFFF87171) : const Color(0xFF34D399),
                size: 44,
              ),
              const SizedBox(width: 16),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      isLocked ? 'Pausa Instantánea ACTIVA' : 'Dispositivos Activos y Protegidos',
                      style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 16),
                    ),
                    const SizedBox(height: 4),
                    Text(
                      isLocked
                          ? 'Todos los celulares de ${_currentChild.name} están bloqueados remotamente.'
                          : 'Uso permitido dentro de los límites y horarios establecidos.',
                      style: const TextStyle(fontSize: 12, color: Color(0xFFCBD5E1)),
                    ),
                  ],
                ),
              ),
              ElevatedButton(
                style: ElevatedButton.styleFrom(
                  backgroundColor: isLocked ? const Color(0xFF10B981) : const Color(0xFFEF4444),
                  foregroundColor: Colors.white,
                  padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
                  shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(8)),
                ),
                onPressed: _isActionInProgress ? null : _toggleInstantLock,
                child: Text(isLocked ? 'Reanudar Acceso' : 'Bloquear Ahora'),
              ),
            ],
          ),
        ),
        const SizedBox(height: 20),

        // Métricas de Pantalla
        Row(
          children: [
            Expanded(
              child: Card(
                shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
                child: Padding(
                  padding: const EdgeInsets.all(16),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      const Text('TIEMPO EN PANTALLA', style: TextStyle(fontSize: 11, color: Color(0xFF94A3B8), fontWeight: FontWeight.bold)),
                      const SizedBox(height: 8),
                      Text('${(_currentChild.usedTodayMinutes / 60).floor()}h ${_currentChild.usedTodayMinutes % 60}m', style: const TextStyle(fontSize: 26, fontWeight: FontWeight.bold, color: Colors.white)),
                      const SizedBox(height: 4),
                      Text('Límite: ${_currentChild.dailyLimitMinutes} min/día', style: const TextStyle(fontSize: 11, color: Color(0xFF38BDF8))),
                    ],
                  ),
                ),
              ),
            ),
            const SizedBox(width: 12),
            Expanded(
              child: Card(
                shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
                child: Padding(
                  padding: const EdgeInsets.all(16),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      const Text('APPS BLOQUEADAS', style: TextStyle(fontSize: 11, color: Color(0xFF94A3B8), fontWeight: FontWeight.bold)),
                      const SizedBox(height: 8),
                      Text('${_currentChild.appRules.where((a) => a.isBlocked).length}', style: const TextStyle(fontSize: 26, fontWeight: FontWeight.bold, color: Color(0xFFF59E0B))),
                      const SizedBox(height: 4),
                      Text('De ${_currentChild.appRules.length} supervisadas', style: const TextStyle(fontSize: 11, color: Color(0xFF94A3B8))),
                    ],
                  ),
                ),
              ),
            ),
          ],
        ),
        const SizedBox(height: 20),

        // Dispositivos Vinculados
        Row(
          mainAxisAlignment: MainAxisAlignment.spaceBetween,
          children: [
            const Text('DISPOSITIVOS SUPERVISADOS', style: TextStyle(fontSize: 12, fontWeight: FontWeight.bold, color: Color(0xFF94A3B8), letterSpacing: 1.1)),
            TextButton.icon(
              icon: const Icon(Icons.add, size: 16, color: Color(0xFF38BDF8)),
              label: const Text('Vincular Celular', style: TextStyle(fontSize: 12, color: Color(0xFF38BDF8))),
              onPressed: _showPairingCodeDialog,
            ),
          ],
        ),
        const SizedBox(height: 8),
        if (_currentChild.devices.isEmpty)
          Card(
            shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
            child: const Padding(
              padding: EdgeInsets.all(16),
              child: Center(
                child: Text('No hay celulares vinculados aún. Usa "Vincular Celular".', style: TextStyle(color: Color(0xFF94A3B8), fontSize: 13)),
              ),
            ),
          )
        else
          ..._currentChild.devices.map((dev) => Card(
                shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
                margin: const EdgeInsets.symmetric(vertical: 4),
                child: ListTile(
                  leading: const Icon(Icons.phone_android, color: Color(0xFF38BDF8), size: 32),
                  title: Text(dev.name, style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 14)),
                  subtitle: Text('${dev.osVersion} • Batería ${dev.battery}%', style: const TextStyle(fontSize: 12, color: Color(0xFF94A3B8))),
                  trailing: Container(
                    padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
                    decoration: BoxDecoration(
                      color: dev.isOnline ? const Color(0xFF022C22) : const Color(0xFF334155),
                      borderRadius: BorderRadius.circular(12),
                    ),
                    child: Text(
                      dev.isOnline ? 'En Línea' : 'Desconectado',
                      style: TextStyle(
                        fontSize: 11,
                        fontWeight: FontWeight.bold,
                        color: dev.isOnline ? const Color(0xFF10B981) : const Color(0xFF94A3B8),
                      ),
                    ),
                  ),
                ),
              )),
      ],
    );
  }

  Widget _buildAppsTab() {
    return ListView(
      padding: const EdgeInsets.all(16),
      children: [
        Row(
          mainAxisAlignment: MainAxisAlignment.spaceBetween,
          children: [
            Text('REGLAS DE APLICACIONES (${_currentChild.name})', style: const TextStyle(fontSize: 12, fontWeight: FontWeight.bold, color: Color(0xFF94A3B8), letterSpacing: 1.1)),
            Text('v$_policyVersion', style: const TextStyle(fontSize: 12, color: Color(0xFF38BDF8), fontWeight: FontWeight.bold)),
          ],
        ),
        const SizedBox(height: 12),
        ..._currentChild.appRules.map((app) => Card(
              shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
              margin: const EdgeInsets.symmetric(vertical: 4),
              child: SwitchListTile(
                title: Text(app.name, style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 14)),
                subtitle: Text('${app.category} • ${app.packageName}', style: const TextStyle(fontSize: 11, color: Color(0xFF94A3B8))),
                secondary: Icon(
                  app.isBlocked ? Icons.block : Icons.check_circle_outline,
                  color: app.isBlocked ? const Color(0xFFEF4444) : const Color(0xFF10B981),
                ),
                activeThumbColor: const Color(0xFFEF4444),
                value: app.isBlocked,
                onChanged: (val) => _toggleAppRule(app, val),
              ),
            )),
      ],
    );
  }

  Widget _buildSchedulesTab() {
    return ListView(
      padding: const EdgeInsets.all(16),
      children: [
        const Text('HORARIOS DE ESTUDIO Y DESCANSO', style: TextStyle(fontSize: 12, fontWeight: FontWeight.bold, color: Color(0xFF94A3B8), letterSpacing: 1.1)),
        const SizedBox(height: 12),
        Card(
          shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
          child: SwitchListTile(
            title: const Text('Horario de Sueño (Bedtime)', style: TextStyle(fontWeight: FontWeight.bold)),
            subtitle: const Text('Bloquea las apps de 21:00 a 07:00 todos los días', style: TextStyle(fontSize: 12, color: Color(0xFF94A3B8))),
            secondary: const Icon(Icons.bedtime, color: Color(0xFFF59E0B)),
            value: _currentChild.bedtimeActive,
            onChanged: (val) {
              setState(() {
                _currentChild.bedtimeActive = val;
                _policyVersion++;
              });
            },
          ),
        ),
        const SizedBox(height: 8),
        Card(
          shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
          child: SwitchListTile(
            title: const Text('Horario Escolar (School Mode)', style: TextStyle(fontWeight: FontWeight.bold)),
            subtitle: const Text('Bloquea redes y juegos de 08:00 a 14:00 (Lunes a Viernes)', style: TextStyle(fontSize: 12, color: Color(0xFF94A3B8))),
            secondary: const Icon(Icons.school, color: Color(0xFF38BDF8)),
            value: _currentChild.schoolActive,
            onChanged: (val) {
              setState(() {
                _currentChild.schoolActive = val;
                _policyVersion++;
              });
            },
          ),
        ),
      ],
    );
  }

  Widget _buildDevicesTab() {
    return ListView(
      padding: const EdgeInsets.all(16),
      children: [
        const Text('ADMINISTRACIÓN DE DISPOSITIVOS', style: TextStyle(fontSize: 12, fontWeight: FontWeight.bold, color: Color(0xFF94A3B8), letterSpacing: 1.1)),
        const SizedBox(height: 12),
        ..._currentChild.devices.map((dev) => Card(
              shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
              child: Padding(
                padding: const EdgeInsets.all(16),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Row(
                      mainAxisAlignment: MainAxisAlignment.spaceBetween,
                      children: [
                        Text(dev.name, style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 16)),
                        Container(
                          padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 2),
                          decoration: BoxDecoration(color: const Color(0xFF022C22), borderRadius: BorderRadius.circular(8)),
                          child: const Text('PROTEGIDO', style: TextStyle(fontSize: 10, color: Color(0xFF10B981), fontWeight: FontWeight.bold)),
                        ),
                      ],
                    ),
                    const SizedBox(height: 8),
                    Text('Sistema Operativo: ${dev.osVersion}', style: const TextStyle(fontSize: 13, color: Color(0xFF94A3B8))),
                    Text('Batería Reportada: ${dev.battery}%', style: const TextStyle(fontSize: 13, color: Color(0xFF94A3B8))),
                    const SizedBox(height: 16),
                    SizedBox(
                      width: double.infinity,
                      child: OutlinedButton.icon(
                        style: OutlinedButton.styleFrom(foregroundColor: const Color(0xFFEF4444), side: const BorderSide(color: Color(0xFFEF4444))),
                        icon: const Icon(Icons.delete_outline),
                        label: const Text('Desvincular Dispositivo'),
                        onPressed: () {
                          setState(() => _currentChild.devices.remove(dev));
                        },
                      ),
                    ),
                  ],
                ),
              ),
            )),
      ],
    );
  }
}
