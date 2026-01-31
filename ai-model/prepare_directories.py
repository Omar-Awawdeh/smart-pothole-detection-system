import os

def create_structure():
    base_path = 'datasets/pothole_combined'
    subsets = ['train', 'valid', 'test']
    folders = ['images', 'labels']
    
    for subset in subsets:
        for folder in folders:
            path = os.path.join(base_path, subset, folder)
            os.makedirs(path, exist_ok=True)
            print(f"Created: {path}")
            
    print("\nDirectory structure created successfully.")
    print(f"Please place your images and labels into {base_path}")
    print("Ensure you split them into train (80%), valid (15%), and test (5%)")

if __name__ == '__main__':
    create_structure()
