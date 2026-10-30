import { Injectable, NotFoundException, BadRequestException } from '@nestjs/common';
import { PrismaService } from '../prisma.service';

export enum PlanTier {
  STARTER = 'STARTER',
  PRO_FAMILY = 'PRO_FAMILY',
  ENTERPRISE = 'ENTERPRISE',
}

@Injectable()
export class SubscriptionsService {
  constructor(private readonly prisma: PrismaService) {}

  async getFamilySubscription(familyId: string) {
    let sub = await this.prisma.subscription.findUnique({
      where: { familyId },
    });

    if (!sub) {
      // Create a default 14-day trial Pro subscription
      const trialEnd = new Date();
      trialEnd.setDate(trialEnd.getDate() + 14);

      sub = await this.prisma.subscription.create({
        data: {
          familyId,
          tier: 'PRO_FAMILY',
          status: 'TRIAL',
          maxChildren: 3,
          maxDevices: 5,
          trialEndsAt: trialEnd,
          currentPeriodEnd: trialEnd,
        },
      });
    }

    return sub;
  }

  async checkQuota(familyId: string, itemType: 'child' | 'device'): Promise<boolean> {
    const sub = await this.getFamilySubscription(familyId);

    if (itemType === 'child') {
      const currentChildren = await this.prisma.child.count({ where: { familyId } });
      if (currentChildren >= sub.maxChildren) {
        throw new BadRequestException(
          `Has alcanzado el límite de ${sub.maxChildren} hijos permitidos en tu plan ${sub.tier}. Actualiza a un plan superior para agregar más.`,
        );
      }
    } else {
      const currentDevices = await this.prisma.device.count({
        where: { child: { familyId } },
      });
      if (currentDevices >= sub.maxDevices) {
        throw new BadRequestException(
          `Has alcanzado el límite de ${sub.maxDevices} dispositivos permitidos en tu plan ${sub.tier}.`,
        );
      }
    }

    return true;
  }

  async upgradePlan(
    familyId: string,
    actorId: string,
    tier: PlanTier,
    durationMonths: number = 1,
  ) {
    const sub = await this.getFamilySubscription(familyId);

    let maxChildren = 1;
    let maxDevices = 2;

    if (tier === PlanTier.PRO_FAMILY) {
      maxChildren = 5;
      maxDevices = 10;
    } else if (tier === PlanTier.ENTERPRISE) {
      maxChildren = 50;
      maxDevices = 100;
    }

    const expiry = new Date();
    expiry.setMonth(expiry.getMonth() + durationMonths);

    const updated = await this.prisma.subscription.update({
      where: { id: sub.id },
      data: {
        tier,
        status: 'ACTIVE',
        maxChildren,
        maxDevices,
        currentPeriodEnd: expiry,
      },
    });

    await this.prisma.auditLog.create({
      data: {
        familyId,
        actorId,
        action: 'SUBSCRIPTION_UPGRADED',
        details: { tier, maxChildren, maxDevices, expiresAt: expiry },
      },
    });

    return updated;
  }
}
