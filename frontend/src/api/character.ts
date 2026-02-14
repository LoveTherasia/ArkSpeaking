// api/character.ts (建议新建此文件)
import type { Character, CreateCharacterRequest, CharacterApiResponse } from '@/types/character';

/** 生成前端唯一角色ID（UUID格式，保证本地唯一性） */
export const generateUniqueCharId = (): string => {
  // UUID v4 简化版，也可使用uuid库
  return 'char_' + Math.random().toString(36).substring(2, 15) + Date.now().toString(36);
};

/** 检查角色ID是否已存在（调用后端接口校验） */
export const checkCharIdExist = async (id: string): Promise<boolean> => {
  try {
    const res = await fetch(`/api/character/check-id/${id}`, {
      method: 'GET',
      headers: {
        'Content-Type': 'application/json',
      },
    });
    const data = await res.json() as CharacterApiResponse<boolean>;
    return data.code === 200 ? data.data : false;
  } catch (error) {
    console.error('校验角色ID失败：', error);
    return false;
  }
};

/** 获取所有角色列表（基础信息） */
export const getCharacterList = async (): Promise<Character[]> => {
  try {
    const res = await fetch('/api/character/list', {
      method: 'GET',
      headers: {
        'Content-Type': 'application/json',
      },
    });
    const data = await res.json() as CharacterApiResponse<Character[]>;
    return data.code === 200 ? data.data : [];
  } catch (error) {
    console.error('获取角色列表失败：', error);
    return [];
  }
};

/** 通过ID获取角色基础信息 */
export const getCharacterById = async (id: string): Promise<Character | null> => {
  try {
    const res = await fetch(`/api/character/${id}`, {
      method: 'GET',
      headers: {
        'Content-Type': 'application/json',
      },
    });
    const data = await res.json() as CharacterApiResponse<Character>;
    return data.code === 200 ? data.data : null;
  } catch (error) {
    console.error(`获取角色${id}信息失败：`, error);
    return null;
  }
};

/** 通过ID读取角色提示词（后端读取/character/prompt/{id}.txt） */
export const getCharacterPromptById = async (id: string): Promise<string> => {
  try {
    const res = await fetch(`/api/character/${id}/prompt`, {
      method: 'GET',
      headers: {
        'Content-Type': 'text/plain',
      },
    });
    if (res.ok) {
      return await res.text();
    }
    console.error(`读取角色${id}提示词失败：`, res.statusText);
    return '';
  } catch (error) {
    console.error(`读取角色${id}提示词失败：`, error);
    return '';
  }
};

/** 新增角色（含头像+提示词文件上传） */
export const createCharacter = async (req: CreateCharacterRequest): Promise<Character | null> => {
  // 1. 生成前端唯一ID，并校验后端是否已存在
  let charId = generateUniqueCharId();
  while (await checkCharIdExist(charId)) {
    charId = generateUniqueCharId(); // 重复则重新生成
  }

  // 2. 构造FormData（上传文件+基础信息）
  const formData = new FormData();
  formData.append('id', charId);
  formData.append('name', req.name);
  if (req.avatarFile) {
    formData.append('avatarFile', req.avatarFile, `${charId}.${req.avatarFile.name.split('.').pop()}`);
  }
  formData.append('promptContent', req.promptContent);

  // 3. 调用后端接口
  try {
    const res = await fetch('/api/character/create', {
      method: 'POST',
      body: formData, // 上传文件必须用FormData
    });
    const data = await res.json() as CharacterApiResponse<Character>;
    if (data.code === 200) {
      return data.data;
    } else {
      ElMessage.error(`新增角色失败：${data.message}`);
      return null;
    }
  } catch (error) {
    console.error('新增角色失败：', error);
    ElMessage.error('新增角色失败，请重试');
    return null;
  }
};

/** 删除角色（仅自定义角色） */
export const deleteCharacter = async (id: string): Promise<boolean> => {
  try {
    const res = await fetch(`/api/character/${id}`, {
      method: 'DELETE',
      headers: {
        'Content-Type': 'application/json',
      },
    });
    const data = await res.json() as CharacterApiResponse;
    if (data.code === 200) {
      ElMessage.success('角色删除成功');
      return true;
    } else {
      ElMessage.error(`删除角色失败：${data.message}`);
      return false;
    }
  } catch (error) {
    console.error(`删除角色${id}失败：`, error);
    ElMessage.error('删除角色失败，请重试');
    return false;
  }
};

/** 保存角色选中状态（可选：也可调用后端接口保存用户偏好） */
export const saveSelectedCharacterId = (id: string): void => {
  // 前端临时存储（也可调用/api/character/selected/{id}接口）
  sessionStorage.setItem('selected_character_id', id);
};

/** 获取已选中的角色ID */
export const getSelectedCharacterId = (): string => {
  return sessionStorage.getItem('selected_character_id') || '';
};