import request from '@/utils/request';
import { Character, CreateCharacterRequest } from '@/types/character';

// ===================== 本地存储选中角色ID =====================
export const saveSelectedCharacterId = (id: number): void => {
  localStorage.setItem('selectedCharacterId', id.toString());
};

export const getSelectedCharacterId = (): number => {
  const id = localStorage.getItem('selectedCharacterId');
  return id ? Number(id) : 0;
};

// ===================== 角色接口（对接后端） =====================
/**
 * 获取所有角色列表（后端无分页，直接返回角色数组）
 */
export const getCharacterList = async (): Promise<Character[]> => {
  try {
    // 后端实际返回的是角色数组，无需Result包装
    const res: Character[] = await request.get('/api/character/list');
    console.log('获取角色列表响应:', res);
    // 直接返回数组（后端返回的就是角色列表）
    return res || [];
  } catch (error) {
    console.error('获取角色列表失败:', error);
    return []; // 兜底：返回空数组避免组件崩溃
  }
};

/**
 * 根据ID查询角色详情
 * @param id 角色ID
 */
export const getCharacterById = async (id: number): Promise<Character | null> => {
  if (!id || id <= 0) return null; // 过滤无效ID
  try {
    // 注意：如果查询单个角色的后端也直接返回角色对象（无Result包装），则去掉Result断言
    const res: Character = await request.get(`/api/character/${id}`);
    return res || null;
  } catch (error) {
    console.error(`查询角色${id}失败:`, error);
    return null;
  }
};

/**
 * 根据ID查询角色提示词（后端读取文件内容返回）
 * @param id 角色ID
 */
export const getCharacterPromptById = async (id: number): Promise<string> => {
  if (!id || id <= 0) return '';
  try {
    // 同理：如果提示词接口直接返回字符串（无Result包装），则直接接收
    const res: string = await request.get(`/api/character/${id}/prompt`);
    return res || '';
  } catch (error) {
    console.error(`查询角色${id}提示词失败:`, error);
    return '';
  }
};

/**
 * 新增角色（含头像文件上传）
 * 修正：请求路径改为后端的 /api/character/create
 */
export const createCharacter = async (data: CreateCharacterRequest): Promise<Character | null> => {
  if (!data.name || !data.promptContent || !data.avatarFile) {
    console.error('新增角色参数不全');
    return null;
  }
  try {
    const formData = new FormData();
    formData.append('name', data.name);
    formData.append('promptContent', data.promptContent);
    formData.append('avatarFile', data.avatarFile);

    // 新增角色的后端如果直接返回角色对象（无Result包装），则去掉Result断言
    const res: Character = await request.post('/api/character/create', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    });
    return res || null;
  } catch (error) {
    console.error('新增角色失败:', error);
    return null;
  }
};

/**
 * 删除角色
 */
export const deleteCharacter = async (id: number): Promise<boolean> => {
  if (!id || id <= 0) return false;
  try {
    // 删除角色的后端如果返回boolean或成功提示，适配实际返回格式
    const res = await request.delete(`/api/character/${id}`);
    // 简单判断：只要请求成功就返回true（根据后端实际返回调整）
    return res ? true : false;
  } catch (error) {
    console.error(`删除角色${id}失败:`, error);
    return false;
  }
};