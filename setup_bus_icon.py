"""
Script to set up the bus icon as the app launcher icon
Place the bus icon PNG in the same directory as this script and name it 'bus_icon.png'
Then run: python setup_bus_icon.py
"""
from PIL import Image, ImageDraw
import os

# Define icon sizes for each density
icon_sizes = {
    'mdpi': 48,
    'hdpi': 72,
    'xhdpi': 96,
    'xxhdpi': 144,
    'xxxhdpi': 192
}

def create_bus_icon(size):
    """Create a blue bus icon programmatically"""
    # Create a transparent background
    img = Image.new('RGBA', (size, size), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)
    
    # Define colors
    blue = (24, 85, 183, 255)  # #1855B7 - matching the bus icon blue
    white = (255, 255, 255, 255)
    
    padding = size * 0.15
    bus_width = size - 2 * padding
    bus_height = bus_width * 0.85
    
    left = padding
    top = padding
    right = left + bus_width
    bottom = top + bus_height
    
    # Draw main bus body with rounded corners
    corner_radius = size * 0.12
    draw.rounded_rectangle(
        [(left, top), (right, bottom)],
        radius=corner_radius,
        fill=blue
    )
    
    # Draw windshield/window (rounded rectangle)
    window_margin = size * 0.08
    window_top = top + size * 0.12
    window_bottom = window_top + bus_height * 0.35
    window_left = left + window_margin
    window_right = right - window_margin
    window_radius = size * 0.06
    
    draw.rounded_rectangle(
        [(window_left, window_top), (window_right, window_bottom)],
        radius=window_radius,
        fill=white
    )
    
    # Draw top strip (destination sign)
    strip_margin = size * 0.1
    strip_height = size * 0.04
    strip_top = top + size * 0.05
    draw.rounded_rectangle(
        [(left + strip_margin, strip_top), (right - strip_margin, strip_top + strip_height)],
        radius=size * 0.02,
        fill=white
    )
    
    # Draw two wheels (circles at the bottom)
    wheel_radius = size * 0.11
    wheel_y = bottom + wheel_radius * 0.4
    wheel1_x = left + bus_width * 0.25
    wheel2_x = right - bus_width * 0.25
    
    draw.ellipse(
        [(wheel1_x - wheel_radius, wheel_y - wheel_radius),
         (wheel1_x + wheel_radius, wheel_y + wheel_radius)],
        fill=white
    )
    draw.ellipse(
        [(wheel2_x - wheel_radius, wheel_y - wheel_radius),
         (wheel2_x + wheel_radius, wheel_y + wheel_radius)],
        fill=white
    )
    
    # Draw wheel wells (darker blue arcs on the bus body)
    wheel_well_color = (18, 65, 153, 255)
    wheel_well_height = wheel_radius * 0.6
    draw.ellipse(
        [(wheel1_x - wheel_radius, bottom - wheel_well_height),
         (wheel1_x + wheel_radius, bottom + wheel_radius)],
        fill=wheel_well_color
    )
    draw.ellipse(
        [(wheel2_x - wheel_radius, bottom - wheel_well_height),
         (wheel2_x + wheel_radius, bottom + wheel_radius)],
        fill=wheel_well_color
    )
    
    return img

def create_app_icons(output_base_path):
    """Create app icons for all Android densities"""
    
    # Check if user provided their own bus icon
    custom_icon_path = "bus_icon.png"
    use_custom = os.path.exists(custom_icon_path)
    
    if use_custom:
        print(f"Using custom icon: {custom_icon_path}")
        source_img = Image.open(custom_icon_path)
        if source_img.mode != 'RGBA':
            source_img = source_img.convert('RGBA')
    else:
        print("No custom icon found, generating bus icon programmatically")
        source_img = None
    
    # Create icons for each density
    for density, size in icon_sizes.items():
        output_dir = os.path.join(output_base_path, f'mipmap-{density}')
        os.makedirs(output_dir, exist_ok=True)
        
        if use_custom:
            # Resize the custom image
            resized_img = source_img.resize((size, size), Image.Resampling.LANCZOS)
        else:
            # Generate the icon programmatically
            resized_img = create_bus_icon(size)
        
        # Save as PNG
        output_path = os.path.join(output_dir, 'ic_launcher.png')
        resized_img.save(output_path, 'PNG')
        print(f"✓ Created: {output_path}")
        
        # Also create round version
        output_path_round = os.path.join(output_dir, 'ic_launcher_round.png')
        resized_img.save(output_path_round, 'PNG')
        print(f"✓ Created: {output_path_round}")
        
        # Delete old .webp files if they exist
        webp_path = os.path.join(output_dir, 'ic_launcher.webp')
        webp_round_path = os.path.join(output_dir, 'ic_launcher_round.webp')
        
        if os.path.exists(webp_path):
            os.remove(webp_path)
            print(f"  Removed: {webp_path}")
        if os.path.exists(webp_round_path):
            os.remove(webp_round_path)
            print(f"  Removed: {webp_round_path}")
    
    print("\n✓ All icons created successfully!")
    print("\nNext steps:")
    print("1. The adaptive icon XML files have been updated")
    print("2. Rebuild your app to see the new icon")
    print("3. If you provided a custom bus_icon.png, it has been used")
    print("4. Otherwise, a blue bus icon was generated")

def update_adaptive_icon_xml(res_path):
    """Update the adaptive icon XML to use PNG instead of WebP"""
    
    xml_content = '''<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@color/ic_launcher_background"/>
    <foreground android:drawable="@mipmap/ic_launcher"/>
</adaptive-icon>'''
    
    anydpi_dir = os.path.join(res_path, 'mipmap-anydpi-v26')
    os.makedirs(anydpi_dir, exist_ok=True)
    
    # Write ic_launcher.xml
    with open(os.path.join(anydpi_dir, 'ic_launcher.xml'), 'w') as f:
        f.write(xml_content)
    
    # Write ic_launcher_round.xml
    with open(os.path.join(anydpi_dir, 'ic_launcher_round.xml'), 'w') as f:
        f.write(xml_content)
    
    print("✓ Updated adaptive icon XML files")
    
    # Also ensure we have the background color
    values_dir = os.path.join(res_path, 'values')
    colors_file = os.path.join(values_dir, 'colors.xml')
    
    if os.path.exists(colors_file):
        with open(colors_file, 'r') as f:
            content = f.read()
        
        # Check if ic_launcher_background exists
        if 'ic_launcher_background' not in content:
            # Add the color before the closing </resources> tag
            content = content.replace(
                '</resources>',
                '    <color name="ic_launcher_background">#FFFFFF</color>\n</resources>'
            )
            with open(colors_file, 'w') as f:
                f.write(content)
            print("✓ Added ic_launcher_background color to colors.xml")

if __name__ == "__main__":
    res_path = "app/src/main/res"
    
    if not os.path.exists(res_path):
        print(f"Error: Could not find res directory at {res_path}")
        print("Please run this script from the BusTrackerApp directory")
    else:
        print("Setting up bus icon as app launcher...")
        print("=" * 50)
        create_app_icons(res_path)
        update_adaptive_icon_xml(res_path)
        print("=" * 50)
        print("Done! 🚌")
