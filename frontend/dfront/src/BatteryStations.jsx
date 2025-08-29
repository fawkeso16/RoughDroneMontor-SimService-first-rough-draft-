import { useEffect, useState } from "react";

function BatteryStation( show) {
  const [batteries, setBatteries] = useState([]);

  const MAP_Width = window.innerWidth * 0.8;
  const MAP_Height = window.innerHeight * 0.8;

  const CELL_WIDTH = MAP_Width / 203;
  const CELL_HEIGHT = MAP_Height / 203;

  useEffect(() => {
    const getBatteryStations = async () => {
      try {
        const response = await fetch("http://localhost:8080/api/batteryStations");
        if (response.ok) {
          const data = await response.json();
          if (data.length === 0) {
            console.log("No stations fetched");
          } else {
            setBatteries(data);
          }
        } else {
          console.warn("Failed to fetch logs:", response.statusText);
        }
      } catch (err) {
        console.error("Failed to fetch stations:", err);
      }
    };

    getBatteryStations();
  }, []);

    if (!show) return null; 


  return batteries.map((station) => (
    <div
      key={station.id}
      className="battery-station"
      style={{
        position: "absolute",
        left: station.x * CELL_WIDTH,
        top: station.y * CELL_HEIGHT,
        backgroundColor: "lightgreen",
        width: "25px",
        height: "25px",
        borderRadius: "50%",
        zIndex: 100,
      }}
      // >
      //    <img
      //     src="./images/BS.svg"
      //     color="black"
      //     alt="Battery Station"
      //     style={{ width: "100%", height: "100%" }}
      //   />
      />
      
  
    
  ));
}


export default BatteryStation;
