$baseUrl = "http://127.0.0.1:8082"

Write-Host "--- SYSTEM TEST START ---"

# Step 1: Login
$loginUrl = "$baseUrl/login"
$loginBody = "username=john.doe@example.com&password=customer123"
Write-Host "1. Logging in..."
$loginResult = Invoke-WebRequest -Uri $loginUrl -Method Post -Body $loginBody -ContentType "application/x-www-form-urlencoded" -SessionVariable session -MaximumRedirection 0 -ErrorAction SilentlyContinue
Write-Host "   Login Status: $($loginResult.StatusCode)"

# Step 2: Clear/Get Cart
Write-Host "2. Getting Cart..."
$cart = Invoke-RestMethod -Uri "$baseUrl/api/cart" -Method Get -WebSession $session
Write-Host "   Cart Items: $($cart.items.Count)"

# Step 3: Add Item
Write-Host "3. Adding Item to Cart..."
$addBody = @{
    variantId = 1
    quantity = 1
    lensOptionId = 1
} | ConvertTo-Json
$addResult = Invoke-RestMethod -Uri "$baseUrl/api/cart/add" -Method Post -Body $addBody -ContentType "application/json" -WebSession $session
Write-Host "   Added. New Cart total: $($addResult.totalPrice)"

# Step 4: Checkout
Write-Host "4. Checking out..."
$checkoutBody = @{
    shippingAddressId = 1
    billingAddressId = 1
    paymentMethod = "COD"
    items = @()
} | ConvertTo-Json
$checkoutResult = Invoke-RestMethod -Uri "$baseUrl/api/orders/checkout" -Method Post -Body $checkoutBody -ContentType "application/json" -WebSession $session
Write-Host "   Checkout result: $($checkoutResult.message)"
$newOrderId = $checkoutResult.data.orderId
Write-Host "   Order ID: $newOrderId"

# Step 5: Verify Snapshot
Write-Host "5. Verifying Snapshot..."
$orderDetails = Invoke-RestMethod -Uri "$baseUrl/api/orders/$newOrderId" -Method Get -WebSession $session
$item = $orderDetails.data.orderItems[0]
Write-Host "   Order Item Details:"
Write-Host "     Product: $($item.productName)"
Write-Host "     Lens Type: $($item.lensType)"
Write-Host "     Lens Price: $($item.lensPrice)"
Write-Host "     Lens Coating: $($item.lensCoating)"

if ($null -ne $item.lensType -and $null -ne $item.lensPrice) {
    Write-Host "   SUCCESS: Snapshot is valid."
} else {
    Write-Host "   FAILED: Snapshot missing lens info."
}

Write-Host "--- SYSTEM TEST COMPLETE ---"
