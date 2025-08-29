function DroneInfoBox({ drone }) {
  return (
    <div
      className="drone-info-box"
      style={{
        marginTop: 4,
        background: "darkgrey",
        border: "3px solid #ccc",
        color: "white",
        padding: "4px",
        width: "4vw",
        justifyContent: "center",
        alignItems: "center",
        display: "flex",
        fontSize: "0.6rem",
        borderRadius: "25px",
        flexDirection: "column",
        textAlign: "center",
        whiteSpace: "nowrap",
        zIndex: "1000",
        position: "absolute",
      }}
    >
      <div>ID: {drone.droneid}</div>
      <div>Battery: {Math.round(drone.battery)}%</div>
      <div>Position: ({Math.round(drone.x)}, {Math.round(drone.y)})</div>
      <div>
        Availability: {drone.available ? drone.available : "Available"}
      </div>
    </div>
  );
}

export default DroneInfoBox;