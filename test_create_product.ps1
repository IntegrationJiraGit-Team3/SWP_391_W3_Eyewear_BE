$json = @{
    name = "Test Glasses"
    brand = "Test Brand"
    description = "New test glasses"
    productType = "FRAME"
    isPrescriptionSupported = $true
} | ConvertTo-Json

Invoke-RestMethod -Uri "http://localhost:8080/api/products" -Method Post -Body $json -ContentType "application/json"
