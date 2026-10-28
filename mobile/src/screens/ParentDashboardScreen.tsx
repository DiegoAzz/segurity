import React, { useState } from 'react';
import {
  StyleSheet,
  Text,
  View,
  ScrollView,
  TouchableOpacity,
  Alert,
} from 'react-native';

export default function ParentDashboardScreen() {
  const [instantLock, setInstantLock] = useState(false);
  const [activeChild, setActiveChild] = useState('Lucas (11 años)');

  const toggleInstantLock = () => {
    const nextState = !instantLock;
    setInstantLock(nextState);
    Alert.alert(
      nextState ? 'Pausa Instantánea Activada' : 'Pausa Desactivada',
      nextState
        ? 'Todos los dispositivos de Lucas han sido bloqueados inmediatamente.'
        : 'Los dispositivos han sido reanudados.'
    );
  };

  return (
    <ScrollView style={styles.container} contentContainerStyle={{ padding: 16 }}>
      
      {/* Child Switcher */}
      <View style={styles.childBar}>
        {['Lucas (11 años)', 'Sofía (14 años)'].map((name, i) => (
          <TouchableOpacity
            key={i}
            style={[styles.childChip, activeChild === name && styles.childChipActive]}
            onPress={() => setActiveChild(name)}
          >
            <Text style={[styles.childChipText, activeChild === name && styles.childChipTextActive]}>
              {name}
            </Text>
          </TouchableOpacity>
        ))}
      </View>

      {/* Instant Lock Callout */}
      <TouchableOpacity
        style={[styles.lockCard, instantLock && styles.lockCardActive]}
        onPress={toggleInstantLock}
      >
        <Text style={{ fontSize: 26 }}>{instantLock ? '🔒' : '🛡️'}</Text>
        <View style={{ marginLeft: 12, flex: 1 }}>
          <Text style={styles.lockCardTitle}>
            {instantLock ? '¡PAUSA INSTANTÁNEA ACTIVA!' : 'Pausa Instantánea'}
          </Text>
          <Text style={styles.lockCardSub}>
            {instantLock
              ? 'Toca para reanudar el uso de dispositivos'
              : 'Bloquea todos los dispositivos de Lucas con un toque'}
          </Text>
        </View>
      </TouchableOpacity>

      {/* Screen Time Metric */}
      <View style={styles.card}>
        <View style={styles.cardHeader}>
          <Text style={styles.cardTitle}>TIEMPO EN PANTALLA HOY</Text>
          <Text style={styles.cardAction}>+30 min</Text>
        </View>
        <View style={styles.metricRow}>
          <Text style={styles.metricMain}>1h 15m</Text>
          <Text style={styles.metricLimit}>/ 2h 00m límite</Text>
        </View>
        <View style={styles.progressBarBg}>
          <View style={[styles.progressBarFill, { width: '62%' }]} />
        </View>
        <Text style={styles.metricFooter}>Quedan 45 minutos permitidos</Text>
      </View>

      {/* GPS Location Snapshot */}
      <View style={styles.card}>
        <View style={styles.cardHeader}>
          <Text style={styles.cardTitle}>ÚLTIMA UBICACIÓN CONOCIDA</Text>
          <Text style={styles.statusOnline}>● En vivo</Text>
        </View>
        <Text style={styles.locationZone}>🏫 Colegio San Patricio</Text>
        <Text style={styles.locationAddress}>Av. de la Paz 42, Madrid (Hace 3 min)</Text>
      </View>

      {/* Pending Requests */}
      <View style={styles.card}>
        <View style={styles.cardHeader}>
          <Text style={styles.cardTitle}>SOLICITUDES PENDIENTES (1)</Text>
        </View>
        <View style={styles.requestItem}>
          <Text style={styles.requestTitle}>Lucas pide +30 minutos de tiempo</Text>
          <Text style={styles.requestMsg}>"Papá, estoy terminando la maqueta de Ciencias con mi equipo."</Text>
          <View style={styles.requestActions}>
            <TouchableOpacity
              style={styles.btnReject}
              onPress={() => Alert.alert('Rechazada', 'Has rechazado la solicitud.')}
            >
              <Text style={styles.btnRejectText}>Rechazar</Text>
            </TouchableOpacity>
            <TouchableOpacity
              style={styles.btnApprove}
              onPress={() => Alert.alert('Aprobada', 'Se añadieron +30 minutos.')}
            >
              <Text style={styles.btnApproveText}>Aprobar +30m</Text>
            </TouchableOpacity>
          </View>
        </View>
      </View>

    </ScrollView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#0B0F19',
  },
  childBar: {
    flexDirection: 'row',
    marginBottom: 16,
  },
  childChip: {
    paddingHorizontal: 14,
    paddingVertical: 8,
    borderRadius: 12,
    backgroundColor: '#161F30',
    marginRight: 8,
    borderWidth: 1,
    borderColor: '#283548',
  },
  childChipActive: {
    backgroundColor: '#FFB800',
    borderColor: '#FFB800',
  },
  childChipText: {
    color: '#94A3B8',
    fontSize: 12,
    fontWeight: '700',
  },
  childChipTextActive: {
    color: '#000000',
    fontWeight: '800',
  },
  lockCard: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: '#1E293B',
    borderRadius: 16,
    borderWidth: 1,
    borderColor: '#FFB800',
    padding: 16,
    marginBottom: 16,
  },
  lockCardActive: {
    backgroundColor: '#7F1D1D',
    borderColor: '#EF4444',
  },
  lockCardTitle: {
    fontSize: 15,
    fontWeight: '800',
    color: '#FFFFFF',
  },
  lockCardSub: {
    fontSize: 12,
    color: '#94A3B8',
    marginTop: 2,
  },
  card: {
    backgroundColor: '#161F30',
    borderRadius: 16,
    borderWidth: 1,
    borderColor: '#283548',
    padding: 16,
    marginBottom: 16,
  },
  cardHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 10,
  },
  cardTitle: {
    fontSize: 11,
    fontWeight: '700',
    color: '#64748B',
    letterSpacing: 1,
  },
  cardAction: {
    fontSize: 12,
    fontWeight: '800',
    color: '#FFB800',
  },
  metricRow: {
    flexDirection: 'row',
    alignItems: 'baseline',
    marginBottom: 8,
  },
  metricMain: {
    fontSize: 28,
    fontWeight: '900',
    color: '#FFFFFF',
  },
  metricLimit: {
    fontSize: 13,
    color: '#94A3B8',
    marginLeft: 6,
  },
  progressBarBg: {
    height: 8,
    backgroundColor: '#0F172A',
    borderRadius: 4,
    overflow: 'hidden',
    marginBottom: 8,
  },
  progressBarFill: {
    height: '100%',
    backgroundColor: '#FFB800',
    borderRadius: 4,
  },
  metricFooter: {
    fontSize: 12,
    color: '#10B981',
    fontWeight: '600',
  },
  statusOnline: {
    fontSize: 11,
    color: '#10B981',
    fontWeight: '700',
  },
  locationZone: {
    fontSize: 15,
    fontWeight: '800',
    color: '#FFFFFF',
  },
  locationAddress: {
    fontSize: 12,
    color: '#94A3B8',
    marginTop: 2,
  },
  requestItem: {
    backgroundColor: '#0F172A',
    padding: 12,
    borderRadius: 12,
    borderWidth: 1,
    borderColor: '#283548',
  },
  requestTitle: {
    fontSize: 13,
    fontWeight: '700',
    color: '#FFB800',
    marginBottom: 4,
  },
  requestMsg: {
    fontSize: 12,
    color: '#E2E8F0',
    fontStyle: 'italic',
    marginBottom: 10,
  },
  requestActions: {
    flexDirection: 'row',
    justifyContent: 'flex-end',
  },
  btnReject: {
    paddingHorizontal: 12,
    paddingVertical: 6,
    borderRadius: 8,
    backgroundColor: '#1E293B',
    marginRight: 8,
  },
  btnRejectText: {
    color: '#94A3B8',
    fontSize: 12,
    fontWeight: '600',
  },
  btnApprove: {
    paddingHorizontal: 14,
    paddingVertical: 6,
    borderRadius: 8,
    backgroundColor: '#FFB800',
  },
  btnApproveText: {
    color: '#000000',
    fontSize: 12,
    fontWeight: '800',
  },
});
