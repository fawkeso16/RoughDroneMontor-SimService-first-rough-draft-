import { useEffect, useState, useRef } from "react";
import React from "react";

import DroneInfoBox from "./DroneInfoBox";

const DroneIcon = React.memo(({ drone, setDroneDestination, isFollowed}) => {
  const [hovered, setHovered] = useState(false);

  return (
    <div
      className="drone-icon"
      onMouseEnter={() => setHovered(true)}
      onMouseLeave={() => setHovered(false)}
      style={{
        position: "relative", 
      }}
    >
      <div
        title={`${drone.droneid} - Battery: ${Math.round(drone.battery)}%`}
        style={{
          width: 24,
          height: 24,
          borderRadius: "50%",
          cursor: "pointer",
          zIndex:"888",
        }}
        onClick={() => setDroneDestination(drone.droneid)}
      >
        <img
          src="./images/drone.svg"
          alt="Drone"
          style={{ width: "100%", height: "100%" }}
        />
      </div>

      <div className={
          (hovered || isFollowed) 
            ? 'drone-info-visible' 
            : 'drone-info-hidden'
        }>
          <DroneInfoBox drone={drone} />
        </div>

    </div>
  );
}, (prevProps, nextProps) => {
  return (
    prevProps.drone === nextProps.drone &&
    prevProps.isFollowed === nextProps.isFollowed 
  );
});

export default DroneIcon;