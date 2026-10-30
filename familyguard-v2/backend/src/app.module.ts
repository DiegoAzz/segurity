import { Module } from '@nestjs/common';
import { AppController } from './app.controller';
import { AppService } from './app.service';
import { PrismaService } from './prisma.service';
import { AuthModule } from './auth/auth.module';
import { ChildrenModule } from './children/children.module';
import { DevicesModule } from './devices/devices.module';
import { PoliciesModule } from './policies/policies.module';
import { SubscriptionsModule } from './subscriptions/subscriptions.module';

@Module({
  imports: [
    AuthModule,
    ChildrenModule,
    DevicesModule,
    PoliciesModule,
    SubscriptionsModule,
  ],
  controllers: [AppController],
  providers: [AppService, PrismaService],
})
export class AppModule {}
