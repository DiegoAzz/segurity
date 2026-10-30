import { Controller, Get, Post, Patch, Body, Param, UseGuards, Request } from '@nestjs/common';
import { ChildrenService } from './children.service';
import { JwtAuthGuard } from '../auth/jwt-auth.guard';

@Controller('children')
@UseGuards(JwtAuthGuard)
export class ChildrenController {
  constructor(private readonly childrenService: ChildrenService) {}

  @Get()
  async getMyChildren(@Request() req: any) {
    return this.childrenService.findAllByFamily(req.user.familyId);
  }

  @Post()
  async createChild(
    @Request() req: any,
    @Body() body: { name: string; age?: number; avatar?: string },
  ) {
    return this.childrenService.create(req.user.familyId, body);
  }

  @Patch(':id')
  async updateChild(
    @Request() req: any,
    @Param('id') id: string,
    @Body() body: { name?: string; age?: number; avatar?: string },
  ) {
    return this.childrenService.update(req.user.familyId, id, body);
  }
}
