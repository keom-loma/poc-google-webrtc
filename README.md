# WebRTC Integration in Android 📱🌐🎥

### #This giude provide step-by-step instructions to integrate WebRTC in an Android application using an AAR file.

   ## Prerequisites:
   
   ### 🚀 Android Studio 
   ### 🚀 Minimum SDK:21+
   ### ⚡ Internet permission in AndroidManifast.xml
   ### ⚡ Setup SRS Server

   #### Url for streaming:
   - https://oryx.lomasq.com/rtc/v1/whep/?app=live&stream=livestream&eip=91.108.105.153:8888

   ## Web Player Reference:
   - https://github.com/Eyevinn/webrtc-player



   

## Installation
#### Since we are using an AAR file for the WebRTC SDK, follow these steps to install it manually:

  #### ✨ Place the libwebrtc.aar file inside the libs folder of your project.
  #### ✨ Modify the build.gradle (Module: app) to include the AAR file.
  #### ✨ Sync the project to apply changes.

## Implementation Steps

  ### 📌 Initialize Peer Connection Factory
  #### Before using WebRTC, initialize the PeerConnectionFactory with the necessary options and settings.
  
  ### 📌 Generate and Send SDP Offer
  #### Create an SDP offer and send it to the server using an HTTP client like OkHttp. Ensure that the offer is properly formatted and follows WebRTC standards.
  
  ### 📌 Set Remote Description
  #### After receiving the response from the server, set the remote description of the peer connection using the provided SDP.
  
  ### 📌 Fix Media Line Order in SDP
  #### Rearrange the order of audio and video media lines in the SDP string to maintain proper transmission order and ensure compatibility with different devices.


# Conclusion ✅✨❄️

#### This guide covers the essential steps to integrate WebRTC in an Android application using an AAR file. The implementation includes setting up a peer connection factory, generating and sending SDP offers, handling remote descriptions, and ensuring proper media line ordering in SDP.
  

