$baseUrl = "http://127.0.0.1:8081"

function Invoke-Login($user, $pass) {
    $loginUrl = "$baseUrl/login"
    $body = "username=$user&password=$pass"
    Write-Host "Logging in as $user..."
    $response = Invoke-WebRequest -Uri $loginUrl -Method Post -Body $body -ContentType "application/x-www-form-urlencoded" -SessionVariable session -MaximumRedirection 0 -ErrorAction SilentlyContinue
    Write-Host "Login Status: $($response.StatusCode)"
    return $session
}

function Get-All-Orders($session) {
    $url = "$baseUrl/api/orders"
    Write-Host "Retrieving all orders..."
    try {
        $response = Invoke-RestMethod -Uri $url -Method Get -WebSession $session
        return $response.data
    } catch {
        Write-Host "Failed to get orders: $($_.Exception.Message)"
        return $null
    }
}

function Test-RBAC($session, $targetUrl) {
    Write-Host "Testing RBAC access to $targetUrl..."
    try {
        $response = Invoke-WebRequest -Uri "$baseUrl$targetUrl" -Method Get -WebSession $session
        Write-Host "  Success! Status: $($response.StatusCode)"
        return $response.StatusCode
    } catch {
        Write-Host "  Access Denied / Error: $($_.Exception.Response.StatusCode)"
        return $_.Exception.Response.StatusCode
    }
}

try {
    Write-Host "--- TEST 1: ADMIN ACCESS & SNAPSHOT VERIFICATION ---"
    $adminSession = Invoke-Login "admin@example.com" "admin123"
    $orders = Get-All-Orders $adminSession
    
    if ($orders -ne $null -and $orders.Count -gt 0) {
        Write-Host "Found $($orders.Count) orders."
        $firstItem = $orders[0].orderItems[0]
        Write-Host "Checking snapshot fields for Order ID $($orders[0].orderId):"
        Write-Host "  Product: $($firstItem.productName)"
        Write-Host "  Price: $($firstItem.unitPrice)"
        Write-Host "  Lens Type: $($firstItem.lensType)"
        Write-Host "  Lens Price: $($firstItem.lensPrice)"
        
        if ($null -ne $firstItem.productName -and $null -ne $firstItem.lensType) {
            Write-Host "PASSED: Snapshot fields are present in seeded data."
        } else {
            Write-Host "WARNING: Seeded snapshot fields might be missing. Checking DataInitialize.java is recommended."
        }
    } else {
        Write-Host "No orders found for admin."
    }

    Write-Host "`n--- TEST 2: RBAC (CUSTOMER ACCESSING ADMIN API) ---"
    $custSession = Invoke-Login "john.doe@example.com" "customer123"
    # Customers shouldn't be able to see ALL orders
    $status = Test-RBAC $custSession "/api/orders"
    if ($status -eq 403 -or $status -eq 401) {
        Write-Host "PASSED: Customer was denied access to all orders."
    } else {
        Write-Host "FAILED: Customer could access admin API! Status: $status"
    }

    Write-Host "`n--- TEST 3: CUSTOMER ORDER CREATION & CALCULATIONS ---"
    # Mocking a checkout flow
    $checkoutBody = @{
        shippingAddressId = 1
        billingAddressId = 1
        paymentMethod = "COD"
        items = @() # In the real code, it pulls from cart but uses this list for prescriptions
    } | ConvertTo-Json

    Write-Host "Attempting checkout for customer..."
    # Note: createOrderFromCart in OrderService.java:74 uses the user's cart
    # We must add to cart first.
    Invoke-RestMethod -Uri "$baseUrl/api/cart/add" -Method Post -Body (@{ variantId = 1; quantity = 2 } | ConvertTo-Json) -ContentType "application/json" -WebSession $custSession
    
    Write-Host "Cart populated. Proceeding to checkout..."
    $checkoutResponse = Invoke-RestMethod -Uri "$baseUrl/api/orders/checkout" -Method Post -Body $checkoutBody -ContentType "application/json" -WebSession $custSession
    Write-Host "Checkout Response: $($checkoutResponse.message)"
    if ($checkoutResponse.success) {
        $newOrder = $checkoutResponse.data
        Write-Host "  Order ID: $($newOrder.orderId)"
        Write-Host "  Total Price: $($newOrder.totalPrice)"
        Write-Host "  Item Count: $($newOrder.totalItems)"
        
        # Verify Snapshots in new order
        $newOrderItem = $newOrder.orderItems[0]
        Write-Host "  Verifying snapshot in new order: Product=$($newOrderItem.productName), Price=$($newOrderItem.unitPrice)"
        if ($newOrderItem.productName -eq "Classic Aviator") {
            Write-Host "PASSED: New order snapshot correctly captured product name."
        } else {
            Write-Host "FAILED: New order snapshot missing or incorrect."
        }
    } else {
        Write-Host "FAILED: Checkout failed - $($checkoutResponse.message)"
    }

} catch {
    Write-Error $_
}
