import os
import glob

def analyze_dataset():
    """Analyze dataset statistics"""
    base_dir = os.path.dirname(os.path.abspath(__file__))
    dataset_root = os.path.join(base_dir, 'datasets/pothole_combined')
    
    print("=" * 60)
    print("POTHOLE DATASET STATISTICS")
    print("=" * 60)
    
    total_images = 0
    total_annotations = 0
    
    for split in ['train', 'valid', 'test']:
        img_dir = os.path.join(dataset_root, split, 'images')
        lbl_dir = os.path.join(dataset_root, split, 'labels')
        
        images = glob.glob(os.path.join(img_dir, '*.*'))
        labels = glob.glob(os.path.join(lbl_dir, '*.txt'))
        
        num_images = len(images)
        num_labels = len(labels)
        total_images += num_images
        
        # Count total annotations
        split_annotations = 0
        for label_file in labels:
            with open(label_file, 'r') as f:
                split_annotations += len(f.readlines())
        
        total_annotations += split_annotations
        
        print(f"\n{split.upper()} SET:")
        print(f"  Images: {num_images}")
        print(f"  Labels: {num_labels}")
        print(f"  Total potholes: {split_annotations}")
        if num_images > 0:
            print(f"  Avg potholes/image: {split_annotations/num_images:.2f}")
    
    print("\n" + "=" * 60)
    print(f"TOTAL DATASET:")
    print(f"  Total images: {total_images}")
    print(f"  Total potholes: {total_annotations}")
    if total_images > 0:
        print(f"  Average potholes/image: {total_annotations/total_images:.2f}")
    print("=" * 60)
    
    # Split ratios
    train_count = len(glob.glob(os.path.join(dataset_root, 'train/images/*.*')))
    valid_count = len(glob.glob(os.path.join(dataset_root, 'valid/images/*.*')))
    test_count = len(glob.glob(os.path.join(dataset_root, 'test/images/*.*')))
    
    print(f"\nSPLIT RATIOS:")
    print(f"  Train: {train_count/total_images*100:.1f}%")
    print(f"  Valid: {valid_count/total_images*100:.1f}%")
    print(f"  Test:  {test_count/total_images*100:.1f}%")
    print("=" * 60)

if __name__ == '__main__':
    analyze_dataset()
