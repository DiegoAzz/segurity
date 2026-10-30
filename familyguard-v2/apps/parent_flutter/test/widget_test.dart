import 'package:flutter_test/flutter_test.dart';
import 'package:parent_flutter/main.dart';

void main() {
  testWidgets('Parent dashboard smoke test', (WidgetTester tester) async {
    await tester.pumpWidget(const FamilyGuardParentApp());
    expect(find.text('FamilyGuard 2.0'), findsOneWidget);
  });
}
