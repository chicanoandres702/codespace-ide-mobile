import { Module } from '@nestjs/common';
import { TypeOrmModule } from '@nestjs/typeorm';
import { User } from '../users/user.entity';
import { RefreshToken } from '../auth/refresh-token.entity';
import { Project } from '../repos/project.entity';

@Module({
  imports: [
    TypeOrmModule.forRoot({
      type: 'postgres',
      url: process.env.DATABASE_URL,
      entities: [User, RefreshToken, Project],
      synchronize: process.env.NODE_ENV !== 'production', // use migrations in prod
      autoLoadEntities: true,
    }),
  ],
})
export class DatabaseModule {}
