import 'package:flutter_test/flutter_test.dart';
import 'package:admin_flutter/main.dart';

void main() {
  testWidgets('Admin console smoke test', (WidgetTester tester) async {
    await tester.pumpWidget(const FamilyGuardAdminApp());
    expect(find.text('FamilyGuard 2.0'), findsOneWidget);
  });
}
