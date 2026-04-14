$baseUrl = "http://127.0.0.1:8082"
$cookieJar = New-Object System.Net.CookieContainer

function Invoke-Login {
    $loginUrl = "$baseUrl/login"
    $body = "username=john.doe@example.com&password=customer123"
    $response = Invoke-WebRequest -Uri $loginUrl -Method Post -Body $body -ContentType "application/x-www-form-urlencoded" -SessionVariable session -MaximumRedirection 0 -ErrorAction SilentlyContinue
    # Spring Security typically returns a 302 redirect on success
    Write-Host "Login Response Status: $($response.StatusCode)"
    if ($response.Headers.Location) {
        Write-Host "Redirected To: $($response.Headers.Location)"
    }
    return $session
}

function Get-Cart($session) {
    $url = "$baseUrl/api/cart"
    $response = Invoke-RestMethod -Uri $url -Method Get -WebSession $session
    Write-Host "Cart Items Count: $($response.items.Count)"
    return $response
}

function Add-To-Cart($session, $variantId, $qty, $lensId) {
    $url = "$baseUrl/api/cart/add"
    $body = @{
        variantId = $variantId
        quantity = $qty
        lensOptionId = $lensId
    } | ConvertTo-Json
    $response = Invoke-RestMethod -Uri $url -Method Post -Body $body -ContentType "application/json" -WebSession $session
    Write-Host "Added to cart. New total items: $($response.totalItems)"
    return $response
}

function Checkout($session) {
    $url = "$baseUrl/api/orders/checkout"
    $body = @{
        shippingAddressId = 1
        billingAddressId = 1
        paymentMethod = "COD"
        items = @(
            @{
                variantId = 1
                quantity = 1
                lensOptionId = 1
                prescription = @{
                    sphLeft = -1.5
                    sphRight = -1.5
                    pd = 63.0
                    doctorName = "Dr. Quick"
                }
            }
        )
    } | ConvertTo-Json
    $response = Invoke-RestMethod -Uri $url -Method Post -Body $body -ContentType "application/json" -WebSession $session
    Write-Host "Checkout Success! Order ID: $($response.data.orderId)"
    return $response.data
}

function Get-Order-Details($session, $orderId) {
    $url = "$baseUrl/api/orders/$orderId"
    $response = Invoke-RestMethod -Uri $url -Method Get -WebSession $session
    return $response.data
}

try {
    Write-Host "Starting System Test..."
    Write-Host "Step 1: Logging in..."
    $sess = Invoke-Login
    Write-Host "Login function finished."
    
    Write-Host "`nStep 2: Adding item to cart..."
    Add-To-Cart $sess 1 1 1
    Write-Host "Add to cart function finished."
    
    Write-Host "`nStep 3: Checking out..."
    $order = Checkout $sess
    Write-Host "Checkout function finished."
    
    Write-Host "`nStep 4: Verifying Snapshots..."
    $details = Get-Order-Details $sess $order.orderId
    $item = $details.orderItems[0]
    
    Write-Host "Order Item Details (Snapshot):"
    Write-Host "  Product Name: $($item.productName)"
    Write-Host "  Variant Color: $($item.variantColor)"
    Write-Host "  Lens Type: $($item.lensType)"
    Write-Host "  Lens Price: $($item.lensPrice)"
    Write-Host "  Prescription Doctor: $($item.prescription.doctorName)"
    
    if ($item.productName -ne $null -and $item.lensType -ne $null) {
        Write-Host "`nTEST PASSED: Snapshots were correctly populated."
    } else {
        Write-Host "`nTEST FAILED: Snapshots are missing data."
    }
} catch {
    Write-Host "ERROR OCCURRED: $($_.Exception.Message)"
    Write-Error $_
}
