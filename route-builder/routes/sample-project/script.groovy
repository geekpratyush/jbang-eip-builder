// Sample Groovy script to manipulate headers and body
def body = request.body
def accountId = request.headers.get('AccountId')

// Add some business logic
if (accountId != null && accountId.startsWith("VIP")) {
    request.headers.put('Priority', 'HIGH')
} else {
    request.headers.put('Priority', 'NORMAL')
}

// Return modified payload
return "PROCESSED: " + body + " | Priority: " + request.headers.get('Priority')
