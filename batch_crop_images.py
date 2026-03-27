import os
from PIL import Image

def batch_crop_images(input_dir, output_dir, crop_size=1500, min_size=3000):
    """
    批量将大图分割为指定尺寸的多个小图
    
    参数:
        input_dir: 输入图片目录路径
        output_dir: 输出目录路径
        crop_size: 裁剪尺寸 (默认1500x1500)
        min_size: 最小尺寸要求，宽或高小于此值的图片将被跳过 (默认3000)
    """
    # 创建输出目录
    os.makedirs(output_dir, exist_ok=True)
    
    # 支持的图片格式
    supported_ext = ('.jpg', '.jpeg', '.png', '.bmp', '.tiff', '.webp')
    
    for filename in os.listdir(input_dir):
        if filename.lower().endswith(supported_ext):
            input_path = os.path.join(input_dir, filename)
            base_name = os.path.splitext(filename)[0]
            
            try:
                with Image.open(input_path) as img:
                    # 统一转换为RGB模式
                    if img.mode != 'RGB':
                        img = img.convert('RGB')
                    
                    width, height = img.size
                    
                    # 检查图片尺寸是否满足最小要求
                    if width < min_size or height < min_size:
                        print(f"跳过 {filename}: 尺寸 {width}x{height} 小于最小要求 {min_size}x{min_size}")
                        continue
                    
                    # 计算可裁剪的行列数
                    cols = width // crop_size
                    rows = height // crop_size
                    
                    # 遍历每个裁剪区域
                    for y in range(rows):
                        for x in range(cols):
                            # 计算裁剪坐标
                            left = x * crop_size
                            upper = y * crop_size
                            right = left + crop_size
                            lower = upper + crop_size
                            
                            # 执行裁剪
                            crop_box = (left, upper, right, lower)
                            cropped = img.crop(crop_box)
                            
                            # 生成输出文件名
                            output_name = f"{base_name}_x{x}_y{y}.jpg"
                            output_path = os.path.join(output_dir, output_name)
                            
                            # 根据格式保存（优化存储）
                            if filename.lower().endswith(('.png', '.bmp')):
                                cropped.save(output_path, optimize=True)
                            else:
                                cropped.save(output_path, quality=95)
                    
                    print(f"成功分割: {filename} ({width}x{height}) -> {cols*rows}个小图")

            except Exception as e:
                print(f"处理 {filename} 出错: {str(e)}")

if __name__ == "__main__":
    input_folder = "/home/SinSR-main/traindata/GT"  # 替换输入路径
    output_folder = "/home/SinSR-main/traindata/GT_crop"  # 替换输出路径
    
    batch_crop_images(input_folder, output_folder, crop_size=3000, min_size=3000)

































































