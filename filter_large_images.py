import os
import shutil
from PIL import Image

def filter_large_images(input_dir, output_dir, min_width=3000, min_height=3000):
    """
    筛选出宽和高都大于指定尺寸的图片并复制到输出目录
    
    参数:
        input_dir: 输入图片目录路径
        output_dir: 输出目录路径
        min_width: 最小宽度要求 (默认3000)
        min_height: 最小高度要求 (默认3000)
    """
    # 创建输出目录
    os.makedirs(output_dir, exist_ok=True)
    
    # 支持的图片格式
    supported_ext = ('.jpg', '.jpeg', '.png', '.bmp', '.tiff', '.webp', '.gif')
    
    # 统计信息
    total_files = 0
    filtered_files = 0
    skipped_files = 0
    
    print(f"开始筛选图片...")
    print(f"最小尺寸要求: {min_width}x{min_height}")
    print(f"输入目录: {input_dir}")
    print(f"输出目录: {output_dir}")
    print("-" * 50)
    
    for filename in os.listdir(input_dir):
        if filename.lower().endswith(supported_ext):
            total_files += 1
            input_path = os.path.join(input_dir, filename)
            
            try:
                with Image.open(input_path) as img:
                    width, height = img.size
                    
                    # 检查图片尺寸是否满足要求
                    if width >= min_width and height >= min_height:
                        # 复制文件到输出目录
                        output_path = os.path.join(output_dir, filename)
                        shutil.copy2(input_path, output_path)
                        filtered_files += 1
                        print(f"✓ 复制: {filename} ({width}x{height})")
                    else:
                        skipped_files += 1
                        print(f"✗ 跳过: {filename} ({width}x{height}) - 尺寸不足")
                        
            except Exception as e:
                skipped_files += 1
                print(f"✗ 错误: {filename} - {str(e)}")
    
    # 输出统计结果
    print("-" * 50)
    print(f"处理完成!")
    print(f"总文件数: {total_files}")
    print(f"符合条件的图片: {filtered_files}")
    print(f"跳过的图片: {skipped_files}")
    print(f"筛选率: {filtered_files/total_files*100:.1f}%" if total_files > 0 else "筛选率: 0%")

def filter_large_images_with_rename(input_dir, output_dir, min_width=3000, min_height=3000, prefix=""):
    """
    筛选出宽和高都大于指定尺寸的图片并重命名保存到输出目录
    
    参数:
        input_dir: 输入图片目录路径
        output_dir: 输出目录路径
        min_width: 最小宽度要求 (默认3000)
        min_height: 最小高度要求 (默认3000)
        prefix: 输出文件名前缀 (可选)
    """
    # 创建输出目录
    os.makedirs(output_dir, exist_ok=True)
    
    # 支持的图片格式
    supported_ext = ('.jpg', '.jpeg', '.png', '.bmp', '.tiff', '.webp', '.gif')
    
    # 统计信息
    total_files = 0
    filtered_files = 0
    skipped_files = 0
    
    print(f"开始筛选并重命名图片...")
    print(f"最小尺寸要求: {min_width}x{min_height}")
    print(f"文件名前缀: '{prefix}'")
    print(f"输入目录: {input_dir}")
    print(f"输出目录: {output_dir}")
    print("-" * 50)
    
    for filename in os.listdir(input_dir):
        if filename.lower().endswith(supported_ext):
            total_files += 1
            input_path = os.path.join(input_dir, filename)
            
            try:
                with Image.open(input_path) as img:
                    width, height = img.size
                    
                    # 检查图片尺寸是否满足要求
                    if width >= min_width and height >= min_height:
                        # 生成新的文件名
                        name, ext = os.path.splitext(filename)
                        new_filename = f"{prefix}{name}{ext}" if prefix else filename
                        output_path = os.path.join(output_dir, new_filename)
                        
                        # 复制文件到输出目录
                        shutil.copy2(input_path, output_path)
                        filtered_files += 1
                        print(f"✓ 复制: {filename} -> {new_filename} ({width}x{height})")
                    else:
                        skipped_files += 1
                        print(f"✗ 跳过: {filename} ({width}x{height}) - 尺寸不足")
                        
            except Exception as e:
                skipped_files += 1
                print(f"✗ 错误: {filename} - {str(e)}")
    
    # 输出统计结果
    print("-" * 50)
    print(f"处理完成!")
    print(f"总文件数: {total_files}")
    print(f"符合条件的图片: {filtered_files}")
    print(f"跳过的图片: {skipped_files}")
    print(f"筛选率: {filtered_files/total_files*100:.1f}%" if total_files > 0 else "筛选率: 0%")

if __name__ == "__main__":
    # 设置路径
    input_folder = "D:/input_images"  # 替换为你的输入路径
    output_folder = "D:/filtered_images"  # 替换为你的输出路径
    
    # 方法1: 直接复制符合条件的图片
    print("=== 方法1: 直接复制 ===")
    filter_large_images(input_folder, output_folder, min_width=3000, min_height=3000)
    
    print("\n" + "="*60 + "\n")
    
    # 方法2: 复制并重命名（添加前缀）
    print("=== 方法2: 复制并重命名 ===")
    filter_large_images_with_rename(input_folder, output_folder, min_width=3000, min_height=3000, prefix="large_")

































































