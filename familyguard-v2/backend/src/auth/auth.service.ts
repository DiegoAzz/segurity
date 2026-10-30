import { Injectable, UnauthorizedException, BadRequestException } from '@nestjs/common';
import { JwtService } from '@nestjs/jwt';
import { PrismaService } from '../prisma.service';
import * as bcrypt from 'bcrypt';

@Injectable()
export class AuthService {
  constructor(
    private readonly prisma: PrismaService,
    private readonly jwtService: JwtService,
  ) {}

  async register(dto: { email: string; password: string; name: string; familyName?: string }) {
    const existing = await this.prisma.user.findUnique({
      where: { email: dto.email.toLowerCase() },
    });
    if (existing) {
      throw new BadRequestException('El correo ya está registrado en la plataforma.');
    }

    const passwordHash = await bcrypt.hash(dto.password, 10);

    // Create a new family tenant automatically for this parent
    const family = await this.prisma.family.create({
      data: {
        name: dto.familyName || `Familia ${dto.name}`,
        familyPin: '1234', // Default PIN
      },
    });

    const user = await this.prisma.user.create({
      data: {
        email: dto.email.toLowerCase(),
        passwordHash,
        name: dto.name,
        role: 'PARENT',
        familyId: family.id,
      },
    });

    // Create a default child profile
    const pairingCode = Math.floor(100000 + Math.random() * 900000).toString();
    const child = await this.prisma.child.create({
      data: {
        familyId: family.id,
        name: 'Mi Hijo',
        age: 10,
        pairingCode,
      },
    });

    // Create default policy v1
    await this.prisma.policy.create({
      data: {
        childId: child.id,
        version: 1,
        instantLockActive: false,
        dailyLimitMinutes: 120,
      },
    });

    return this.generateToken(user);
  }

  async login(dto: { email: string; password: string }) {
    const user = await this.prisma.user.findUnique({
      where: { email: dto.email.toLowerCase() },
      include: { family: true },
    });

    if (!user) {
      throw new UnauthorizedException('Credenciales inválidas.');
    }

    const isMatch = await bcrypt.compare(dto.password, user.passwordHash);
    if (!isMatch) {
      throw new UnauthorizedException('Credenciales inválidas.');
    }

    return this.generateToken(user);
  }

  private generateToken(user: any) {
    const payload = {
      sub: user.id,
      email: user.email,
      role: user.role,
      familyId: user.familyId,
    };

    return {
      accessToken: this.jwtService.sign(payload),
      user: {
        id: user.id,
        email: user.email,
        name: user.name,
        role: user.role,
        familyId: user.familyId,
      },
    };
  }
}
