//读取角色数据的工具函数
export const loadCharacter = async() =>{
try {
        const res = await import('../data/characters.json');
        return res.default.map(character => ({
            ...character,
            avatar: new URL(character.avatar, import.meta.url).href
        }));
    } catch (error) {
        console.error('加载角色数据失败：', error);
        return [
        { id: 0, name: "加载失败", avatar: "../assets/default-avatar.jpg", route: "#" }
        ];
    };
}