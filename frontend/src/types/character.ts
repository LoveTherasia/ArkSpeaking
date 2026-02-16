// src/types/character.ts
/** 角色实体类型（严格对接后端Character实体） */
export interface Character {
  id: number; // 后端Long类型，前端用number兼容
  name: string; // 对应后端char_name
  avatarPath: string; // 对应后端avatar_path
  promptFilePath: string; // 修正：后端是prompt_file_path，不是promptContent
  preset: boolean; // 对应后端is_preset
}

/** 新增角色的请求参数（包含文件，需用FormData传递） */
export interface CreateCharacterRequest {
  name: string;
  avatarFile: File | null; // 头像文件
  promptContent: string; // 前端输入的提示词内容（后端会保存为文件）
}

/** 后端统一返回格式（核心！必须和后端Result类对齐） */
export interface Result<T = any> {
  code: number; // 状态码：200成功，400/404失败
  msg: string; // 提示信息
  data: T; // 业务数据
}

// 以下分页类型暂时注释（后端getCharacterList无分页，后续需要再启用）
// /** 分页请求参数（对接后端Pageable） */
// export interface PageRequest {
//   page: number; // 后端从0开始
//   size: number;
// }

// /** 后端分页返回结构 */
// export interface PageResponse<T> {
//   content: T[];
//   totalElements: number;
//   totalPages: number;
//   size: number;
//   number: number;
// }