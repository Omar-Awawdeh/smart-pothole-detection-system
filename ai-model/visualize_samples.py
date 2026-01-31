import os
import random
import cv2
import matplotlib.pyplot as plt
import matplotlib.patches as patches

def load_yolo_label(label_path):
    """Load YOLO format label file"""
    boxes = []
    with open(label_path, 'r') as f:
        for line in f:
            parts = line.strip().split()
            if len(parts) == 5:
                class_id, x_center, y_center, width, height = map(float, parts)
                boxes.append((class_id, x_center, y_center, width, height))
    return boxes

def draw_boxes(image, boxes):
    """Draw bounding boxes on image"""
    h, w = image.shape[:2]
    fig, ax = plt.subplots(1, figsize=(10, 8))
    ax.imshow(cv2.cvtColor(image, cv2.COLOR_BGR2RGB))
    
    for class_id, x_center, y_center, box_w, box_h in boxes:
        # Convert normalized YOLO coords to pixel coords
        x_center_px = x_center * w
        y_center_px = y_center * h
        box_w_px = box_w * w
        box_h_px = box_h * h
        
        # Convert center coords to top-left corner
        x1 = x_center_px - box_w_px / 2
        y1 = y_center_px - box_h_px / 2
        
        rect = patches.Rectangle((x1, y1), box_w_px, box_h_px,
                                 linewidth=2, edgecolor='red', facecolor='none')
        ax.add_patch(rect)
        ax.text(x1, y1 - 5, 'pothole', color='red', fontsize=12, 
               bbox=dict(facecolor='white', alpha=0.7))
    
    ax.axis('off')
    return fig

def visualize_samples(num_samples=5):
    """Visualize random samples from the dataset"""
    base_dir = os.path.dirname(os.path.abspath(__file__))
    train_img_dir = os.path.join(base_dir, 'datasets/pothole_combined/train/images')
    train_lbl_dir = os.path.join(base_dir, 'datasets/pothole_combined/train/labels')
    
    images = os.listdir(train_img_dir)
    samples = random.sample(images, min(num_samples, len(images)))
    
    for img_name in samples:
        img_path = os.path.join(train_img_dir, img_name)
        label_path = os.path.join(train_lbl_dir, img_name.replace('.jpg', '.txt').replace('.png', '.txt').replace('.jpeg', '.txt'))
        
        if not os.path.exists(label_path):
            print(f"Label not found for {img_name}")
            continue
        
        image = cv2.imread(img_path)
        boxes = load_yolo_label(label_path)
        
        print(f"\n{img_name}")
        print(f"  Image size: {image.shape[:2]}")
        print(f"  Number of potholes: {len(boxes)}")
        
        fig = draw_boxes(image, boxes)
        plt.title(f"{img_name} - {len(boxes)} pothole(s)")
        plt.tight_layout()
        plt.show()

if __name__ == '__main__':
    print("Visualizing random samples from training set...")
    visualize_samples(num_samples=3)
