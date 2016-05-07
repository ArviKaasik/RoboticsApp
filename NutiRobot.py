import RPi.GPIO as GPIO #Sisend/väljund viikude paketi lisamine  
import time
from http.server import * #HTTP seadete paketi lisamine  
import re #Lihtsustatud tekstitöötluse funktsionaalsuse lisamine  
  
GPIO.setwarnings(False) #Hoiatuste väljalülitamine  
GPIO.setmode(GPIO.BOARD) #Viikude nummerduse valimine järjekorra alusel  
GPIO.setup(11, GPIO.OUT) #Viikude 11 ja 12 väljundiks seadmine   
GPIO.setup(12, GPIO.OUT)
GPIO.setup(18, GPIO.OUT)   
GPIO.setup(16, GPIO.OUT)  
GPIO.setup(38, GPIO.OUT)
GPIO.setup(40, GPIO.IN)


pwm1 = GPIO.PWM(11, 50) #Viik nr 11 pulsilaiusmodulatsiooni seadistamine 50Hz (20ms pulsi pikkus)   
pwm2 = GPIO.PWM(12, 50) #Viik nr 12 pulsilaiusmodulatsiooni seadistamine 50Hz (20ms pulsi pikkus)   

def vilgutame_LEDi(x, pin):
	if (x == 1):
		print("Põleb")
		GPIO.output(pin,GPIO.HIGH)
	elif (x == 0):
		print ("Kustus")
		GPIO.output(pin,GPIO.LOW)
  
def liigutame_robotit(pwm1, x, pwm2, y): #Mootorite liikumise määramise funktsioon 
	pwm1.ChangeDutyCycle(7.0+x/8+y/8) #Parempoolne mootor kiiruse seadistamine vastavalt nutiseadme x ja y väärtustele
	pwm2.ChangeDutyCycle(7.0-x/8+y/8) #Vasakpoolne mootor kiiruse seadistamine vastavalt nutiseadme x ja y väärtustele
	if x > 0:
		GPIO.output(16,GPIO.HIGH)
		GPIO.output(18,GPIO.LOW)
	else:
		GPIO.output(16,GPIO.LOW)
		GPIO.output(18,GPIO.HIGH)
		
def leia_kaugus():
        GPIO_TRIGGER = 38
        GPIO_ECHO = 40

        GPIO.output(GPIO_TRIGGER, False)
        time.sleep(0.5)
        GPIO.output(GPIO_TRIGGER, True)
        time.sleep(0.00001)
        GPIO.output(GPIO_TRIGGER, False)
        start = time.time()
        while GPIO.input(GPIO_ECHO)==0:
            start = time.time()
        while GPIO.input(GPIO_ECHO)==1:
            stop = time.time()
        elapsed = stop-start
        
        distance = elapsed * 34000

        distance = distance / 2

        return distance

#Selles klassis käsitletakse HTTP päringuid, mis saabuvad serverisse  
class MyHTTPHandler(BaseHTTPRequestHandler):
    def do_POST(self):  
        self.send_response(200) #Lisatakse vastuse päis
        self.send_header("Content-type", "text/html") #Päis puhvrisse  
        self.end_headers() #Kirjutatkse puhvris olev info väljundisse  
        #Väljundite kirjutamise vormindamine  
        print ("path: " + str(self.path))
        if self.path.startswith("/update_LED"):  
            #Muutujate määramine  
            m = re.search("/update_LED\\?x=([^&]*)", self.path)  
            x = int(m.group(1))
            vilgutame_LEDi(x,18)
        elif self.path.startswith("/update_servo"):
            #Muutujate määramine
            m = re.search("/update_servo\\?x=([^&]*)&y=(.*)", self.path)  
            x = float(m.group(1))
            y = float(m.group(2))
            liigutame_robotit(pwm1, x, pwm2, y)
        elif self.path.startswith("/get_sensor"):
            kaugus = leia_kaugus()
            self.wfile.write(bytes(str(kaugus), 'UTF-8'))
        else:
            print("Teadmata päring: " + self.path)
        
    def do_GET(self):  
        self.send_response(200) #Lisatakse vastuse päis
        self.send_header("Content-type", "text/html") #Päis puhvrisse  
        self.end_headers() #Kirjutatkse puhvris olev info väljundisse  
        #Väljundite kirjutamise vormindamine  
        if self.path.startswith("/update_servo"):  
            #Muutujate määramine  
            m = re.search("/update_servo\\?x=([^&]*)&y=(.*)", self.path)  
            x = float(m.group(1))
            y = float(m.group(2))
            liigutame_robotit(pwm1, x, pwm2, y)  #Mootorite juhtimise kivitamine vastavalt nutitelefoni x ja y asendile 
  
        else:   #index faili kirjutamine/lugemine  
            f = open("index.html", "rb")  
            self.wfile.write(f.read())  
            f.close()
            
#Loome serveri, mis kuulab kindlat porti, antud juhul :8080  
def run(server_class=HTTPServer, handler_class=MyHTTPHandler):  
    server_address = ('', 8080)  
    httpd = server_class(server_address, handler_class)  
    httpd.serve_forever()  
  
  
pwm1.start(0) #Mootori pulsilaiusmodulatsiooni lubamine ja käivitamine
pwm2.start(0) #Mootori pulsilaiusmodulatsiooni lubamine ja käivitamine

run() #Käivita eelnev kood 
