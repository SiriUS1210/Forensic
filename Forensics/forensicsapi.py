from flask import Flask, request, jsonify, send_file
import torch
import numpy as np
from scipy.spatial.distance import cdist
import boto3
from PIL import Image

app = Flask(__name__)

# Initialize S3 client
s3 = boto3.client('s3')
bucket_name = 'forensics-bucket-project'

# Define the CITEModel class
class CITEModel(torch.nn.Module):
    def __init__(self):
        super(CITEModel, self).__init__()
        self.encoder = torch.nn.Sequential(
            torch.nn.Conv2d(1, 32, kernel_size=3, stride=1, padding=1),
            torch.nn.ReLU(),
            torch.nn.MaxPool2d(2),
            torch.nn.Conv2d(32, 64, kernel_size=3, stride=1, padding=1),
            torch.nn.ReLU(),
            torch.nn.MaxPool2d(2)
        )
        self.fc1 = torch.nn.Linear(64 * 32 * 32, 128)  # Encoder to feature vector
        self.fc2 = torch.nn.Linear(128, 64 * 32 * 32)  # Decoder fully connected layer
        self.decoder = torch.nn.Sequential(
            torch.nn.ConvTranspose2d(64, 32, kernel_size=3, stride=2, padding=1, output_padding=1),
            torch.nn.ReLU(),
            torch.nn.ConvTranspose2d(32, 1, kernel_size=3, stride=2, padding=1, output_padding=1),
            torch.nn.Sigmoid()  # Sigmoid to output normalized image
        )

    def forward(self, x):
        # Encode the image
        x = self.encoder(x)
        x = x.view(x.size(0), -1)  # Flatten the tensor
        compressed = self.fc1(x)

        # Decode back to the original image space (for reconstruction loss)
        x = self.fc2(compressed)
        x = x.view(-1, 64, 32, 32)
        reconstructed = self.decoder(x)

        return compressed, reconstructed

# Initialize the model
model = CITEModel()

# Load the model weights with filtering to handle extra keys
state_dict = torch.load('cite_model.pth')
model_state_dict = model.state_dict()
filtered_state_dict = {k: v for k, v in state_dict.items() if k in model_state_dict}
model.load_state_dict(filtered_state_dict, strict=False)
model.eval()

# Helper function to download and preprocess images from S3
def download_and_preprocess_image(file_name=None, bucket_name=None):
    if bucket_name:
        local_file_name = file_name.split('/')[-1]
        try:
            s3.download_file(bucket_name, file_name, local_file_name)
            img = Image.open(local_file_name).convert('L')  # Convert to grayscale
        except Exception as e:
            print(f"Failed to process image {file_name}: {e}")
            return None
    else:
        img = Image.open(file_name).convert('L')

    # Resize and normalize the image
    img = img.resize((128, 128))
    img_array = np.array(img, dtype=np.float32) / 255.0  # Normalize to [0, 1]

    # Ensure the tensor is 4D: [batch_size, channels, height, width]
    img_tensor = torch.tensor(img_array, dtype=torch.float32).unsqueeze(0).unsqueeze(0)  # Shape: [1, 1, 128, 128]
    return img_tensor

# Preprocess multiple images from a folder on S3
def preprocess_images(image_paths):
    images = []
    for image_path in image_paths:
        img_tensor = download_and_preprocess_image(bucket_name=bucket_name, file_name=image_path)
        if img_tensor is not None:
            images.append(img_tensor)
    return torch.cat(images, dim=0) if images else None

# List all image files in a specified S3 folder
def list_images_in_s3_folder(bucket, folder_name):
    response = s3.list_objects_v2(Bucket=bucket, Prefix=folder_name)
    return [item['Key'] for item in response.get('Contents', []) if item['Key'].endswith(('.jpg', '.jpeg', '.png'))]

# Load and process all photos from the S3 bucket
photos = list_images_in_s3_folder(bucket_name, 'Photos/')
photo_images = preprocess_images(photos)

# Extract features from images using the model
def extract_features(images, model):
    features = []
    for img_tensor in images:
        with torch.no_grad():
            feature_vector, _ = model(img_tensor.unsqueeze(0))  # Use the encoder output
            features.append(feature_vector)
    return torch.cat(features, dim=0)

# Extract features from all loaded photo images
photo_features = extract_features(photo_images, model)

@app.route('/upload_sketch', methods=['POST'])
def upload_sketch():
    # Handle sketch upload
    file = request.files['sketch']
    file_name = file.filename   
    file.save(file_name)

    # Preprocess the uploaded sketch
    sketch_tensor = download_and_preprocess_image(file_name=file_name)
    if sketch_tensor is None:
        return jsonify({'error': 'Failed to process the sketch'}), 400

    # Ensure the input tensor is 4D: [batch_size, channels, height, width]
    sketch_tensor = sketch_tensor.squeeze(0)  # Remove any unnecessary batch dimension

    # Extract features from the sketch
    sketch_features, _ = model(sketch_tensor.unsqueeze(0))  # Get features and ignore reconstructed image

    # Detach the sketch features to avoid the RuntimeError
    sketch_features_np = sketch_features.detach().numpy()  # Detach before converting to NumPy array

    # Calculate distances between the sketch and all photos
    distance_matrix = cdist(sketch_features_np, photo_features.detach().numpy(), metric='euclidean')  # Detach photo features as well

    # Find the closest matching photo index
    most_similar_photo_index = np.argmin(distance_matrix)

    # Calculate similarity percentage
    max_distance = np.max(distance_matrix)
    similarity_score = 100 * (1 - distance_matrix[0][most_similar_photo_index] / max_distance)

    return jsonify({
        'matched_image_id': photos[most_similar_photo_index],
        'similarity': similarity_score
    })


from io import BytesIO

@app.route('/<image_id>', methods=['GET'])
def get_image(image_id):
    try:
        # Create a BytesIO stream to hold the image data
        image_stream = BytesIO()
        
        # Download the image from S3 directly into the stream
        s3.download_fileobj(bucket_name, f'Photos/{image_id}', image_stream)
        
        # Reset the stream's position to the beginning
        image_stream.seek(0)
        
        # Return the image as a file-like object
        return send_file(image_stream, mimetype='image/jpeg')
    except Exception as e:
        print(f"Error fetching the image: {e}")
        return jsonify({"error": "Image not found"}), 404

if __name__ == '__main__':
    app.run(port=5000)
