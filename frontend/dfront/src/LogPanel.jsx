
import { useEffect, useState, useRef } from "react";
function LogPanel() {
   const [messages, setMessages] = useState([
  ]);



  const latestMessageLength = useRef(0);

  useEffect(() => {
    getLogInfo();
    const interval = setInterval(() => {
        getLogInfo();
      }, 2000);

      return () => clearInterval(interval);
    }, []);

    const getLogInfo = async () => {
    try {
      const response = await fetch(`http://localhost:8080/api/drones/allLogs`, {
        method: "get",
      });

      if (response.ok) {
        const responseData = await response.json();


        if (responseData.length ===0) {
          console.log("No new logs to update.");
          return;
        }

        if(responseData.length >latestMessageLength.current) {
          console.log("New logs detected, updating messages." + JSON.stringify(responseData));
        }

        latestMessageLength.current = responseData.length;

       
        setMessages(responseData);
      } else {
        console.warn("Failed to fetch logs:", response.statusText);
      }
    } catch (err) {
      console.error("Failed to update destination:", err);
    }
  };

  return (
    <div style={{
      marginRight: "1%",
      width: "15vw",
      height: "79vh",
      padding: 10,
      borderRadius: 8,
      textAlign: "center",
      right: 0,
      position: "absolute",
      top: "10%",
      display: "flex",
      flexDirection: "column",
      gap: 8,
      overflow: "none",

      background: "rgba(119, 104, 104, 0.1)",       
      backdropFilter: "blur(10px)",                 
      WebkitBackdropFilter: "blur(10px)",          
      boxShadow: "0 4px 30px rgba(25, 23, 126, 0.29)",   
      border: "1px solid rgba(255, 255, 255, 0.3)", 
    }}>
      <h3>Log</h3>
      <div
        style={{
          height: "90%",
          overflowY: "auto",
          position: "absolute",
          textAlign: "left",
          marginLeft: "5%",
          display: "flex",
          left: 0,
          color: "white",
          width: "90%",
          bottom: 0,
          flexDirection: "column",
          gap: 0,
        }}
        className="hide-scroll"

      >
       {!messages ? (
          <div style={{ padding: 4, fontStyle: "italic", color: "#888" }}>
            No messages to display.
          </div>
        ) : (
        [...messages].reverse().map((msg, i) => {
            const parts = msg.split(",");
            return (
              <div
                key={i}
                style={{
                  display: "flex",
                  flexDirection: "column",
                  fontSize: "0.8rem",
                  width: "100%",            
                  maxWidth: "100%",         
                  height: "auto",           
                  minHeight: "5vh",
                  padding: 4,
                  borderRadius: "4%",
                  overflow: "auto",       
                  whiteSpace: "normal",   
                  wordWrap: "break-word",
                }}
                className="hide-scroll"
              >
                {[parts[0], parts[2]].filter(Boolean).join(" - ")}
              </div>
            );
          })


        )}

      </div>
    </div>
  );
}
export default LogPanel;
