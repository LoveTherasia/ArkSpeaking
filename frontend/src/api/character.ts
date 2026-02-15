import request from '@/utils/request';
import { Character, CreateCharacterRequest, PageRequest, PageResponse } from '@/types/character';

// ===================== 本地存储选中角色ID =====================
// 修改点1：统一ID类型为number，处理空值
export const saveSelectedCharacterId = (id: number | ''): void => {
  if (id) {
    localStorage.setItem('selectedCharacterId', id.toString());
  } else {
    localStorage.removeItem('selectedCharacterId');
  }
};

export const getSelectedCharacterId = (): number => {
  const id = localStorage.getItem('selectedCharacterId');
  // 修改点2：空值返回0而非空字符串，避免类型混乱
  return id ? Number(id) : 0;
};

// ===================== 角色接口（对接后端） =====================
/**
 * 获取角色列表（分页）
 * @param pageRequest 分页参数（默认第一页，10条/页）
 */
export const getCharacterList = async (
  pageRequest: PageRequest = { pageNum: 1, pageSize: 10 } // 修改点3：添加默认分页参数
): Promise<PageResponse<Character>> => {
  try {
    return await request.get('/api/character/list', {
      params: pageRequest
    });
  } catch (error) {
    console.error('获取角色列表失败:', error);
    // 返回空列表，避免组件崩溃
    return { list: [], total: 0, pageNum: 1, pageSize: 10 };
  }
};

/**
 * 根据ID查询角色详情
 * @param id 角色ID
 */
export const getCharacterById = async (id: number): Promise<Character | null> => { // 修改点4：返回null兼容空值
  if (!id || id <= 0) return null; // 过滤无效ID
  try {
    return await request.get(`/api/character/${id}`);
  } catch (error) {
    console.error(`查询角色${id}失败:`, error);
    return null;
  }
};

/**
 * 根据ID查询角色提示词
 * @param id 角色ID
 */
export const getCharacterPromptById = async (id: number): Promise<string> => {
  if (!id || id <= 0) return '';
  try {
    return await request.get(`/api/character/${id}/prompt`);
  } catch (error) {
    console.error(`查询角色${id}提示词失败:`, error);
    return '';
  }
};

/**
 * 新增角色（含头像文件上传）
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

    return await request.post('/api/character', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    });
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
    const res = await request.delete(`/api/character/${id}`);
    // 兼容后端不同的成功响应格式
    return res?.code === 200 || res?.success === true || res === true;
  } catch (error) {
    console.error(`删除角色${id}失败:`, error);
    return false;
  }
};