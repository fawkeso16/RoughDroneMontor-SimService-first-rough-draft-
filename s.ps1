# Start backend Spring Boot app in a new PowerShell window
Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd backend\drone-prototype; .\mvnw.cmd spring-boot:run"

# Optional: wait a few seconds to let backend warm up
Start-Sleep -Seconds 5

# Start frontend React app in another PowerShell window
Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd frontend\dfront; npm start"
