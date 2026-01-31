from ultralytics import YOLO
import os
import tensorflow as tf
import numpy as np

def export_model():
    # Path to the best trained model
    # Adjust this path if your run name or project structure is different
    model_path = 'pothole_training/yolov8n_run1/weights/best.pt'
    
    if not os.path.exists(model_path):
        print(f"Error: Trained model not found at {model_path}")
        print("Please run train.py first or check the path.")
        return

    print(f"Loading model from {model_path}...")
    model = YOLO(model_path)

    print("Exporting to TFLite (float16)...")
    # Export the model
    # format='tflite'
    # half=True for float16 quantization
    # int8=False
    # simplify=True for ONNX simplification before TFLite conversion
    output_files = model.export(
        format='tflite',
        imgsz=640,
        half=True, 
        int8=False, 
        simplify=True
    )
    
    print(f"Export complete. Files: {output_files}")
    
    # Verification steps
    tflite_path = None
    if isinstance(output_files, str):
        tflite_path = output_files
    elif isinstance(output_files, list):
         for f in output_files:
             if f.endswith('.tflite'):
                 tflite_path = f
                 break
    
    if tflite_path and os.path.exists(tflite_path):
        verify_tflite_model(tflite_path)
    else:
        print("Could not locate TFLite file for verification.")

def verify_tflite_model(tflite_path):
    print("\n--- Verifying TFLite Model ---")
    
    # 1. Check file size
    file_size_mb = os.path.getsize(tflite_path) / (1024 * 1024)
    print(f"File size: {file_size_mb:.2f} MB")
    if file_size_mb > 10:
        print("WARNING: Model is larger than 10MB")
    else:
        print("Check: Size is within limits (<10MB)")

    # 2. Load model and check details
    try:
        interpreter = tf.lite.Interpreter(model_path=tflite_path)
        interpreter.allocate_tensors()
        
        input_details = interpreter.get_input_details()
        output_details = interpreter.get_output_details()
        
        print(f"Input Shape: {input_details[0]['shape']}")
        print(f"Input Dtype: {input_details[0]['dtype']}")
        print(f"Output Shape: {output_details[0]['shape']}")
        
        # 3. Simple inference test with random data
        input_shape = input_details[0]['shape']
        input_data = np.array(np.random.random_sample(input_shape), dtype=np.float32)
        interpreter.set_tensor(input_details[0]['index'], input_data)
        
        interpreter.invoke()
        
        output_data = interpreter.get_tensor(output_details[0]['index'])
        print("Inference test successful.")
        print(f"Output result shape: {output_data.shape}")
        
    except Exception as e:
        print(f"Verification failed: {e}")

if __name__ == '__main__':
    export_model()
