import { useEffect, useState, useRef } from "react";

import "./App.css";
import DroneInfoBox from "./DroneInfoBox";
import LogPanel from './LogPanel';
import DroneIcon from "./DroneIcon"; 
import BatteryStation from "./BatteryStations";
import PickUpStations from "./PickUpStations";
import { BrowserRouter as Router, Route, Routes, Link } from "react-router-dom";
import Drone3DMap from "./Drone3DMap"; 


function App() {
  const [show3D, setShow3D] = useState(false);
  const [show3DMap, setShow3DMap] = useState(false);
  const [simState, setSimState] = useState("running");
  const [drones, setDrones] = useState([]);
  const [statusMessage, setStatusMessage] = useState("");
  const [targets, setTargets] = useState([]);
  const [showBatteryStations, setShowBatteryStations] = useState(false);
  const [showPickUpStations, setShowPickUpStations] = useState(false);
  const [toggleDroneInfo, setToggleDroneInfo] = useState(false);
  const [showPaths, setShowPaths] = useState(false);
  const [paths, setPaths] = useState([]);
  const [dimensions, setDimensions] = useState({
    width: window.innerWidth * 0.8,
    height: window.innerHeight * 0.8,
  });
  const [followingDrone, setFollowingDrone] = useState(null);
  const [cameraOffset, setCameraOffset] = useState({ x: 0, y: 0 });
  const [isFollowMode, setIsFollowMode] = useState(false);
  const containerRef = useRef(null);
  const followTransition = useRef(null);
  
  const [followZoom, setFollowZoom] = useState(2);
  const [showInfoBoxes, setShowInfoBoxes] = useState(true);

  useEffect(() => {
  const handleResize = () => {
    setDimensions({
      width: window.innerWidth * 0.8,
      height: window.innerHeight * 0.8,
    });
  };

  window.addEventListener('resize', handleResize);
    return () => window.removeEventListener('resize', handleResize);
  }, []);


  const CELL_WIDTH = dimensions.width / 203;
  const CELL_HEIGHT = dimensions.height / 203;
  const scaleX = (x) => x * CELL_WIDTH;
  const scaleY = (y) => y * CELL_HEIGHT;
  const ws = useRef(null);
  const reconnectTimeoutRef = useRef(null);
  const maxReconnectAttempts = 5;
  const reconnectAttemptsRef = useRef(0);



  const connectWebSocket = () => {
    try {
      ws.current = new WebSocket("ws://localhost:8080/ws/drones");

      ws.current.onopen = () => {
        console.log("WebSocket connected");
        reconnectAttemptsRef.current = 0; 
        ws.current.send(JSON.stringify({ action: "getDrones" }));
        ws.current.send(JSON.stringify({ action: "getPaths" }));
      };

    ws.current.onmessage = (event) => {
      try {
        const data = JSON.parse(event.data);
        
        if (data.type === 'drones') {
          const dronesArray = Object.values(data.payload);
          setDrones(dronesArray);
          console.log("Drones received:", dronesArray);
        } else if (data.type === 'paths' && data.payload) {
          console.log("Paths received:", data.payload);
          setPaths(data.payload);
        }
      } catch (e) {
        console.warn("Failed to parse message as JSON:", event.data);
      }
    };

    ws.current.onerror = (error) => {
      console.error("WebSocket error:", error);
    };

    ws.current.onclose = (event) => {
      console.log("WebSocket disconnected:", event.code, event.reason);
      
    
      if (event.code !== 1000 && reconnectAttemptsRef.current < maxReconnectAttempts) {
        const delay = Math.min(1000 * Math.pow(2, reconnectAttemptsRef.current), 30000);
        console.log(`Attempting to reconnect in ${delay}ms... (attempt ${reconnectAttemptsRef.current + 1}/${maxReconnectAttempts})`);
        
        reconnectTimeoutRef.current = setTimeout(() => {
          reconnectAttemptsRef.current++;
          connectWebSocket();
        }, delay);
      }
    };

  } catch (error) {
    console.error("Failed to create WebSocket connection:", error);
  }
};

    useEffect(() => {
      connectWebSocket();

      return () => {
        if (reconnectTimeoutRef.current) {
          clearTimeout(reconnectTimeoutRef.current);
        }
        if (ws.current) {
          ws.current.close(1000, "Component unmounting");
        }
      };
    }, []);


    const renderPath = (droneId, nodeList) => {
      const drone = drones.find(d => d.droneid === droneId);
      if (!drone) return null;

      const currentIndex = getCurrentNodeIndex(drone, nodeList);
      const remainingNodes = nodeList.slice(currentIndex);

      if (remainingNodes.length < 2) return null;

      const pathString = remainingNodes.map((node, idx) => {
        const x = scaleX(node.x);
        const y = scaleY(node.y);
        return idx === 0 ? `M ${x} ${y}` : `L ${x} ${y}`;
      }).join(' ');

      return (
        <svg
          key={`path-${droneId}`}
          style={{
            position: "absolute",
            top: 0,
            left: 0,
            width: "100%",
            height: "100%",
            pointerEvents: "none",
            zIndex: 500,
          }}
        >
          <path
            d={pathString}
            stroke="rgba(255, 100, 100, 1)"
            strokeWidth="2"
            fill="none"
            r="3"
          />
        </svg>
      );
    };
    



    function getCurrentNodeIndex(drone, nodeList) {
      let closestIndex = 0;
      let closestDistance = Infinity;

      nodeList.forEach((node, i) => {
        const dx = node.x - drone.x;
        const dy = node.y - drone.y;
        const dist = dx * dx + dy * dy; 

        if (dist < closestDistance) {
          closestDistance = dist;
          closestIndex = i;
        }
      });

      return closestIndex;
    }


    useEffect(() => {
      const interval = setInterval(() => {
        updateDrones();
      }, 150); 

      return () => clearInterval(interval); 
    }, []);



    const updateDrones = () => {
      if (ws.current && ws.current.readyState === WebSocket.OPEN) {
        ws.current.send(JSON.stringify({ action: "getAll" }));
      } else {
        console.warn("WebSocket not connected");
      }
    };

    const updateDrones2 = () => {
      if (ws.current && ws.current.readyState === WebSocket.OPEN) {
        ws.current.send(JSON.stringify({ action: "moveAll" }));
      } else {
        console.warn("WebSocket not connected");
      }
    };

  const toggleBS = () => { console.log("SHOW"); setShowBatteryStations((prev) => !prev); };
  const togglePaths = () => setShowPaths((prev) => !prev);


  const simulateDrones = async () => {


    if (simState === "running") {
        console.log(" running sim state", simState);

      try {
        const response = await fetch(`http://localhost:8080/api/drones/simulate?runSimulation=${simState === "running"}`, {
          method: "POST",
        });


        if (response.ok) {
          setStatusMessage(`sim started`);
          setTimeout(() => setStatusMessage(""), 3000); 
          setSimState("stopped");
        } else {
          setStatusMessage(`Failed to set `);
        }
      } catch (err) {
        console.error("Failed to update destination:", err);
        setStatusMessage("Network error occurred");
      }
    }
    
    else if (simState === "stopped") {

      console.log(" running sim state", simState);

      try {
        const response = await fetch(`http://localhost:8080/api/drones/simulate?runSimulation=${simState === "running"}`, {
          method: "POST",
        });

        if (response.ok) {
          setStatusMessage(`Simulation stopped`);
          setTimeout(() => setStatusMessage(""), 3000); 
          setSimState("running");
        } else {
          setStatusMessage(`Failed to stop simulation`);
        }
      } catch (err) {
        console.error("Failed to stop simulation:", err);
        setStatusMessage("Network error occurred");
      }
    };
  }

  const setDroneDestination = async (droneId) => {
    try {
      const response = await fetch(`http://localhost:8080/api/drones/${droneId}/destination`, {
        method: "POST",
      });

      if (response.ok) {
        setStatusMessage(`Destination added for drone ${droneId}`);
        setTimeout(() => setStatusMessage(""), 3000); 
      } else {
        setStatusMessage(`Failed to set destination for drone ${droneId}`);
      }
    } catch (err) {
      console.error("Failed to update destination:", err);
      setStatusMessage("Network error occurred");
    }
  };



  // prevent us breaking out of map bounds (which messes up maps position in container)
  const applyCameraBounds = (x, y, zoom) => {
    
    const container = containerRef.current.getBoundingClientRect();
    const scaledMapWidth = dimensions.width * zoom;
    const scaledMapHeight = dimensions.height * zoom;
    
    const maxX = Math.max(0, scaledMapWidth - container.width);
    const maxY = Math.max(0, scaledMapHeight - container.height);
    
    const boundedX = Math.max(-maxX, Math.min(0, x));
    const boundedY = Math.max(-maxY, Math.min(0, y));
    
    return { x: boundedX, y: boundedY };
  };

  const calculateCameraTarget = (drone) => {
    if (!containerRef.current || !drone) return { x: 0, y: 0, zoom: 1 };
    
    const container = containerRef.current.getBoundingClientRect();
    const centerX = container.width / 2;
    const centerY = container.height / 2;
    
    const zoom = isFollowMode ? followZoom : 1;
    
    const dronePixelX = drone.x * CELL_WIDTH + CELL_WIDTH / 2;
    const dronePixelY = drone.y * CELL_HEIGHT + CELL_HEIGHT / 2;
    
    const targetX = centerX - dronePixelX * zoom;
    const targetY = centerY - dronePixelY * zoom;
    
  
    const bounded = applyCameraBounds(targetX, targetY, zoom);
    
    return {
      x: bounded.x,
      y: bounded.y,
      zoom: zoom
    };
  };



  const smoothTransition = (targetX, targetY, targetZoom = 1, duration = 800) => {
    if (followTransition.current) {
      clearInterval(followTransition.current);
    }

    const bounded = applyCameraBounds(targetX, targetY, targetZoom);
    const finalTargetX = bounded.x;
    const finalTargetY = bounded.y;

    const startX = cameraOffset.x;
    const startY = cameraOffset.y;
    const startZoom = cameraOffset.zoom || 1;
    const deltaX = finalTargetX - startX;
    const deltaY = finalTargetY - startY;
    const deltaZoom = targetZoom - startZoom;
    const startTime = Date.now();

    followTransition.current = setInterval(() => {
      const elapsed = Date.now() - startTime;
      const progress = Math.min(elapsed / duration, 1);
      
      const easeProgress = 1 - Math.pow(1 - progress, 3);
      
      setCameraOffset({
        x: startX + deltaX * easeProgress,
        y: startY + deltaY * easeProgress,
        zoom: startZoom + deltaZoom * easeProgress
      });

      if (progress >= 1) {
        clearInterval(followTransition.current);
        followTransition.current = null;
      }
    }, 15);
  };

  

  //handles zooming, we pass in type (a small fix for allowing us to tap on a drone to zoom in, but upon second tap we dont zoom out - we leave zoom out to the exit button)
  const handleDroneClick = (drone, type) => {
    if(type == false && followingDrone?.droneid === drone.droneid){
      return;
    }
    if (followingDrone?.droneid === drone.droneid) {
      setFollowingDrone(null);
      setIsFollowMode(false);
      smoothTransition(0, 0, 1); 
    } else {
      setFollowingDrone(drone);
      setIsFollowMode(true);
      const target = calculateCameraTarget(drone);
      smoothTransition(target.x, target.y, target.zoom);
      
      setTimeout(() => {
        const freshTarget = calculateCameraTarget(drone);
        smoothTransition(freshTarget.x, freshTarget.y, freshTarget.zoom);
      }, 20);
    }
  };

  useEffect(() => {
    if (!isFollowMode || !followingDrone) return;
    const currentDrone = drones.find(d => d.droneid === followingDrone.droneid);
    if (currentDrone) {
      setFollowingDrone(currentDrone);
      const target = calculateCameraTarget(currentDrone);
      smoothTransition(target.x, target.y, target.zoom, 800);
    }
  }, [drones, isFollowMode, followingDrone?.droneid]);

  //camera offset and scale to handle zoom efferc 
  const worldStyle = {
    position: 'absolute',
    transform: `translate(${cameraOffset.x}px, ${cameraOffset.y}px) scale(${cameraOffset.zoom || 1})`,
    transition: 'transform 0.3s ease-out',
    transformOrigin: '0 0',
    backgroundSize: `${dimensions.width}px ${dimensions.height}px`,
    width: `${dimensions.width}px`,
    height: `${dimensions.height}px`, 
    backgroundImage: 'url("/images/nm.png")', 
    backgroundPosition: 'center',
    backgroundRepeat: 'no-repeat',
  };

return (
  <div>
    <div className="header">
      <h1 className="app-title"></h1>
    </div>

    <div className="map-toolbar">
      <button className='btn btn--primary' onClick={simulateDrones}>{simState === 'running' ? 'Start Simulation' : 'Stop Simulation'}</button>
      <button className='btn btn--primary' onClick={toggleBS}>Toggle Battery Stations</button>
      <button className='btn btn--primary' onClick={togglePaths}>Toggle Path</button>
      <button className='btn btn--primary' onClick={() => setShowPickUpStations((prev) => !prev)}>Toggle Pick Up Stations</button>
      <button className='btn btn--primary' onClick={() => setToggleDroneInfo((prev) => !prev)}>Toggle Drones Info</button>
      <button className='btn btn--primary' onClick={() => setShow3DMap(prev => !prev)}>
        {show3DMap ? "Show 2D Map" : "Show 3D Map"}
      </button>
      
    </div>

    {isFollowMode && !show3DMap && (
      <div style={{
        position: 'fixed',
        top: '20px',
        right: '20px',
        zIndex: 2000,
        background: 'rgba(0, 0, 0, 0.8)',
        padding: '10px 15px',
        borderRadius: '8px',
        color: 'white',
        fontFamily: 'monospace',
      }}>
        <div>📹 Following: Drone {followingDrone?.droneid}</div>
        <button
          onClick={() => handleDroneClick(followingDrone, true)}
          style={{
            marginTop: '5px',
            padding: '5px 10px',
            background: 'rgba(255, 0, 0, 0.7)',
            border: 'none',
            borderRadius: '4px',
            color: 'white',
            cursor: 'pointer',
          }}
        >
          Exit Follow Mode
        </button>
      </div>
    )}

    <div><LogPanel /></div>

    <div
      ref={containerRef}
      className="map-container"
      style={{
        position: "relative",
        marginTop: "1%",
        width: "80vw",
        height: "80vh",
        border: "7px solid grey",
        overflow: "hidden",
        marginLeft: "1%",

        marginRight: 0,
      }}
    >
      {show3DMap ? (
        <Drone3DMap
          drones={drones}
          showInfoBoxes={showInfoBoxes}
          paths={paths}
          showPaths={showPaths} 
        />
      ) : (
        <>
          <div id="map" style={{ width: "100%", height: "100%" }}></div>

          <div style={worldStyle}>
            <div style={{ display: showBatteryStations ? 'block' : 'none' }}>
              <BatteryStation />
            </div>
            <div style={{ display: showPickUpStations ? 'block' : 'none' }}>
              <PickUpStations />
            </div>

            {drones.map((drone) => (
              <div
                key={drone.droneid}
                onClick={() => handleDroneClick(drone, false)}
                style={{
                  position: 'absolute',
                  left: `${scaleX(drone.x)}px`,
                  top: `${scaleY(drone.y)}px`,
                  cursor: 'pointer',
                  transition: 'left 0.3s linear, top 0.3s linear',
                  filter: followingDrone?.droneid === drone.droneid
                    ? 'drop-shadow(0 0 10px rgba(0, 255, 255, 0.8))'
                    : 'none',
                  zIndex: followingDrone?.droneid === drone.droneid ? 1000 : 900,
                }}
              >
                <DroneIcon
                  drone={drone}
                  setDroneDestination={setDroneDestination}
                  isFollowed={followingDrone?.droneid === drone.droneid}
                />

                <div className={
                  toggleDroneInfo
                    ? 'drone-info-visible'
                    : 'drone-info-hidden'
                }>
                  <DroneInfoBox drone={drone} />
                </div>

              </div>
            ))}

            {showPaths && (
              isFollowMode && followingDrone
                ? (() => {
                    const droneId = followingDrone.droneid;
                    const nodeList = paths[droneId];
                    if (!nodeList) return null;

                    return renderPath(droneId, nodeList);
                  })()
                : (
                  Object.entries(paths).map(([droneId, nodeList]) => {
                    const drone = drones.find(d => d.droneid === droneId);
                    if (!drone || !["Busy", "Pickup", "Queueing"].includes(drone.available)) return null;
                    return renderPath(droneId, nodeList);
                  })
                )
            )}

          </div>
        </>
      )}
    </div>
  </div>
);

}

export default App;
