// src/types/character.ts
/** 角色实体类型（严格对接后端Character实体） */
export interface Character {
  id: number; // 后端主键通常是number，修正原string类型
  name: string;
  avatarPath: string; // 后端返回的头像存储路径
  promptContent: string;
  isPreset: boolean; // 是否预设角色
  createTime?: string; // 后端返回的创建时间
  status?: 'ENABLE' | 'DISABLE'; // 角色状态（后端新增）
}

/** 新增角色的请求参数（包含文件，需用FormData传递） */
export interface CreateCharacterRequest {
  name: string;
  avatarFile: File | null; // 头像文件
  promptContent: string;
}

/** 分页请求参数（对接后端Pageable） */
export interface PageRequest {
  page: number; // 后端从0开始
  size: number;
}

/** 后端分页返回结构 */
export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}

/** 前端分页参数（Element Plus分页组件用，从1开始） */
export interface FrontPageParams {
  pageNum: number;
  pageSize: number;
  total: number;
}