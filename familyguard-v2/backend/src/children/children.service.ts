import { Injectable, NotFoundException, ForbiddenException } from '@nestjs/common';
import { PrismaService } from '../prisma.service';

@Injectable()
export class ChildrenService {
  constructor(private readonly prisma: PrismaService) {}

  async findAllByFamily(familyId: string) {
    return this.prisma.child.findMany({
      where: { familyId },
      include: {
        devices: true,
        policies: {
          orderBy: { version: 'desc' },
          take: 1,
        },
      },
      orderBy: { createdAt: 'asc' },
    });
  }

  async create(familyId: string, dto: { name: string; age?: number; avatar?: string }) {
    const pairingCode = Math.floor(100000 + Math.random() * 900000).toString();

    const child = await this.prisma.child.create({
      data: {
        familyId,
        name: dto.name,
        age: dto.age || 10,
        avatar: dto.avatar || 'avatar_1',
        pairingCode,
      },
    });

    await this.prisma.policy.create({
      data: {
        childId: child.id,
        version: 1,
        instantLockActive: false,
        dailyLimitMinutes: 120,
      },
    });

    return child;
  }

  async update(familyId: string, childId: string, dto: { name?: string; age?: number; avatar?: string }) {
    const child = await this.prisma.child.findUnique({
      where: { id: childId },
    });

    if (!child) {
      throw new NotFoundException('Perfil de hijo no encontrado.');
    }

    if (child.familyId !== familyId) {
      throw new ForbiddenException('No tienes autorización para modificar este perfil.');
    }

    return this.prisma.child.update({
      where: { id: childId },
      data: {
        name: dto.name ?? child.name,
        age: dto.age ?? child.age,
        avatar: dto.avatar ?? child.avatar,
      },
    });
  }
}
