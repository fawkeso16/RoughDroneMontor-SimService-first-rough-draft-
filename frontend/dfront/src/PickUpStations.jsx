import { useEffect, useState } from "react";

function PickUpStations( show) {
  const [pickUpStations, setPickUpStations] = useState([]);

  const MAP_Width = window.innerWidth * 0.8;
  const MAP_Height = window.innerHeight * 0.8;

  const CELL_WIDTH = MAP_Width / 203;
  const CELL_HEIGHT = MAP_Height / 203;

  useEffect(() => {
    const getPickUpStations = async () => {
      try {
        const response = await fetch("http://localhost:8080/api/PickUpStations");
        if (response.ok) {
          const data = await response.json();
          if (data.length === 0) {
            console.log("No stations fetched");
          } else {
            setPickUpStations(data);
          }
        } else {
          console.warn("Failed to fetch logs:", response.statusText);
        }
      } catch (err) {
        console.error("Failed to fetch stations:", err);
      }
    };

    getPickUpStations();
  }, []);

    if (!show) return null; 


  return pickUpStations.map((station) => (
    <div
      key={station.id}
      className="pick-up-station"
      style={{
        position: "absolute",
        left: station.x * CELL_WIDTH,
        top: station.y * CELL_HEIGHT,
        width: 25,
        height: 25,
        backgroundColor: "red",
        borderRadius: "50%",
        zIndex: 100,
      }}
    />
  ));
}


export default PickUpStations;
