AudioJackTransfer
=================

Transfer data through male-to-male audio jack cable from one android device to another android device.

When you start the application, there are two buttons. First for sender and another for receiver.

The main classes and methods are summarized below:

    SenderActivity: Keep input value, encode that value. 
            encodeMessage: Encodes the message and plays the sound signal using FSK. 
    ReceiverActivity: Start the receiver to receive the sound signal from mic.  The data received is stored in a buffer and then will appear on the screen
    AudioReceiver: Receive the data/message.
            messageReceived: Check the message is received, if not then send last message.
    

FSK Modulation/Demodulation:

  When app starts a thread object that continuously records sound. 

    ArduinoService: sound acquisition
          AudioRecordingRun(): loop that records sound
          write(int): encodes the integer and plays the sound signal.
    FSKDecoder: Thread object that demodulates the sound signal.
    FSKModule: Utility static methods related to FSK modulation/demodulation.
          encode(int): encodes the number into a sound signal
          decodeSound(double): decodes the sound signal into an integer
    ErrorDetection: Before transmission the number(between 0 and 31) is added with a checksum. Then at the reception, the message is decoded and if checksum check:
          fails: an ARQ (Automatic Request Query) code is sent, then the sender repeats the last message sent.
          agrees: an ACK (Acknowledgment) code is sent (if not received the sender tries to the send the last message again). 
          
          
      
