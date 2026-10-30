import { Injectable, NotFoundException } from '@nestjs/common';
import { PrismaService } from '../prisma.service';

@Injectable()
export class DevicesService {
  constructor(private readonly prisma: PrismaService) {}

  async pairDevice(dto: {
    pairingCode: string;
    deviceFingerprint: string;
    modelName: string;
    manufacturer: string;
    osVersion: string;
    appVersion: string;
  }) {
    const child = await this.prisma.child.findUnique({
      where: { pairingCode: dto.pairingCode },
      include: {
        family: true,
        policies: {
          orderBy: { version: 'desc' },
          take: 1,
        },
      },
    });

    if (!child) {
      throw new NotFoundException('Código de vinculación inválido o expirado.');
    }

    const device = await this.prisma.device.upsert({
      where: { deviceFingerprint: dto.deviceFingerprint },
      update: {
        childId: child.id,
        modelName: dto.modelName,
        manufacturer: dto.manufacturer,
        osVersion: dto.osVersion,
        appVersion: dto.appVersion,
        isOnline: true,
        lastSeenAt: new Date(),
      },
      create: {
        childId: child.id,
        deviceFingerprint: dto.deviceFingerprint,
        modelName: dto.modelName,
        manufacturer: dto.manufacturer,
        osVersion: dto.osVersion,
        appVersion: dto.appVersion,
        isOnline: true,
        lastSeenAt: new Date(),
      },
    });

    const activePolicy = child.policies[0] || null;

    return {
      success: true,
      deviceId: device.id,
      child: {
        id: child.id,
        name: child.name,
      },
      family: {
        id: child.family.id,
        name: child.family.name,
        familyPin: child.family.familyPin,
      },
      policy: activePolicy,
    };
  }

  async reportTelemetry(dto: {
    deviceFingerprint: string;
    batteryLevel?: number;
    fcmToken?: string;
  }) {
    const device = await this.prisma.device.findUnique({
      where: { deviceFingerprint: dto.deviceFingerprint },
      include: {
        child: {
          include: {
            policies: {
              orderBy: { version: 'desc' },
              take: 1,
            },
          },
        },
      },
    });

    if (!device) {
      throw new NotFoundException('Dispositivo no registrado.');
    }

    await this.prisma.device.update({
      where: { id: device.id },
      data: {
        batteryLevel: dto.batteryLevel ?? device.batteryLevel,
        fcmToken: dto.fcmToken ?? device.fcmToken,
        isOnline: true,
        lastSeenAt: new Date(),
      },
    });

    return {
      success: true,
      activePolicy: device.child.policies[0] || null,
    };
  }
}
