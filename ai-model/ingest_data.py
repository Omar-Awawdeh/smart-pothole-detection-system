import os
import shutil
import zipfile
import glob
import random
from pathlib import Path

# Configuration
BASE_DIR = os.path.dirname(os.path.abspath(__file__))
DOWNLOADS_DIR = os.path.join(BASE_DIR, 'downloads')
DATASET_ROOT = os.path.join(BASE_DIR, 'datasets/pothole_combined')
TRAIN_IMG = os.path.join(DATASET_ROOT, 'train/images')
TRAIN_LBL = os.path.join(DATASET_ROOT, 'train/labels')
VAL_IMG = os.path.join(DATASET_ROOT, 'valid/images')
VAL_LBL = os.path.join(DATASET_ROOT, 'valid/labels')
TEST_IMG = os.path.join(DATASET_ROOT, 'test/images')
TEST_LBL = os.path.join(DATASET_ROOT, 'test/labels')

def setup_dirs():
    for d in [TRAIN_IMG, TRAIN_LBL, VAL_IMG, VAL_LBL, TEST_IMG, TEST_LBL]:
        os.makedirs(d, exist_ok=True)

def is_yolo_label(file_path):
    try:
        with open(file_path, 'r') as f:
            lines = f.readlines()
            if not lines: return False # Empty file is possibly valid (no objects), but let's be careful
            for line in lines:
                parts = line.strip().split()
                if len(parts) != 5:
                    return False
                # Check normalized coords
                if not all(0.0 <= float(p) <= 1.0 for p in parts[1:]):
                    return False
        return True
    except:
        return False

def process_zip(zip_path):
    print(f"Processing {zip_path}...")
    temp_dir = os.path.join(BASE_DIR, 'temp_extract', os.path.basename(zip_path).replace('.zip', ''))
    
    with zipfile.ZipFile(zip_path, 'r') as zip_ref:
        zip_ref.extractall(temp_dir)
    
    # Find all images
    image_extensions = ['*.jpg', '*.jpeg', '*.png']
    images = []
    for ext in image_extensions:
        images.extend(glob.glob(os.path.join(temp_dir, '**', ext), recursive=True))
    
    print(f"Found {len(images)} images in {zip_path}")
    
    count = 0
    for img_path in images:
        # Find corresponding label
        # Assumptions: 
        # 1. Label has same name as image but .txt
        # 2. Label is in 'labels' folder parallel to 'images' OR same folder
        
        img_path_obj = Path(img_path)
        label_name = img_path_obj.stem + '.txt'
        
        # Strategy 1: Check same folder
        label_path = img_path_obj.parent / label_name
        
        # Strategy 2: Check 'labels' folder if current is 'images'
        if not label_path.exists() and img_path_obj.parent.name == 'images':
             # try replacing 'images' with 'labels' in path
             label_path = img_path_obj.parents[1] / 'labels' / label_name
             
        # Strategy 3: Recursive search for the text file (slow but robust)
        if not label_path.exists():
            # Skip for now to keep it simple, or implement if needed
            pass

        if label_path.exists() and is_yolo_label(label_path):
            # Decide split
            r = random.random()
            if r < 0.8:
                dest_img = TRAIN_IMG
                dest_lbl = TRAIN_LBL
            elif r < 0.95:
                dest_img = VAL_IMG
                dest_lbl = VAL_LBL
            else:
                dest_img = TEST_IMG
                dest_lbl = TEST_LBL
                
            # Copy files
            # Rename to avoid collisions: zipname_filename
            prefix = os.path.basename(zip_path).replace('.zip', '') + '_'
            new_name = prefix + img_path_obj.name
            new_label_name = prefix + label_name
            
            shutil.copy2(img_path, os.path.join(dest_img, new_name))
            shutil.copy2(label_path, os.path.join(dest_lbl, new_label_name))
            count += 1
            
    print(f"Imported {count} valid image/label pairs from {zip_path}")
    
    # Cleanup
    shutil.rmtree(os.path.join(BASE_DIR, 'temp_extract'), ignore_errors=True)

def main():
    setup_dirs()
    zips = glob.glob(os.path.join(DOWNLOADS_DIR, '*.zip'))
    
    if not zips:
        print(f"No zip files found in {DOWNLOADS_DIR}")
        print("Please place your dataset zip files there.")
        return

    for z in zips:
        process_zip(z)
        
    print("\nIngestion complete.")
    
    # Stats
    for name, p in [('Train', TRAIN_IMG), ('Valid', VAL_IMG), ('Test', TEST_IMG)]:
        print(f"{name}: {len(os.listdir(p))} images")

if __name__ == '__main__':
    main()
