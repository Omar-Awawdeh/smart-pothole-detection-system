from ultralytics import YOLO
import os

def train_model():
    # Load a model
    # Build a new model from scratch or load a pretrained model (recommended for training)
    model = YOLO('yolov8n.pt')  

    # Training parameters from the plan
    # epochs: 100
    # batch: 16 (or 8 if OOM)
    # imgsz: 640
    # patience: 20
    # project: pothole_training
    # name: yolov8n_run1
    
    # Check if data.yaml exists
    data_path = 'data.yaml'
    if not os.path.exists(data_path):
        print(f"Error: {data_path} not found. Please ensure dataset is prepared.")
        return

    print("Starting training...")
    results = model.train(
        data=data_path,
        epochs=100,
        imgsz=640,
        batch=16,
        patience=20,
        save=True,
        project='pothole_training',
        name='yolov8n_run1',
        optimizer='AdamW',
        lr0=0.01,
        lrf=0.01,
        momentum=0.937,
        weight_decay=0.0005,
        augment=True,
        hsv_h=0.015,
        hsv_s=0.7,
        hsv_v=0.4,
        degrees=10.0,
        translate=0.1,
        scale=0.5,
        fliplr=0.5,
        mosaic=1.0,
        mixup=0.1
    )
    
    # Evaluate the model's performance on the validation set
    print("Evaluating model...")
    metrics = model.val()
    print(f"mAP@50: {metrics.box.map50}")
    print(f"mAP@50-95: {metrics.box.map}")

    # Export the model is handled in a separate script, but we can do it here too if desired.
    # Leaving it separate as per the plan having a distinct export phase.

if __name__ == '__main__':
    train_model()
