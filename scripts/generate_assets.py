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
except ImportError:
    print("numpy not found. Installing numpy...")
    subprocess.check_call([sys.executable, "-m", "pip", "install", "numpy"])
    from PIL import Image, ImageDraw
    import numpy as np

def generate_adaptive_assets():
    source_path = "app/src/main/res/drawable/ic_launcher.jpg"
    if not os.path.exists(source_path):
        print(f"Source icon not found at: {source_path}")
        return

    # 1. Load source image
    img = Image.open(source_path).convert("RGBA")
    data = np.array(img)

    # 2. Isolate the rose-red umbrella (High Red, lower Green/Blue)
    red_mask = (data[:, :, 0] > 100) & (data[:, :, 1] < 120) & (data[:, :, 2] < 120)

    # Create transparent canvas
    rgba = np.zeros_like(data)
    rgba[red_mask] = data[red_mask]
    rgba[red_mask, 3] = 255 # Set opaque alpha for the logo

    logo_img = Image.fromarray(rgba, 'RGBA')

    # Bounding box crop
    y_indices, x_indices = np.nonzero(red_mask)
    if len(y_indices) > 0:
        ymin, ymax = y_indices.min(), y_indices.max()
        xmin, xmax = x_indices.min(), x_indices.max()
        cropped_logo = logo_img.crop((xmin, ymin, xmax + 1, ymax + 1))
    else:
        cropped_logo = logo_img

    # Create transparent 512x512 foreground canvas
    fg_canvas = Image.new("RGBA", (512, 512), (0, 0, 0, 0))
    
    # Scale the logo to fit safely inside the 60% safe zone (around 260x260px) to prevent clipping
    safe_size = 260
    aspect_ratio = cropped_logo.width / cropped_logo.height
    if aspect_ratio > 1:
        logo_w = safe_size
        logo_h = int(safe_size / aspect_ratio)
    else:
        logo_h = safe_size
        logo_w = int(safe_size * aspect_ratio)

    resized_logo = cropped_logo.resize((logo_w, logo_h), Image.Resampling.LANCZOS)
    
    # Center paste
    paste_x = (512 - logo_w) // 2
    paste_y = (512 - logo_h) // 2
    fg_canvas.paste(resized_logo, (paste_x, paste_y), resized_logo)

    # Save foreground file
    os.makedirs("app/src/main/res/drawable", exist_ok=True)
    fg_canvas.save("app/src/main/res/drawable/ic_launcher_foreground.png", "PNG")
    print("Generated: app/src/main/res/drawable/ic_launcher_foreground.png")

    # 3. Create colors.xml with background color
    os.makedirs("app/src/main/res/values", exist_ok=True)
    colors_content = """<?xml version="1.0" encoding="utf-8"?>
<resources>
    <color name="ic_launcher_background">#121214</color>
</resources>
"""
    with open("app/src/main/res/values/colors.xml", "w", encoding="utf-8") as f:
        f.write(colors_content)
    print("Generated: app/src/main/res/values/colors.xml")

    # 4. Create mipmap-anydpi-v26 XML files
    anydpi_path = "app/src/main/res/mipmap-anydpi-v26"
    os.makedirs(anydpi_path, exist_ok=True)
    
    adaptive_content = """<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@color/ic_launcher_background"/>
    <foreground android:drawable="@drawable/ic_launcher_foreground"/>
</adaptive-icon>
"""
    with open(os.path.join(anydpi_path, "ic_launcher.xml"), "w", encoding="utf-8") as f:
        f.write(adaptive_content)
    with open(os.path.join(anydpi_path, "ic_launcher_round.xml"), "w", encoding="utf-8") as f:
        f.write(adaptive_content)
    print(f"Generated: {anydpi_path}/ic_launcher.xml and ic_launcher_round.xml")

    # 5. Legacy PNG Fallbacks
    legacy_targets = [
        ("app/src/main/res/mipmap-mdpi", "ic_launcher.png", 48),
        ("app/src/main/res/mipmap-hdpi", "ic_launcher.png", 72),
        ("app/src/main/res/mipmap-xhdpi", "ic_launcher.png", 96),
        ("app/src/main/res/mipmap-xxhdpi", "ic_launcher.png", 144),
        ("app/src/main/res/mipmap-xxxhdpi", "ic_launcher.png", 192),
        ("app/src/main/res/mipmap-mdpi", "ic_launcher_round.png", 48),
        ("app/src/main/res/mipmap-hdpi", "ic_launcher_round.png", 72),
        ("app/src/main/res/mipmap-xhdpi", "ic_launcher_round.png", 96),
        ("app/src/main/res/mipmap-xxhdpi", "ic_launcher_round.png", 144),
        ("app/src/main/res/mipmap-xxxhdpi", "ic_launcher_round.png", 192),
        ("assets", "app_icon_512.png", 512)
    ]

    for dir_path, filename, size in legacy_targets:
        os.makedirs(dir_path, exist_ok=True)
        dest_path = os.path.join(dir_path, filename)
        resized_img = img.resize((size, size), Image.Resampling.LANCZOS)
        resized_img.save(dest_path, "PNG")
        print(f"Generated Legacy: {dest_path} ({size}x{size})")

if __name__ == "__main__":
    generate_adaptive_assets()
