/** 角色基础信息类型（存储在数据库） */
export interface Character {
  /** 角色唯一ID（前端预生成，后端校验唯一性） */
  id: string;
  /** 角色名称 */
  name: string;
  /** 头像文件路径（后端存储路径：/character/{id}.png/jpg） */
  avatarPath: string;
  /** 提示词文件路径（后端存储路径：/character/prompt/{id}.txt） */
  promptFilePath: string;
  /** 是否为预设角色（不可删除） */
  isPreset: boolean;
}

/** 新增角色请求参数（前端提交给后端） */
export interface CreateCharacterRequest {
  name: string;
  /** 头像文件（FormData上传） */
  avatarFile: File | null;
  /** 提示词内容（单独上传为文本文件） */
  promptContent: string;
}

/** 角色API响应通用格式 */
export interface CharacterApiResponse<T = any> {
  code: number;
  message: string;
  data: T;
}