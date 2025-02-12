package com.example.poc_google_webrtc.utils

import org.webrtc.DataChannel
import org.webrtc.IceCandidate
import org.webrtc.MediaStream
import org.webrtc.PeerConnection

open class CustomPeerConnectionObserver: PeerConnection.Observer {
    override fun onSignalingChange(signalingState: PeerConnection.SignalingState?) {
        signalingState?.let {
            when (it) {
                PeerConnection.SignalingState.STABLE -> {
                    println("Information of onSignalingChange STABLE")
                    // The connection is stable (e.g., offer/answer process is complete)
                }

                PeerConnection.SignalingState.HAVE_LOCAL_OFFER -> {
                    println("Information of onSignalingChange HAVE_LOCAL_OFFER")
                    // Local offer has been created, waiting for remote answer
                }

                PeerConnection.SignalingState.HAVE_REMOTE_OFFER -> {
                    println("Information of onSignalingChange HAVE_REMOTE_OFFER")
                    // Remote offer received, waiting for local answer
                }

                PeerConnection.SignalingState.HAVE_LOCAL_PRANSWER -> {
                    println("Information of onSignalingChange HAVE_LOCAL_PRANSWER")
                    // Local PRANSWER has been created (e.g., partial answer during negotiation)
                }

                PeerConnection.SignalingState.HAVE_REMOTE_PRANSWER -> {
                    println("Information of onSignalingChange HAVE_REMOTE_PRANSWER")
                    // Remote PRANSWER received, waiting for local PRANSWER
                }

                PeerConnection.SignalingState.CLOSED -> {
                    println("Information of onSignalingChange Close")
                    // Connection is closed
                }

                else -> {
                    println("Information of onSignalingChange else")
                    // Handle other signaling states
                }
            }
        }
    }

    override fun onIceConnectionChange(newState: PeerConnection.IceConnectionState?) {
        newState?.let {
            when (it) {
                PeerConnection.IceConnectionState.NEW -> {
                    println("Information of onIceConnectionChange new ")
                    // ICE connection is being established
                }
                PeerConnection.IceConnectionState.CHECKING -> {
                    println("Information of onIceConnectionChange CHECKING")
                    // ICE connection is checking
                }
                PeerConnection.IceConnectionState.CONNECTED -> {
                      println("Information of onIceConnectionChange CONNECTED")
                    // ICE connection is successfully established
                }
                PeerConnection.IceConnectionState.FAILED -> {
                      println("Information of onIceConnectionChange FAILED")
                    // ICE connection failed
                }
                PeerConnection.IceConnectionState.DISCONNECTED -> {
                      println("Information of onIceConnectionChange DISCONNECTED")
                    // ICE connection disconnected
                }
                PeerConnection.IceConnectionState.CLOSED -> {
                      println("Information of onIceConnectionChange close")
                    // ICE connection closed
                }

                PeerConnection.IceConnectionState.COMPLETED -> {
                      println("Information of onIceConnectionChange completed")
                }
            }
        }
    }

    override fun onIceConnectionReceivingChange(receiving: Boolean) {
        // Handle when ICE connection is receiving candidates or not
        if (receiving) {
            // Receiving ICE candidates
            println("Information of onIceConnectionReceivingChange receiving")
        } else {
            // Not receiving ICE candidates anymore
            println("Information of onIceConnectionChange not receiving")
        }
    }

    override fun onIceGatheringChange(newState: PeerConnection.IceGatheringState?) {
        newState?.let {
            when (it) {
                PeerConnection.IceGatheringState.NEW -> {
                    // Start gathering ICE candidates
                    println("Information of onIceGatheringChange new")
                }
                PeerConnection.IceGatheringState.GATHERING -> {
                    // ICE candidates are being gathered
                    println("Information of onIceGatheringChange gathering")
                }
                PeerConnection.IceGatheringState.COMPLETE -> {
                    // ICE gathering is complete
                    println("Information of onIceGatheringChange complete")
                }
            }
        }
    }

    override fun onIceCandidate(candidate: IceCandidate?) {
        // New ICE candidate discovered
        candidate?.let {
            // Process the new candidate (send it to the other peer)
           // peerConnection.addIceCandidate(it)
            println("Information of onIceCandidate ")
        }
    }

    override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>?) {
        candidates?.forEach {
            // You may want to process removal of candidates if necessary
            println("Information of onIceCandidatesRemoved: $candidates ")
        }
    }

    override fun onAddStream(stream: MediaStream?) {
        stream?.let {
            // Process the incoming stream, for example by adding it to the renderer
            val videoTrack = it.videoTracks[0]
            println("Information of video track: $videoTrack")
             // val videoRenderer = RTCVideoRenderer()
            //videoTrack.addSink(videoRenderer)
        }
    }

    override fun onRemoveStream(stream: MediaStream?) {
        // Handle the removal of a media stream from the remote peer
        stream?.let {
            // Process the removal of the stream, like stopping the renderer
        }
    }

    override fun onDataChannel(dataChannel: DataChannel?) {
        // Handle the creation of a data channel
        dataChannel?.let {
            // Process the data channel if needed
        }
    }

    override fun onRenegotiationNeeded() {
        TODO("Not yet implemented")
    }
}