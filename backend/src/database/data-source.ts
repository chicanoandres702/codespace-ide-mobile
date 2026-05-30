import { DataSource } from 'typeorm';
import { config } from 'dotenv';
config();

/** Used by the TypeORM CLI for migrations (npm run migration:run). */
export default new DataSource({
  type: 'postgres',
  url: process.env.DATABASE_URL,
  entities: ['src/**/*.entity.ts'],
  migrations: ['src/database/migrations/*.ts'],
});
