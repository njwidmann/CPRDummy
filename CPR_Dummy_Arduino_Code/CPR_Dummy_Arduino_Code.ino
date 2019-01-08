// Interrupt information
// 0 on pin 2
// 1 on pin 4

#define encoderI 2
#define encoderQ 4 // Only use one interrupt in this example

//used for serial output
const String comma = ",";
const String colon = ":";
const String hashtag = "#";
const String squiggly = "~";
const String semicolon = ";";

volatile int depth;
volatile long currentTime;
volatile int vent_pressure;
void setup()
{
  Serial.begin(9600);
  depth=0;
  pinMode(encoderI, INPUT);
  pinMode(encoderQ, INPUT); 
  attachInterrupt(digitalPinToInterrupt(encoderI), handleEncoder, CHANGE);
  setupVentilations();

}

int prevDepth = 0;

void loop()
{
  currentTime = millis();
  int vent_pressure = readVentilationVelocity();
  Serial.println(hashtag + currentTime + colon + depth + comma + vent_pressure + squiggly);
}

void handleEncoder()
{

  if(digitalRead(encoderI) == digitalRead(encoderQ))
  { 
    depth--;
  }
  else
  { 
    depth++;
  }

}
