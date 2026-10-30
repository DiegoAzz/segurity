import { Controller, Get, Post, Body, UseGuards, Request } from '@nestjs/common';
import { SubscriptionsService, PlanTier } from './subscriptions.service';
import { JwtAuthGuard } from '../auth/jwt-auth.guard';

@Controller('subscriptions')
@UseGuards(JwtAuthGuard)
export class SubscriptionsController {
  constructor(private readonly subscriptionsService: SubscriptionsService) {}

  @Get('my-plan')
  async getMyPlan(@Request() req: any) {
    return this.subscriptionsService.getFamilySubscription(req.user.familyId);
  }

  @Post('upgrade')
  async upgradePlan(
    @Request() req: any,
    @Body() body: { tier: PlanTier; durationMonths?: number },
  ) {
    return this.subscriptionsService.upgradePlan(
      req.user.familyId,
      req.user.userId,
      body.tier,
      body.durationMonths || 1,
    );
  }
}
