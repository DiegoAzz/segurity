import { Controller, Post, Body, HttpCode, HttpStatus } from '@nestjs/common';
import { DevicesService } from './devices.service';

@Controller('devices')
export class DevicesController {
  constructor(private readonly devicesService: DevicesService) {}

  @Post('pair')
  @HttpCode(HttpStatus.OK)
  async pairDevice(
    @Body() body: {
      pairingCode: string;
      deviceFingerprint: string;
      modelName: string;
      manufacturer: string;
      osVersion: string;
      appVersion: string;
    },
  ) {
    return this.devicesService.pairDevice(body);
  }

  @Post('telemetry')
  @HttpCode(HttpStatus.OK)
  async reportTelemetry(
    @Body() body: {
      deviceFingerprint: string;
      batteryLevel?: number;
      fcmToken?: string;
    },
  ) {
    return this.devicesService.reportTelemetry(body);
  }
}
