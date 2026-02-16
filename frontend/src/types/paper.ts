// 选择题结构
export interface PaperQuestion {
  question: string;    // 问题内容
  options: string[];   // 选项列表（A/B/C/D）
  answer: number;      // 正确选项索引（0开始）
  explanation: string; // 角色风格的解析
}

// AI返回的解析结果
export interface PaperAnalysisResult {
  characterAnalysis: string; // 角色风格的论文解析
  questions: PaperQuestion[]; // 生成的选择题列表
}

// 答题状态
export interface AnswerState {
  selectedOption: number | null; // 用户选中的选项
  isAnswered: boolean;           // 是否已答题
  isCorrect: boolean | null;     // 是否答对
}