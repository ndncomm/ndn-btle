/*
This RFduino sketch demonstrates a full bi-directional Bluetooth Low
Energy 4 connection between an iPhone application and an RFduino.

This sketch works with the rfduinoLedButton iPhone application.

The button on the iPhone can be used to turn the green led on or off.
The button state of button 1 is transmitted to the iPhone and shown in
the application.
*/

/*
 Copyright (c) 2014 OpenSourceRF.com.  All right reserved.

 This library is free software; you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public
 License as published by the Free Software Foundation; either
 version 2.1 of the License, or (at your option) any later version.

 This library is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 See the GNU Lesser General Public License for more details.

 You should have received a copy of the GNU Lesser General Public
 License along with this library; if not, write to the Free Software
 Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA

 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
 CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
*/

/*
 * 
 * susmit@cs.colostate.edu
 */


#include <RFduinoBLE.h>
#include <ndn-cpp/lite/data-lite.hpp>
#include <ndn-cpp/lite/interest-lite.hpp>
#include <ndn-cpp/lite/encoding/tlv-0_1_1-wire-format-lite.hpp>
// TODO: Make an API for these.

using namespace ndn;
char multipacketData[151]=  {};

// pin 3 on the RGB shield is the red led
// (can be turned on/off from the iPhone app)
int red_led = 2;
int green_led = 3;
int blue_led = 4;


// pin 5 on the RGB shield is button 1
// (button press will be shown on the iPhone app)
int button = 5;

// debounce time (in ms)
int debounce_time = 10;

// maximum debounce timeout (in ms)
int debounce_timeout = 100;

int duration = SECONDS(5);

void setup() {
  Serial.begin(9600);
  while (!Serial); // Wait untilSerial is ready.\
  Serial.println("Serial initialized");

  pinMode(red_led, OUTPUT);
  pinMode(green_led, OUTPUT);  
  pinMode(blue_led, OUTPUT);
  
  //start announcing
  RFduinoBLE.advertisementData = "RFduino #1";
  
  // start the BLE stack
  RFduinoBLE.begin();
}


void loop() {
  RFduino_ULPDelay(INFINITE);
}

void RFduinoBLE_onConnect()
{
  // ready to connect again when done
  Serial.println("Device connected");
  digitalWrite(blue_led, 255);
}


void RFduinoBLE_onDisconnect()
{
  Serial.println("Device disconnected");
  // ready to connect again when done
 digitalWrite(red_led, 255);
}

void RFduinoBLE_onReceive(char *data, int len)
{

  digitalWrite(blue_led, 0);

  Serial.print("Recived data from remote device");
 
  //RFduinoBLE.send("test", 4);  
  digitalWrite(green_led, 123);
  char testData[] = {0x05, 0x12, 0x07, 0x0a, 0x08, 0x03, 0x61, 0x62, 0x63, 0x08, 0x03, 0x31, 0x32, 0x33, 0x0a, 0x04, 0xf0, 0x2f, 0x11, 0xf4};
  int error = replyToInterest((const uint8_t *)testData, (size_t)sizeof(testData));
  Serial.print("Error: ");
  Serial.println(error);
 
}


void RFduinoBLE_onAdvertisement(bool start)
{
  // turn the green led on if we start advertisement, and turn it
  // off if we stop advertisement
  
  if (start)
    digitalWrite(red_led, HIGH);
  else
    digitalWrite(red_led, LOW);
}



/** 
 * Decode the element as an interest and check the prefix. 
 */
static ndn_Error
replyToInterest(const uint8_t *element, size_t elementLength)
{
  Serial.println("In reply");
  // Decode the element as an InterestLite.
  ndn_NameComponent interestNameComponents[3];
  struct ndn_ExcludeEntry excludeEntries[2];
  InterestLite interest
    (interestNameComponents, sizeof(interestNameComponents) / sizeof(interestNameComponents[0]), 
     excludeEntries, sizeof(excludeEntries) / sizeof(excludeEntries[0]), 0, 0);
  size_t signedPortionBeginOffset, signedPortionEndOffset;
  ndn_Error error;
  if ((error = Tlv0_1_1WireFormatLite::decodeInterest
       (interest, element, elementLength, &signedPortionBeginOffset, 
        &signedPortionEndOffset)))
    return error;
  
  // We expect the interest name to be "/mac/reading_number". 
  // Check the size here. We construct the data name below and if it doesn't 
  // match the interest prefix, then the forwarder will drop it.
  if (interest.getName().size() != 2)
    // Ignore an unexpected prefix.
    return NDN_ERROR_success;

 
  // Create the response data packet.
  ndn_NameComponent dataNameComponents[2];
  DataLite data(dataNameComponents, sizeof(dataNameComponents) / sizeof(dataNameComponents[0]), 0, 0);  
  //data.getName().append(interest.getName());
  data.getName().append(interest.getName().get(0));
  data.getName().append(interest.getName().get(1));

  // Set the content to an analog reading.
  float reading = RFduino_temperature(FAHRENHEIT);
  char contentBuffer[12];
  Serial.println(reading); 
  sprintf(contentBuffer, "%d", (int)reading);
  data.setContent(BlobLite((const uint8_t*)contentBuffer, strlen(contentBuffer)));
  Serial.println(contentBuffer);
  Serial.println(strlen(contentBuffer));
  
  // Set up the signature with the hmacKeyDigest key locator digest.
  // TODO: Change to ndn_SignatureType_HmacWithSha256Signature when
  //   SignatureHmacWithSha256 is in the NDN-TLV Signature spec:
  //   http://named-data.net/doc/ndn-tlv/signature.html
  data.getSignature().setType(ndn_SignatureType_DigestSha256Signature);
  
  // Encode once to get the signed portion.
  uint8_t encoding[120] = {0};
  DynamicUInt8ArrayLite output(encoding, sizeof(encoding), 0);
  size_t encodingLength;
  if ((error = Tlv0_1_1WireFormatLite::encodeData
       (data, &signedPortionBeginOffset, &signedPortionEndOffset, 
  output, &encodingLength)))
    return error;

  // Encode again to include the signature.
  if ((error = Tlv0_1_1WireFormatLite::encodeData
       (data, &signedPortionBeginOffset, &signedPortionEndOffset, 
  output, &encodingLength)))
    return error;

       char temp[10];
       for (int i = 0; i < encodingLength ; ++i) {
         sprintf(temp, "%02x ", (int)encoding[i]);
         Serial.print(temp);
       }    
  Serial.println("");
  unsigned char sendBuf[20] = {0};
  unsigned int curSize = 0;
  unsigned int numPackets  = 0;
  unsigned int curIndex = 0;
  Serial.println(encodingLength);
  if (encodingLength%18 != 0)
  {
    numPackets = encodingLength/18 + 1; 
  }
  else{
    numPackets = encodingLength/18;
  }


  while(curSize < encodingLength){
     sendBuf[0] = (unsigned char)curIndex;
     sendBuf[1] = (unsigned char)numPackets;
     Serial.println(encodingLength-curSize);
     if (encodingLength-curSize < 18){
      Serial.println(encodingLength-curSize);
       memcpy(sendBuf+2, encoding+curSize, encodingLength-curSize);
       RFduinoBLE.send((const char *)sendBuf, encodingLength-curSize+2);
       char temp[10];
       for (int i = 0; i < encodingLength-curSize +2 ; ++i) {
         sprintf(temp, "%02x ", (int)sendBuf[i]);
         Serial.print(temp);
       }
       Serial.println("");
       curSize += encodingLength-curSize;
     }
     else{
       memcpy(sendBuf+2, encoding+curSize, 18);
       char temp[10];
       for (int i = 0; i < 20 ; ++i) {
         sprintf(temp, "%02x ", (int)sendBuf[i]);
         Serial.print(temp);
       }
       Serial.println("");
       RFduinoBLE.send((const char *)sendBuf,20);
        curSize += 18;
     }
     
     
     curIndex++;
  }
  
   
  Serial.println("Data size");
  Serial.println((char *)encoding);
  Serial.println(sizeof(encoding));

  return NDN_ERROR_success;
}

