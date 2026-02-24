import os
import PyPDF2
from docx import Document
from dotenv import load_dotenv

class PaperParser:
    def __init__(self,file_path : str):
        self.file_path = file_path
        self.file_type = self.get_file_type()
        self.parsed_content = {}

    def get_file_type(self) -> str:
        """判断文件类型"""
        if self.file_path.endswith('.pdf'):
            return 'pdf'
        elif self.file_path.endswith('.docx'):
            return 'docx'
        else:
            raise ValueError('仅支持PDF和Word格式')
    
    def parse_pdf(self) -> str:
        """解析PDF文件"""
        try:
            with open(self.file_path, 'rb') as f:
                reader = PyPDF2.PdfReader(f)
                # 提取文本
                full_text = ''
                for page in reader.pages:
                    page_text = page.extract_text()
                    if page_text:
                        full_text += page_text + '\n'
            # 简单结构化
            self.parsed_content = {
                'title': full_text.split('\n')[0].strip() if full_text else '未知标题',
                'abstract': self._extract_abstract(full_text),
                'content': full_text,
                'page_count': len(reader.pages)
            }
            return self.parsed_content
        except Exception as e:
            print(f"解析PDF文件失败: {e}")
    
    def parse_docx(self) -> str:
        """解析word文档"""
        try:
            doc = Document(self.file_path)
            full_text = '\n'.join([para.text for para in doc.paragraphs])

            self.parsed_content = {
                'title': doc.core_properties.title or full_text.split('\n')[0].strip(),
                'abstract': self._extract_abstract(full_text),
                'content': full_text,
                'page_count': len(doc.paragraphs) // 20
            }
            return self.parsed_content
        except Exception as e:
            print(f"解析Word文件失败: {e}")

    def _extract_abstract(self,full_text : str) -> str:
        """简单提取摘要"""
        abstract_keywords = ['摘要','Abstract',"ABSTRACT"]
        for keyword in abstract_keywords:
            if keyword in full_text:
                abstract_start = full_text.index(keyword) + len(keyword)
                # 截取下一个关键帧
                end_keywords = ['引言','Introduction','1','一','INTRODUCTION']
                abstract_end = len(full_text)
                for end_key in end_keywords:
                    if end_key in full_text[abstract_start:]:
                        abstract_end = full_text[abstract_start:].index(end_key)
                        break
                    return full_text[abstract_start:abstract_end].strip()
            return '未找到摘要'
    
    def parse(self):
        """统一解析入口"""
        if self.file_type == 'pdf':
            return self.parse_pdf()
        elif self.file_type == 'docx':
            return self.parse_docx()
    
# 测试代码 
if __name__ == "__main__":
    test_paper_path = 'test_paper.pdf'
    parser = PaperParser(test_paper_path)
    try:
        result = parser.parse()
        print("解析结果：")
        print(f"标题: {result['title']}")
        print(f"摘要: {result['abstract'][:200]}……")
        print(f"内容预览: {result['content'][:500]}……")
        print(f"页数: {result['page_count']}")
    except Exception as e:
        print(f"解析失败: {e}")
