
#include <Servo.h>

Servo servo;
char state;

void setup() {
  // put your setup code here, to run once:
  servo.attach(7);
  
  Serial.begin(9600);
}

void loop() {
  // put your main code here, to run repeatedly:
  if(Serial.available() > 0)
  {
    char data;
    data = Serial.read(); // The variable data is used to store the value sent by the Android app

    switch(data)
    {

      case '1':        
	for(int x = 120; x >= 70; x--) // Rotates the servo to the unlocked position
        {
          servo.write(x);
          delay(15);
        }  
        break;

      case '2': 
        for(int a = 70; a <= 120; a++) // Rotates the servo to the locked position
        {
          servo.write(a);
          delay(15);
          Serial.println(servo.read());
        }
        break;
    }
  }
  
}
