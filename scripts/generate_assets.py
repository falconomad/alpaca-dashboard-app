import os
import sys
import subprocess

try:
    from PIL import Image, ImageDraw
except ImportError:
    print("Pillow not found. Installing Pillow...")
    subprocess.check_call([sys.executable, "-m", "pip", "install", "Pillow"])
    from PIL import Image, ImageDraw

try:
    import numpy as np
    from scipy.ndimage import binary_fill_holes
except ImportError:
    print("numpy/scipy not found. Installing dependencies...")
    subprocess.check_call([sys.executable, "-m", "pip", "install", "numpy", "scipy"])
    import numpy as np
    from scipy.ndimage import binary_fill_holes

def extract_logo(source_path):
    # Load original icon
    img = Image.open(source_path)
    data = np.array(img)

    # 1. Create masks
    white_mask = (data[:, :, 0] > 180) & (data[:, :, 1] > 180) & (data[:, :, 2] > 180)
    green_mask = (data[:, :, 1] > 100) & (data[:, :, 1] > data[:, :, 0] * 1.2) & (data[:, :, 1] > data[:, :, 2] * 1.2)

    # 2. Fill eye and nostril holes inside the white head
    filled_white_mask = binary_fill_holes(white_mask)
    holes_mask = filled_white_mask & ~white_mask

    # Restrict holes to the llama head bounding box
    y_indices, x_indices = np.nonzero(white_mask)
    if len(y_indices) > 0:
        ymin, ymax = y_indices.min(), y_indices.max()
        xmin, xmax = x_indices.min(), x_indices.max()
        valid_holes = np.zeros_like(holes_mask)
        valid_holes[ymin:ymax, xmin:xmax] = holes_mask[ymin:ymax, xmin:xmax]
    else:
        valid_holes = holes_mask

    # 3. Create RGBA image of the logo
    h, w, c = data.shape
    rgba = np.zeros((h, w, 4), dtype=np.uint8)
    rgba[:, :, :3] = data
    
    # Set alpha for logo (white + green)
    logo_mask = white_mask | green_mask
    rgba[logo_mask, 3] = 255
    
    # Set eye/nostril to dark grey
    rgba[valid_holes, :3] = [32, 32, 32]
    rgba[valid_holes, 3] = 255

    logo_img = Image.fromarray(rgba, 'RGBA')
    
    # Crop to bounding box
    all_logo_mask = logo_mask | valid_holes
    y_indices, x_indices = np.nonzero(all_logo_mask)
    if len(y_indices) > 0:
        ymin, ymax = y_indices.min(), y_indices.max()
        xmin, xmax = x_indices.min(), x_indices.max()
        cropped_logo = logo_img.crop((xmin, ymin, xmax + 1, ymax + 1))
    else:
        cropped_logo = logo_img
        
    return cropped_logo

def generate_launcher_assets():
    source_path = "app/src/main/res/drawable/ic_launcher.jpg"
    if not os.path.exists(source_path):
        print(f"Source icon not found at: {source_path}")
        return

    print("Extracting and processing logo from source...")
    logo = extract_logo(source_path)

    # Output targets
    # (directory, filename, size, background_type)
    # background_type can be:
    # - 'circle': white circular background on transparent canvas (diameter = 90% of canvas)
    # - 'rounded_square': white rounded square background on transparent canvas (size = 90% of canvas)
    # - 'square': full solid white square background (size = 100% of canvas)
    targets = [
        # Standard launcher icons
        ("app/src/main/res/mipmap-mdpi", "ic_launcher.png", 48, 'rounded_square'),
        ("app/src/main/res/mipmap-hdpi", "ic_launcher.png", 72, 'rounded_square'),
        ("app/src/main/res/mipmap-xhdpi", "ic_launcher.png", 96, 'rounded_square'),
        ("app/src/main/res/mipmap-xxhdpi", "ic_launcher.png", 144, 'rounded_square'),
        ("app/src/main/res/mipmap-xxxhdpi", "ic_launcher.png", 192, 'rounded_square'),
        
        # Round launcher icons
        ("app/src/main/res/mipmap-mdpi", "ic_launcher_round.png", 48, 'circle'),
        ("app/src/main/res/mipmap-hdpi", "ic_launcher_round.png", 72, 'circle'),
        ("app/src/main/res/mipmap-xhdpi", "ic_launcher_round.png", 96, 'circle'),
        ("app/src/main/res/mipmap-xxhdpi", "ic_launcher_round.png", 144, 'circle'),
        ("app/src/main/res/mipmap-xxxhdpi", "ic_launcher_round.png", 192, 'circle'),
        
        # High resolution Google Play asset
        ("assets", "app_icon_512.png", 512, 'square')
    ]

    for dir_path, filename, size, bg_type in targets:
        os.makedirs(dir_path, exist_ok=True)
        dest_path = os.path.join(dir_path, filename)
        
        # Create output image
        out_img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
        draw = ImageDraw.Draw(out_img)
        
        # Draw background shapes
        if bg_type == 'circle':
            bg_size = int(size * 0.90)
            margin = (size - bg_size) // 2
            draw.ellipse(
                [margin, margin, size - margin, size - margin],
                fill=(255, 255, 255, 255)
            )
            # Logo is 60% of background diameter -> 54% of canvas size
            logo_h = int(bg_size * 0.60)
        elif bg_type == 'rounded_square':
            bg_size = int(size * 0.90)
            margin = (size - bg_size) // 2
            radius = int(bg_size * 0.20)
            draw.rounded_rectangle(
                [margin, margin, size - margin, size - margin],
                radius=radius,
                fill=(255, 255, 255, 255)
            )
            # Logo is 60% of background size -> 54% of canvas size
            logo_h = int(bg_size * 0.60)
        elif bg_type == 'square':
            # Solid white background (full canvas)
            out_img = Image.new("RGBA", (size, size), (255, 255, 255, 255))
            # Logo is 60% of canvas size
            logo_h = int(size * 0.60)
        
        # Resize logo
        aspect_ratio = logo.width / logo.height
        logo_w = int(logo_h * aspect_ratio)
        resized_logo = logo.resize((logo_w, logo_h), Image.Resampling.LANCZOS)
        
        # Paste centered
        logo_x = (size - logo_w) // 2
        logo_y = (size - logo_h) // 2
        out_img.paste(resized_logo, (logo_x, logo_y), resized_logo)
        
        # Save as PNG
        out_img.save(dest_path, "PNG")
        print(f"Generated: {dest_path} ({size}x{size}) - Type: {bg_type}")

if __name__ == "__main__":
    generate_launcher_assets()
