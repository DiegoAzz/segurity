import { Injectable, NotFoundException, ForbiddenException } from '@nestjs/common';
import { PrismaService } from '../prisma.service';

@Injectable()
export class PoliciesService {
  constructor(private readonly prisma: PrismaService) {}

  async updatePolicy(
    familyId: string,
    childId: string,
    actorId: string,
    dto: {
      instantLockActive?: boolean;
      dailyLimitMinutes?: number;
      schoolScheduleActive?: boolean;
      schoolStart?: string;
      schoolEnd?: string;
      bedtimeScheduleActive?: boolean;
      bedtimeStart?: string;
      bedtimeEnd?: string;
      rawRulesJson?: any;
    },
  ) {
    const child = await this.prisma.child.findUnique({
      where: { id: childId },
      include: {
        policies: {
          orderBy: { version: 'desc' },
          take: 1,
        },
      },
    });

    if (!child) {
      throw new NotFoundException('Perfil de hijo no encontrado.');
    }

    if (child.familyId !== familyId) {
      throw new ForbiddenException('No tienes permiso para modificar las políticas de este menor.');
    }

    const latest = child.policies[0];
    const newVersion = (latest?.version || 0) + 1;

    const newPolicy = await this.prisma.policy.create({
      data: {
        childId: child.id,
        version: newVersion,
        instantLockActive: dto.instantLockActive ?? latest?.instantLockActive ?? false,
        dailyLimitMinutes: dto.dailyLimitMinutes ?? latest?.dailyLimitMinutes ?? 120,
        schoolScheduleActive: dto.schoolScheduleActive ?? latest?.schoolScheduleActive ?? false,
        schoolStart: dto.schoolStart ?? latest?.schoolStart,
        schoolEnd: dto.schoolEnd ?? latest?.schoolEnd,
        bedtimeScheduleActive: dto.bedtimeScheduleActive ?? latest?.bedtimeScheduleActive ?? false,
        bedtimeStart: dto.bedtimeStart ?? latest?.bedtimeStart,
        bedtimeEnd: dto.bedtimeEnd ?? latest?.bedtimeEnd,
        rawRulesJson: dto.rawRulesJson ?? latest?.rawRulesJson,
      },
    });

    // Register audit log
    await this.prisma.auditLog.create({
      data: {
        familyId,
        actorId,
        action: 'POLICY_UPDATED',
        details: {
          childId,
          newVersion,
          instantLock: newPolicy.instantLockActive,
        },
      },
    });

    return newPolicy;
  }

  async acknowledgePolicy(dto: {
    deviceId: string;
    policyVersion: number;
    appliedState: string;
    details?: string;
  }) {
    const device = await this.prisma.device.findUnique({
      where: { id: dto.deviceId },
      include: {
        child: {
          include: {
            policies: {
              where: { version: dto.policyVersion },
            },
          },
        },
      },
    });

    if (!device) {
      throw new NotFoundException('Dispositivo no encontrado.');
    }

    const policy = device.child.policies[0];
    if (!policy) {
      throw new NotFoundException('Política no encontrada.');
    }

    return this.prisma.policyAck.create({
      data: {
        deviceId: device.id,
        policyId: policy.id,
        policyVersion: dto.policyVersion,
        appliedState: dto.appliedState,
        details: dto.details,
      },
    });
  }
}
