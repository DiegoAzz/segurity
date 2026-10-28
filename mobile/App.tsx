import React, { useState, useEffect } from 'react';
import { StyleSheet, Text, View, SafeAreaView, TouchableOpacity, StatusBar } from 'react-native';
import ChildHomeScreen from './src/screens/ChildHomeScreen';
import ParentDashboardScreen from './src/screens/ParentDashboardScreen';

export default function App() {
  const [appMode, setAppMode] = useState<'parent' | 'child'>('parent');

  return (
    <SafeAreaView style={styles.container}>
      <StatusBar barStyle="light-content" backgroundColor="#0B0F19" />
      
      {/* Top Bar Switcher between Parent Mode & Child Companion Mode */}
      <View style={styles.topSwitcher}>
        <View style={styles.brandRow}>
          <Text style={styles.brandTitle}>
            Norton <Text style={styles.brandHighlight}>Family</Text>
          </Text>
        </View>

        <View style={styles.segmentedControl}>
          <TouchableOpacity
            style={[styles.segmentBtn, appMode === 'parent' && styles.segmentBtnActive]}
            onPress={() => setAppMode('parent')}
          >
            <Text style={[styles.segmentText, appMode === 'parent' && styles.segmentTextActive]}>
              Modo Padres
            </Text>
          </TouchableOpacity>

          <TouchableOpacity
            style={[styles.segmentBtn, appMode === 'child' && styles.segmentBtnActive]}
            onPress={() => setAppMode('child')}
          >
            <Text style={[styles.segmentText, appMode === 'child' && styles.segmentTextActive]}>
              Modo Niño
            </Text>
          </TouchableOpacity>
        </View>
      </View>

      {/* Screen Body */}
      <View style={styles.content}>
        {appMode === 'parent' ? <ParentDashboardScreen /> : <ChildHomeScreen />}
      </View>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#0B0F19',
  },
  topSwitcher: {
    paddingHorizontal: 16,
    paddingVertical: 10,
    backgroundColor: '#111827',
    borderBottomWidth: 1,
    borderBottomColor: '#283548',
    flexDirection: 'row',
    justifyContent: 'between',
    alignItems: 'center',
  },
  brandRow: {
    flexDirection: 'row',
    alignItems: 'center',
  },
  brandTitle: {
    fontSize: 18,
    fontWeight: '900',
    color: '#FFFFFF',
  },
  brandHighlight: {
    color: '#FFB800',
  },
  segmentedControl: {
    flexDirection: 'row',
    backgroundColor: '#1E293B',
    borderRadius: 10,
    padding: 3,
  },
  segmentBtn: {
    paddingHorizontal: 12,
    paddingVertical: 6,
    borderRadius: 8,
  },
  segmentBtnActive: {
    backgroundColor: '#FFB800',
  },
  segmentText: {
    fontSize: 12,
    fontWeight: '600',
    color: '#94A3B8',
  },
  segmentTextActive: {
    color: '#000000',
    fontWeight: '800',
  },
  content: {
    flex: 1,
  },
});
