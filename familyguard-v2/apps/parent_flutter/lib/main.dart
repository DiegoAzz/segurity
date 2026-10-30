import 'package:flutter/material.dart';

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

  final List<ChildProfile> _children = [
    ChildProfile(
      id: 'child_1',
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
    ChildProfile(
      id: 'child_2',
      name: 'Mateo',
      age: 12,
      avatar: '👦',
      pairingCode: '458129',
      instantLockActive: false,
      dailyLimitMinutes: 90,
      bedtimeActive: false,
      schoolActive: false,
      appRules: [
        AppRule(name: 'Roblox', packageName: 'com.roblox.client', category: 'Juegos', isBlocked: true),
        AppRule(name: 'YouTube', packageName: 'com.google.android.youtube', category: 'Videos', isBlocked: false),
        AppRule(name: 'WhatsApp', packageName: 'com.whatsapp', category: 'Comunicación', isBlocked: false),
      ],
      devices: [
        DeviceItem(name: 'Xiaomi Redmi Note 13', osVersion: 'Android 14', battery: 92, isOnline: true),
      ],
    ),
  ];

  int _selectedChildIndex = 0;

  ChildProfile get _currentChild => _children[_selectedChildIndex];

  void _toggleInstantLock() {
    setState(() {
      _currentChild.instantLockActive = !_currentChild.instantLockActive;
      _policyVersion++;
    });

    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        backgroundColor: _currentChild.instantLockActive ? const Color(0xFFEF4444) : const Color(0xFF10B981),
        content: Text(
          _currentChild.instantLockActive
              ? '🚨 Bloqueo Inmediato Activado para ${_currentChild.name} (v$_policyVersion)'
              : '✅ Dispositivos de ${_currentChild.name} Reanudados (v$_policyVersion)',
          style: const TextStyle(fontWeight: FontWeight.bold),
        ),
      ),
    );
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
              onPressed: () {
                if (nameCtrl.text.trim().isNotEmpty) {
                  final newChild = ChildProfile(
                    id: 'child_${DateTime.now().millisecondsSinceEpoch}',
                    name: nameCtrl.text.trim(),
                    age: int.tryParse(ageCtrl.text) ?? 10,
                    avatar: selectedAvatar,
                    pairingCode: (100000 + _children.length * 777).toString(),
                    appRules: [
                      AppRule(name: 'Instagram', packageName: 'com.instagram.android', category: 'Redes Sociales', isBlocked: true),
                      AppRule(name: 'YouTube', packageName: 'com.google.android.youtube', category: 'Videos', isBlocked: false),
                    ],
                    devices: [],
                  );
                  setState(() {
                    _children.add(newChild);
                    _selectedChildIndex = _children.length - 1;
                  });
                  Navigator.pop(ctx);
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
                        const SizedBox(width: 8),
                        Text('${c.name} (${c.age}a)', style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 14)),
                      ],
                    ),
                  );
                }),
              ],
              onChanged: (val) {
                if (val != null) setState(() => _selectedChildIndex = val);
              },
            ),
          ),
          const SizedBox(width: 8),
          IconButton(
            icon: const Icon(Icons.person_add, color: Color(0xFF38BDF8)),
            tooltip: 'Registrar Hijo',
            onPressed: _showAddChildDialog,
          ),
          const SizedBox(width: 8),
          IconButton(
            icon: const Icon(Icons.qr_code, color: Color(0xFF34D399)),
            tooltip: 'Vincular Celular',
            onPressed: _showPairingCodeDialog,
          ),
          const SizedBox(width: 16),
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
                onPressed: _toggleInstantLock,
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
                      const Text('1h 15m', style: TextStyle(fontSize: 26, fontWeight: FontWeight.bold, color: Colors.white)),
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
                onChanged: (val) {
                  setState(() {
                    app.isBlocked = val;
                    _policyVersion++;
                  });
                },
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
