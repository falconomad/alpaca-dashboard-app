import os
from PIL import Image

def generate_launcher_assets():
    source_path = "app/src/main/res/drawable/ic_launcher.jpg"
    if not os.path.exists(source_path):
        print(f"Source icon not found at: {source_path}")
        return

    img = Image.open(source_path)

    # Output targets
    targets = [
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

    for dir_path, filename, size in targets:
        os.makedirs(dir_path, exist_ok=True)
        dest_path = os.path.join(dir_path, filename)
        
        # Directly resize the full-bleed canvas
        resized_img = img.resize((size, size), Image.Resampling.LANCZOS)
        resized_img.save(dest_path, "PNG")
        print(f"Generated: {dest_path} ({size}x{size})")

if __name__ == "__main__":
    generate_launcher_assets()
