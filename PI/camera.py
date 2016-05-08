import cv2

class VideoCamera(object):
	def __init__(self):
		self.video = cv2.VideoCapture (0)
		self.video.set(3,320)
                self.video.set(4,240)
		self.video.set(5,30)


	def __del__(self):
		self.video.release()

	def get_frame(self):
		success, image = self.video.read()
		ret, jpeg = cv2.imencode('.jpg',image)
		return jpeg.tobytes()
