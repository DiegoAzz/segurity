import 'package:flutter_test/flutter_test.dart';
import 'package:child_flutter/main.dart';

void main() {
  testWidgets('FamilyGuard Child App smoke test', (WidgetTester tester) async {
    await tester.pumpWidget(const FamilyGuardChildApp());
    expect(find.text('FamilyGuard Companion'), findsOneWidget);
  });
}
