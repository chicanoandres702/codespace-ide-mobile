import { Injectable, UnauthorizedException, ConflictException } from '@nestjs/common';
import { JwtService } from '@nestjs/jwt';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository } from 'typeorm';
import * as argon2 from 'argon2';
import { createHash, randomBytes } from 'crypto';
import { UsersService } from '../users/users.service';
import { RefreshToken } from './refresh-token.entity';
import { LoginDto, RegisterDto } from './dto';

@Injectable()
export class AuthService {
  constructor(
    private readonly users: UsersService,
    private readonly jwt: JwtService,
    @InjectRepository(RefreshToken)
    private readonly refreshRepo: Repository<RefreshToken>,
  ) {}

  async register(dto: RegisterDto) {
    const existing = await this.users.findByEmail(dto.email);
    if (existing) throw new ConflictException('Email already registered');
    const passwordHash = await argon2.hash(dto.password, { type: argon2.argon2id });
    const user = await this.users.create({
      email: dto.email,
      displayName: dto.displayName,
      passwordHash,
    });
    return this.issueTokens(user.id, user.email, dto['deviceId']);
  }

  async login(dto: LoginDto) {
    const user = await this.users.findByEmail(dto.email);
    if (!user?.passwordHash) throw new UnauthorizedException('Invalid credentials');
    const ok = await argon2.verify(user.passwordHash, dto.password);
    if (!ok) throw new UnauthorizedException('Invalid credentials');
    return this.issueTokens(user.id, user.email, dto.deviceId);
  }

  async refresh(rawToken: string) {
    const tokenHash = this.hash(rawToken);
    const stored = await this.refreshRepo.findOne({ where: { tokenHash } });
    if (!stored || stored.revokedAt || stored.expiresAt < new Date()) {
      throw new UnauthorizedException('Invalid refresh token');
    }
    // Rotate: revoke old, issue new (reuse detection on the family).
    stored.revokedAt = new Date();
    await this.refreshRepo.save(stored);
    const user = await this.users.findById(stored.userId);
    if (!user) throw new UnauthorizedException();
    return this.issueTokens(user.id, user.email, stored.deviceId);
  }

  async logout(rawToken: string) {
    const tokenHash = this.hash(rawToken);
    await this.refreshRepo.update({ tokenHash }, { revokedAt: new Date() });
    return { success: true };
  }

  private async issueTokens(userId: string, email: string, deviceId?: string) {
    const accessTtl = Number(process.env.JWT_ACCESS_TTL ?? 900);
    const refreshTtl = Number(process.env.JWT_REFRESH_TTL ?? 2_592_000);

    const accessToken = await this.jwt.signAsync(
      { sub: userId, email },
      { secret: process.env.JWT_SECRET, expiresIn: accessTtl },
    );

    const rawRefresh = randomBytes(48).toString('hex');
    await this.refreshRepo.save(
      this.refreshRepo.create({
        userId,
        deviceId,
        tokenHash: this.hash(rawRefresh),
        expiresAt: new Date(Date.now() + refreshTtl * 1000),
      }),
    );

    return {
      accessToken,
      accessTokenExpiresIn: accessTtl,
      refreshToken: rawRefresh,
    };
  }

  private hash(token: string) {
    return createHash('sha256').update(token).digest('hex');
  }
}
