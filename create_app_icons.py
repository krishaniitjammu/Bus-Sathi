"""
Script to create Android app icons from a source image
"""
from PIL import Image
import os

# Define icon sizes for each density
icon_sizes = {
    'mdpi': 48,
    'hdpi': 72,
    'xhdpi': 96,
    'xxhdpi': 144,
    'xxxhdpi': 192
}

def create_app_icons(source_image_path, output_base_path):
    """
    Create app icons for all Android densities
    
    Args:
        source_image_path: Path to the source PNG image
        output_base_path: Base path to the res directory
    """
    # Open the source image
    img = Image.open(source_image_path)
    
    # Convert to RGBA if not already
    if img.mode != 'RGBA':
        img = img.convert('RGBA')
    
    # Create icons for each density
    for density, size in icon_sizes.items():
        # Create output directory if it doesn't exist
        output_dir = os.path.join(output_base_path, f'mipmap-{density}')
        os.makedirs(output_dir, exist_ok=True)
        
        # Resize image
        resized_img = img.resize((size, size), Image.Resampling.LANCZOS)
        
        # Save as PNG (Android now prefers PNG for better compatibility)
        output_path = os.path.join(output_dir, 'ic_launcher.png')
        resized_img.save(output_path, 'PNG')
        print(f"Created: {output_path}")
        
        # Also create round version (same image)
        output_path_round = os.path.join(output_dir, 'ic_launcher_round.png')
        resized_img.save(output_path_round, 'PNG')
        print(f"Created: {output_path_round}")
    
    print("\nAll icons created successfully!")
    print("\nNote: You may need to delete the old .webp files and update ic_launcher.xml")

if __name__ == "__main__":
    # Path to the source image (the bus icon)
    source_image = "bus_icon.png"
    
    # Path to the res directory
    res_path = "app/src/main/res"
    
    if os.path.exists(source_image):
        create_app_icons(source_image, res_path)
    else:
        print(f"Error: Source image '{source_image}' not found!")
        print("Please save the bus icon PNG as 'bus_icon.png' in the BusTrackerApp directory")
