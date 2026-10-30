import 'package:flutter/material.dart';

void main() {
  WidgetsFlutterBinding.ensureInitialized();
  runApp(const FamilyGuardAdminApp());
}

class FamilyGuardAdminApp extends StatelessWidget {
  const FamilyGuardAdminApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'FamilyGuard 2.0 — Consola de Administración SaaS',
      debugShowCheckedModeBanner: false,
      theme: ThemeData.dark().copyWith(
        scaffoldBackgroundColor: const Color(0xFF090D16),
        cardColor: const Color(0xFF131B2E),
        colorScheme: const ColorScheme.dark(
          primary: Color(0xFF38BDF8),
          secondary: Color(0xFF10B981),
          error: Color(0xFFEF4444),
          surface: Color(0xFF131B2E),
        ),
      ),
      home: const AdminConsoleScreen(),
    );
  }
}

class AdminConsoleScreen extends StatefulWidget {
  const AdminConsoleScreen({super.key});

  @override
  State<AdminConsoleScreen> createState() => _AdminConsoleScreenState();
}

class _AdminConsoleScreenState extends State<AdminConsoleScreen> {
  int _selectedTabIndex = 0;

  final List<Map<String, dynamic>> _families = [
    {
      'id': 'fam_1',
      'name': 'Familia Zuñiga',
      'parent': 'Diego Zúñiga',
      'email': 'diego.zuniga@gmail.com',
      'plan': 'PRO_FAMILY',
      'childrenCount': 1,
      'devicesCount': 1,
      'status': 'ACTIVO',
      'createdAt': '2026-10-30',
    },
    {
      'id': 'fam_2',
      'name': 'Familia Rodríguez',
      'parent': 'Carlos Rodríguez',
      'email': 'carlos.rodriguez@outlook.com',
      'plan': 'PRO_FAMILY',
      'childrenCount': 2,
      'devicesCount': 2,
      'status': 'ACTIVO',
      'createdAt': '2026-10-28',
    },
    {
      'id': 'fam_3',
      'name': 'Familia Gómez',
      'parent': 'Laura Gómez',
      'email': 'laura.gomez@gmail.com',
      'plan': 'STARTER',
      'childrenCount': 1,
      'devicesCount': 1,
      'status': 'TRIAL',
      'createdAt': '2026-10-29',
    },
  ];

  final List<Map<String, dynamic>> _auditLogs = [
    {'time': '12:40:12', 'family': 'Familia Zuñiga', 'action': 'POLICY_APPLIED', 'device': 'OPPO CPH2743', 'status': 'ÉXITO (v2)'},
    {'time': '12:38:45', 'family': 'Familia Rodríguez', 'action': 'INSTANT_LOCK_ON', 'device': 'Xiaomi Redmi 13', 'status': 'CONFIRMADO'},
    {'time': '12:35:10', 'family': 'Familia Gómez', 'action': 'DEVICE_ENROLLED', 'device': 'Samsung A54', 'status': 'REGISTRADO'},
  ];

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
              decoration: BoxDecoration(color: const Color(0xFFDC2626), borderRadius: BorderRadius.circular(4)),
              child: const Text('SUPER ADMIN', style: TextStyle(fontSize: 10, fontWeight: FontWeight.bold, color: Colors.white)),
            ),
          ],
        ),
        actions: [
          Container(
            padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
            margin: const EdgeInsets.symmetric(vertical: 10),
            decoration: BoxDecoration(
              color: const Color(0xFF022C22),
              borderRadius: BorderRadius.circular(8),
              border: Border.all(color: const Color(0xFF10B981)),
            ),
            child: const Row(
              children: [
                Icon(Icons.circle, color: Color(0xFF34D399), size: 10),
                SizedBox(width: 8),
                Text('SISTEMA EN LÍNEA', style: TextStyle(fontSize: 11, fontWeight: FontWeight.bold, color: Color(0xFF10B981))),
              ],
            ),
          ),
          const SizedBox(width: 16),
        ],
      ),
      body: Row(
        children: [
          // Barra lateral de navegación
          NavigationRail(
            backgroundColor: const Color(0xFF0F172A),
            selectedIndex: _selectedTabIndex,
            onDestinationSelected: (idx) => setState(() => _selectedTabIndex = idx),
            labelType: NavigationRailLabelType.all,
            indicatorColor: const Color(0xFF0284C7),
            destinations: const [
              NavigationRailDestination(icon: Icon(Icons.analytics_outlined), selectedIcon: Icon(Icons.analytics), label: Text('Métricas')),
              NavigationRailDestination(icon: Icon(Icons.people_outline), selectedIcon: Icon(Icons.people), label: Text('Familias')),
              NavigationRailDestination(icon: Icon(Icons.security_outlined), selectedIcon: Icon(Icons.security), label: Text('Auditoría')),
              NavigationRailDestination(icon: Icon(Icons.dns_outlined), selectedIcon: Icon(Icons.dns), label: Text('Servicios')),
            ],
          ),
          const VerticalDivider(width: 1, color: Color(0xFF1E293B)),
          Expanded(child: _buildCurrentTab()),
        ],
      ),
    );
  }

  Widget _buildCurrentTab() {
    switch (_selectedTabIndex) {
      case 0:
        return _buildMetricsTab();
      case 1:
        return _buildFamiliesTab();
      case 2:
        return _buildAuditTab();
      case 3:
        return _buildServicesTab();
      default:
        return _buildMetricsTab();
    }
  }

  Widget _buildMetricsTab() {
    return ListView(
      padding: const EdgeInsets.all(24),
      children: [
        const Text('PANEL GENERAL DE GESTIÓN SAAS', style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold)),
        const SizedBox(height: 16),
        Row(
          children: [
            _buildStatCard('Familias Registradas', '1,280', Icons.home_work, const Color(0xFF38BDF8), '+12% esta semana'),
            const SizedBox(width: 16),
            _buildStatCard('Dispositivos Activos', '3,410', Icons.phone_android, const Color(0xFF10B981), '99.4% en línea'),
            const SizedBox(width: 16),
            _buildStatCard('Ingreso Mensual (MRR)', '\$21,050 USD', Icons.monetization_on, const Color(0xFFF59E0B), '842 planes PRO'),
            const SizedBox(width: 16),
            _buildStatCard('Tasa de Evasión', '0.00%', Icons.verified_user, const Color(0xFF34D399), 'Cero bypass detectado'),
          ],
        ),
        const SizedBox(height: 24),
        Card(
          shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
          child: Padding(
            padding: const EdgeInsets.all(20),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const Text('ESTADO DE LA INFRAESTRUCTURA DE PRODUCCIÓN (COOLIFY)', style: TextStyle(fontWeight: FontWeight.bold, fontSize: 13, color: Color(0xFF94A3B8))),
                const SizedBox(height: 16),
                _buildServiceRow('API Gateway (NestJS)', 'http://localhost:3000/api/v1', 'Activo • 12ms', true),
                _buildServiceRow('PostgreSQL 16 (Multi-tenant)', 'familyguard_v2 @ 5432', 'Conectado • 18 pool conn', true),
                _buildServiceRow('Redis 7 (BullMQ & Cache)', 'redis://localhost:6379', 'Memoria: 42MB • 0 tareas pendientes', true),
                _buildServiceRow('Motor de Políticas (Kotlin)', 'v2.0.0 (Anti-bypass estricto)', 'Deduplicación activa', true),
              ],
            ),
          ),
        ),
      ],
    );
  }

  Widget _buildFamiliesTab() {
    return ListView(
      padding: const EdgeInsets.all(24),
      children: [
        Row(
          mainAxisAlignment: MainAxisAlignment.spaceBetween,
          children: [
            const Text('GESTIÓN DE FAMILIAS Y CLIENTES', style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold)),
            ElevatedButton.icon(
              style: ElevatedButton.styleFrom(backgroundColor: const Color(0xFF38BDF8), foregroundColor: Colors.black),
              icon: const Icon(Icons.add),
              label: const Text('Crear Familia Manualmente'),
              onPressed: () {},
            ),
          ],
        ),
        const SizedBox(height: 16),
        Card(
          shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
          child: DataTable(
            columns: const [
              DataColumn(label: Text('Familia')),
              DataColumn(label: Text('Padre Responsable')),
              DataColumn(label: Text('Email')),
              DataColumn(label: Text('Plan')),
              DataColumn(label: Text('Hijos / Disp')),
              DataColumn(label: Text('Estado')),
            ],
            rows: _families.map((fam) {
              return DataRow(cells: [
                DataCell(Text(fam['name'], style: const TextStyle(fontWeight: FontWeight.bold))),
                DataCell(Text(fam['parent'])),
                DataCell(Text(fam['email'])),
                DataCell(Container(
                  padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 2),
                  decoration: BoxDecoration(color: const Color(0xFF0284C7), borderRadius: BorderRadius.circular(4)),
                  child: Text(fam['plan'], style: const TextStyle(fontSize: 11, fontWeight: FontWeight.bold)),
                )),
                DataCell(Text('${fam['childrenCount']} hijos • ${fam['devicesCount']} disp')),
                DataCell(Text(fam['status'], style: const TextStyle(color: Color(0xFF10B981), fontWeight: FontWeight.bold))),
              ]);
            }).toList(),
          ),
        ),
      ],
    );
  }

  Widget _buildAuditTab() {
    return ListView(
      padding: const EdgeInsets.all(24),
      children: [
        const Text('REGISTRO DE AUDITORÍA Y TRAZABILIDAD EN TIEMPO REAL', style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold)),
        const SizedBox(height: 16),
        Card(
          shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
          child: ListView.separated(
            shrinkWrap: true,
            physics: const NeverScrollableScrollPhysics(),
            itemCount: _auditLogs.length,
            separatorBuilder: (_, _) => const Divider(height: 1),
            itemBuilder: (ctx, idx) {
              final log = _auditLogs[idx];
              return ListTile(
                leading: const Icon(Icons.shield, color: Color(0xFF38BDF8)),
                title: Text('${log['action']} • ${log['family']}', style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 13)),
                subtitle: Text('Dispositivo: ${log['device']} • Resultado: ${log['status']}', style: const TextStyle(fontSize: 11, color: Color(0xFF94A3B8))),
                trailing: Text(log['time'], style: const TextStyle(fontSize: 12, color: Color(0xFF64748B))),
              );
            },
          ),
        ),
      ],
    );
  }

  Widget _buildServicesTab() {
    return ListView(
      padding: const EdgeInsets.all(24),
      children: [
        const Text('MONITOREO DE SERVICIOS Y RECURSOS DEL SERVIDOR', style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold)),
        const SizedBox(height: 16),
        Card(
          shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
          child: Padding(
            padding: const EdgeInsets.all(20),
            child: Column(
              children: [
                _buildServiceRow('PostgreSQL 16 Multi-Tenant', 'Volumen: familyguard_v2_pgdata', 'Saludable', true),
                _buildServiceRow('Redis 7 Cache Store', 'Volumen: familyguard_v2_redisdata', 'Saludable', true),
                _buildServiceRow('NestJS Backend API', 'Contenedor: familyguard_backend', 'Saludable', true),
                _buildServiceRow('Coolify Reverse Proxy', 'Traefik HTTPS SSL Let\'s Encrypt', 'Saludable', true),
              ],
            ),
          ),
        ),
      ],
    );
  }

  Widget _buildStatCard(String title, String value, IconData icon, Color color, String sub) {
    return Expanded(
      child: Card(
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
        child: Padding(
          padding: const EdgeInsets.all(20),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Icon(icon, color: color, size: 28),
              const SizedBox(height: 12),
              Text(value, style: const TextStyle(fontSize: 22, fontWeight: FontWeight.bold, color: Colors.white)),
              const SizedBox(height: 4),
              Text(title, style: const TextStyle(fontSize: 12, color: Color(0xFF94A3B8))),
              const SizedBox(height: 8),
              Text(sub, style: TextStyle(fontSize: 10, color: color, fontWeight: FontWeight.bold)),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildServiceRow(String name, String detail, String status, bool isHealthy) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 8),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: [
          Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(name, style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 14)),
              Text(detail, style: const TextStyle(fontSize: 11, color: Color(0xFF94A3B8))),
            ],
          ),
          Container(
            padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
            decoration: BoxDecoration(
              color: isHealthy ? const Color(0xFF022C22) : const Color(0xFF450A0A),
              borderRadius: BorderRadius.circular(8),
              border: Border.all(color: isHealthy ? const Color(0xFF10B981) : const Color(0xFFEF4444)),
            ),
            child: Text(
              status,
              style: TextStyle(fontSize: 11, fontWeight: FontWeight.bold, color: isHealthy ? const Color(0xFF34D399) : const Color(0xFFF87171)),
            ),
          ),
        ],
      ),
    );
  }
}
