import React, { useState } from 'react';
import {
  StyleSheet,
  Text,
  View,
  TouchableOpacity,
  ScrollView,
  Alert,
  Modal,
  TextInput,
} from 'react-native';

export default function ChildHomeScreen() {
  const [isLocked, setIsLocked] = useState(false);
  const [remainingMinutes, setRemainingMinutes] = useState(45);
  const [modalVisible, setModalVisible] = useState(false);
  const [requestMessage, setRequestMessage] = useState('');
  const [selectedExtraTime, setSelectedExtraTime] = useState(30);

  const handleSendRequest = () => {
    Alert.alert(
      'Petición Enviada',
      `Tus padres han recibido tu solicitud de +${selectedExtraTime} minutos con tu mensaje. Te avisaremos cuando respondan.`,
      [{ text: 'Entendido', onPress: () => setModalVisible(false) }]
    );
  };

  const handleSOS = () => {
    Alert.alert(
      '🚨 ALERTA SOS ACTIVADA',
      'Tu ubicación GPS exacta y una notificación prioritaria han sido enviadas de inmediato a tus padres.',
      [{ text: 'Aceptar' }]
    );
  };

  if (isLocked) {
    return (
      <View style={styles.lockedContainer}>
        <View style={styles.lockIconCircle}>
          <Text style={{ fontSize: 36 }}>🔒</Text>
        </View>
        <Text style={styles.lockedTitle}>Dispositivo Pausado</Text>
        <Text style={styles.lockedSubtitle}>
          Pausa Instantánea activa o tiempo de pantalla diario agotado. Es hora de desconectarse o hablar con tus padres.
        </Text>

        <TouchableOpacity
          style={styles.requestTimeBtn}
          onPress={() => setModalVisible(true)}
        >
          <Text style={styles.requestTimeBtnText}>Pedir más tiempo a mis padres</Text>
        </TouchableOpacity>

        <TouchableOpacity
          style={styles.emergencyBtn}
          onPress={() => Alert.alert('Llamada', 'Iniciando llamada a Mamá...')}
        >
          <Text style={styles.emergencyBtnText}>📞 Llamada Familiar Permitida</Text>
        </TouchableOpacity>

        <TouchableOpacity
          style={styles.devToggleBtn}
          onPress={() => setIsLocked(false)}
        >
          <Text style={styles.devToggleText}>[Dev] Simular Desbloqueo</Text>
        </TouchableOpacity>
      </View>
    );
  }

  return (
    <ScrollView style={styles.container} contentContainerStyle={{ padding: 20 }}>
      {/* Norton Protection Status Bar */}
      <View style={styles.statusBarCard}>
        <View style={styles.statusInfo}>
          <Text style={styles.shieldIcon}>🛡️</Text>
          <View>
            <Text style={styles.statusTitle}>Norton Companion</Text>
            <Text style={styles.statusSubtitle}>● Protección Activa en este móvil</Text>
          </View>
        </View>
        <TouchableOpacity style={styles.sosBtn} onPress={handleSOS}>
          <Text style={styles.sosText}>SOS</Text>
        </TouchableOpacity>
      </View>

      {/* Countdown Card */}
      <View style={styles.countdownCard}>
        <Text style={styles.countdownLabel}>TIEMPO RESTANTE HOY</Text>
        <Text style={styles.countdownNumber}>{remainingMinutes} min</Text>
        <Text style={styles.countdownHelper}>Límite diario: 2h 00m</Text>

        <TouchableOpacity
          style={styles.askTimeBtn}
          onPress={() => setModalVisible(true)}
        >
          <Text style={styles.askTimeBtnText}>Pedir tiempo extra a Papá/Mamá</Text>
        </TouchableOpacity>
      </View>

      {/* Simulated Apps Section */}
      <View style={styles.sectionHeader}>
        <Text style={styles.sectionTitle}>MIS APLICACIONES SUPERVISADAS</Text>
      </View>

      <View style={styles.appsGrid}>
        {[
          { name: 'Duolingo', icon: '🦉', color: '#84CC16', status: 'Permitida' },
          { name: 'Roblox', icon: '🎮', color: '#DC2626', status: '35/45 min' },
          { name: 'YouTube', icon: '▶️', color: '#EF4444', status: 'Permitida' },
          { name: 'TikTok', icon: '🎵', color: '#1E293B', status: '28/30 min' },
          { name: 'WhatsApp', icon: '💬', color: '#10B981', status: 'Permitida' },
          { name: 'Brawl Stars', icon: '⭐', color: '#F59E0B', status: 'Permitida' },
        ].map((app, index) => (
          <TouchableOpacity
            key={index}
            style={styles.appItem}
            onPress={() => Alert.alert('App', `Abriendo ${app.name} bajo supervisión Norton.`)}
          >
            <View style={[styles.appIcon, { backgroundColor: app.color }]}>
              <Text style={{ fontSize: 24 }}>{app.icon}</Text>
            </View>
            <Text style={styles.appName}>{app.name}</Text>
            <Text style={styles.appStatus}>{app.status}</Text>
          </TouchableOpacity>
        ))}
      </View>

      {/* Emergency Family Call */}
      <TouchableOpacity
        style={styles.familyCallCard}
        onPress={() => Alert.alert('Contacto Familiar', 'Llamando al número registrado del tutor...')}
      >
        <Text style={{ fontSize: 20 }}>📞</Text>
        <View style={{ marginLeft: 12 }}>
          <Text style={styles.familyCallTitle}>Llamar a Papá / Mamá</Text>
          <Text style={styles.familyCallSubtitle}>Siempre disponible incluso con pantalla bloqueada</Text>
        </View>
      </TouchableOpacity>

      {/* Dev Toggle */}
      <TouchableOpacity
        style={styles.devToggleBtn}
        onPress={() => setIsLocked(true)}
      >
        <Text style={styles.devToggleText}>[Dev] Simular Bloqueo por Pausa Instantánea</Text>
      </TouchableOpacity>

      {/* Modal for asking time */}
      <Modal visible={modalVisible} transparent animationType="slide">
        <View style={styles.modalOverlay}>
          <View style={styles.modalContent}>
            <Text style={styles.modalTitle}>Solicitar Tiempo Adicional</Text>
            <Text style={styles.modalSubtitle}>
              Selecciona cuánto tiempo necesitas y escribe el motivo para tus padres:
            </Text>

            <View style={styles.timeButtonsRow}>
              {[15, 30, 60].map(mins => (
                <TouchableOpacity
                  key={mins}
                  style={[
                    styles.timeChoiceBtn,
                    selectedExtraTime === mins && styles.timeChoiceBtnActive,
                  ]}
                  onPress={() => setSelectedExtraTime(mins)}
                >
                  <Text
                    style={[
                      styles.timeChoiceText,
                      selectedExtraTime === mins && styles.timeChoiceTextActive,
                    ]}
                  >
                    +{mins} min
                  </Text>
                </TouchableOpacity>
              ))}
            </View>

            <TextInput
              style={styles.modalInput}
              placeholder="Ej: Necesito 30 minutos más para terminar mi maqueta..."
              placeholderTextColor="#64748B"
              value={requestMessage}
              onChangeText={setRequestMessage}
              multiline
              numberOfLines={3}
            />

            <View style={styles.modalActions}>
              <TouchableOpacity
                style={styles.modalCancelBtn}
                onPress={() => setModalVisible(false)}
              >
                <Text style={styles.modalCancelText}>Cancelar</Text>
              </TouchableOpacity>
              <TouchableOpacity
                style={styles.modalSendBtn}
                onPress={handleSendRequest}
              >
                <Text style={styles.modalSendText}>Enviar a Padres</Text>
              </TouchableOpacity>
            </View>
          </View>
        </View>
      </Modal>
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#0B0F19',
  },
  statusBarCard: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    backgroundColor: '#161F30',
    padding: 14,
    borderRadius: 16,
    borderWidth: 1,
    borderColor: '#283548',
    marginBottom: 16,
  },
  statusInfo: {
    flexDirection: 'row',
    alignItems: 'center',
  },
  shieldIcon: {
    fontSize: 24,
    marginRight: 10,
  },
  statusTitle: {
    fontSize: 14,
    fontWeight: '700',
    color: '#FFFFFF',
  },
  statusSubtitle: {
    fontSize: 11,
    color: '#10B981',
    fontWeight: '600',
  },
  sosBtn: {
    backgroundColor: '#DC2626',
    paddingHorizontal: 12,
    paddingVertical: 6,
    borderRadius: 8,
  },
  sosText: {
    color: '#FFFFFF',
    fontWeight: '900',
    fontSize: 12,
    letterSpacing: 1,
  },
  countdownCard: {
    backgroundColor: '#161F30',
    borderRadius: 20,
    borderWidth: 1,
    borderColor: '#283548',
    padding: 20,
    alignItems: 'center',
    marginBottom: 20,
  },
  countdownLabel: {
    fontSize: 11,
    color: '#94A3B8',
    fontWeight: '700',
    letterSpacing: 1,
  },
  countdownNumber: {
    fontSize: 44,
    fontWeight: '900',
    color: '#FFB800',
    marginVertical: 4,
  },
  countdownHelper: {
    fontSize: 12,
    color: '#64748B',
    marginBottom: 16,
  },
  askTimeBtn: {
    backgroundColor: '#1E293B',
    borderWidth: 1,
    borderColor: '#FFB800',
    paddingVertical: 10,
    paddingHorizontal: 20,
    borderRadius: 12,
    width: '100%',
    alignItems: 'center',
  },
  askTimeBtnText: {
    color: '#FFB800',
    fontWeight: '700',
    fontSize: 13,
  },
  sectionHeader: {
    marginBottom: 12,
  },
  sectionTitle: {
    fontSize: 11,
    fontWeight: '700',
    color: '#64748B',
    letterSpacing: 1,
  },
  appsGrid: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    justifyContent: 'space-between',
    marginBottom: 20,
  },
  appItem: {
    width: '30%',
    backgroundColor: '#161F30',
    borderRadius: 16,
    borderWidth: 1,
    borderColor: '#283548',
    padding: 12,
    alignItems: 'center',
    marginBottom: 12,
  },
  appIcon: {
    width: 44,
    height: 44,
    borderRadius: 12,
    justifyContent: 'center',
    alignItems: 'center',
    marginBottom: 8,
  },
  appName: {
    fontSize: 12,
    fontWeight: '700',
    color: '#FFFFFF',
    marginBottom: 2,
  },
  appStatus: {
    fontSize: 10,
    color: '#94A3B8',
  },
  familyCallCard: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: '#161F30',
    borderRadius: 16,
    borderWidth: 1,
    borderColor: '#283548',
    padding: 14,
    marginBottom: 20,
  },
  familyCallTitle: {
    fontSize: 14,
    fontWeight: '700',
    color: '#FFFFFF',
  },
  familyCallSubtitle: {
    fontSize: 11,
    color: '#94A3B8',
  },
  devToggleBtn: {
    padding: 12,
    alignItems: 'center',
  },
  devToggleText: {
    fontSize: 11,
    color: '#64748B',
    textDecorationLine: 'underline',
  },
  lockedContainer: {
    flex: 1,
    backgroundColor: '#0F172A',
    justifyContent: 'center',
    alignItems: 'center',
    padding: 24,
  },
  lockIconCircle: {
    width: 80,
    height: 80,
    borderRadius: 40,
    backgroundColor: 'rgba(239, 68, 68, 0.2)',
    borderWidth: 2,
    borderColor: '#EF4444',
    justifyContent: 'center',
    alignItems: 'center',
    marginBottom: 20,
  },
  lockedTitle: {
    fontSize: 24,
    fontWeight: '900',
    color: '#FFFFFF',
    marginBottom: 8,
  },
  lockedSubtitle: {
    fontSize: 13,
    color: '#94A3B8',
    textAlign: 'center',
    lineHeight: 20,
    marginBottom: 24,
  },
  requestTimeBtn: {
    backgroundColor: '#FFB800',
    width: '100%',
    paddingVertical: 14,
    borderRadius: 14,
    alignItems: 'center',
    marginBottom: 12,
  },
  requestTimeBtnText: {
    color: '#000000',
    fontWeight: '800',
    fontSize: 14,
  },
  emergencyBtn: {
    backgroundColor: '#1E293B',
    borderWidth: 1,
    borderColor: '#374151',
    width: '100%',
    paddingVertical: 12,
    borderRadius: 14,
    alignItems: 'center',
  },
  emergencyBtnText: {
    color: '#E2E8F0',
    fontWeight: '600',
    fontSize: 13,
  },
  modalOverlay: {
    flex: 1,
    backgroundColor: 'rgba(0, 0, 0, 0.8)',
    justifyContent: 'center',
    padding: 20,
  },
  modalContent: {
    backgroundColor: '#161F30',
    borderRadius: 20,
    borderWidth: 1,
    borderColor: '#283548',
    padding: 20,
  },
  modalTitle: {
    fontSize: 18,
    fontWeight: '800',
    color: '#FFFFFF',
    marginBottom: 6,
  },
  modalSubtitle: {
    fontSize: 12,
    color: '#94A3B8',
    marginBottom: 16,
  },
  timeButtonsRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    marginBottom: 16,
  },
  timeChoiceBtn: {
    flex: 1,
    backgroundColor: '#1E293B',
    paddingVertical: 10,
    borderRadius: 10,
    alignItems: 'center',
    marginHorizontal: 4,
    borderWidth: 1,
    borderColor: '#374151',
  },
  timeChoiceBtnActive: {
    backgroundColor: '#FFB800',
    borderColor: '#FFB800',
  },
  timeChoiceText: {
    fontSize: 12,
    fontWeight: '700',
    color: '#94A3B8',
  },
  timeChoiceTextActive: {
    color: '#000000',
  },
  modalInput: {
    backgroundColor: '#0F172A',
    borderWidth: 1,
    borderColor: '#283548',
    borderRadius: 12,
    padding: 12,
    color: '#FFFFFF',
    fontSize: 13,
    marginBottom: 16,
    textAlignVertical: 'top',
  },
  modalActions: {
    flexDirection: 'row',
    justifyContent: 'flex-end',
    alignItems: 'center',
  },
  modalCancelBtn: {
    paddingHorizontal: 16,
    paddingVertical: 10,
    marginRight: 8,
  },
  modalCancelText: {
    color: '#94A3B8',
    fontSize: 13,
    fontWeight: '600',
  },
  modalSendBtn: {
    backgroundColor: '#FFB800',
    paddingHorizontal: 18,
    paddingVertical: 10,
    borderRadius: 10,
  },
  modalSendText: {
    color: '#000000',
    fontSize: 13,
    fontWeight: '800',
  },
});
