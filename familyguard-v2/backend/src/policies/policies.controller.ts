import { Controller, Post, Body, Param, UseGuards, Request } from '@nestjs/common';
import { PoliciesService } from './policies.service';
import { JwtAuthGuard } from '../auth/jwt-auth.guard';

@Controller('policies')
export class PoliciesController {
  constructor(private readonly policiesService: PoliciesService) {}

  @Post(':childId')
  @UseGuards(JwtAuthGuard)
  async updatePolicy(
    @Request() req: any,
    @Param('childId') childId: string,
    @Body() body: any,
  ) {
    return this.policiesService.updatePolicy(
      req.user.familyId,
      childId,
      req.user.userId,
      body,
    );
  }

  @Post('ack')
  async acknowledgePolicy(
    @Body() body: {
      deviceId: string;
      policyVersion: number;
      appliedState: string;
      details?: string;
    },
  ) {
    return this.policiesService.acknowledgePolicy(body);
  }
}
