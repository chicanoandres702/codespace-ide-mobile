import { Column, CreateDateColumn, Entity, PrimaryGeneratedColumn } from 'typeorm';

@Entity('projects')
export class Project {
  @PrimaryGeneratedColumn('uuid')
  id: string;

  @Column()
  userId: string;

  @Column()
  name: string;

  @Column()
  kind: string; // git | ssh | codespace | docker | local

  @Column({ nullable: true })
  gitRemoteUrl?: string;

  @Column({ nullable: true })
  defaultBranch?: string;

  @Column({ type: 'jsonb', default: {} })
  remoteConfig: Record<string, unknown>;

  @CreateDateColumn()
  createdAt: Date;
}
