# Committee Presentation Notes

## Slide 1 - Title

- Introduce the project as an end-to-end smart pothole detection and reporting system.
- Mention the team members and that the goal is to support road maintenance authorities with AI-assisted monitoring.

## Slide 2 - Problem Statement

- Explain that manual inspection is slow, expensive, and hard to scale.
- Emphasize the practical impact of potholes on safety, vehicles, and maintenance cost.

## Slide 3 - Objectives

- State that the project goal is not only detection, but also reporting, visualization, and deployment.
- Highlight the focus on a usable real-world workflow.

## Slide 4 - Proposed Solution

- Walk through the pipeline from camera input to dashboard output.
- Stress that the detection runs directly on the smartphone.

## Slide 5 - System Architecture

- Briefly explain each layer: mobile, AI, backend, database, dashboard, deployment.
- Mention that the architecture supports both field usage and administrative monitoring.

## Slide 6 - AI Model and Dataset

- Explain why YOLOv8n was selected: light enough for mobile while keeping strong accuracy.
- Mention the dataset size and that the final model is exported as TFLite.

## Slide 7 - Android Application

- Describe the real-time camera overlay, GPS tagging, and upload queue.
- Mention that the app is designed to remain responsive and mobile-friendly.

## Slide 8 - Backend and Dashboard

- Explain that the backend stores reports and the dashboard helps authorities review and manage them.
- Point out the map-based view and status management.

## Slide 9 - Results

- Present the key metrics clearly.
- Explain that the team tuned the operating point to improve recall because missing potholes is costly in practice.

## Slide 10 - Deployment and Demo Readiness

- Mention the VPS, Docker Compose, NGINX, and the live domains.
- This slide is useful to show that the project is deployable, not only theoretical.

## Slide 11 - Limitations and Future Work

- Be honest about dataset bias and environmental challenges.
- Show that you already know the next research and engineering steps.

## Slide 12 - Conclusion

- Conclude that the project successfully combines AI, mobile development, backend engineering, and deployment.
- End by restating the practical value for smart-city maintenance workflows.
