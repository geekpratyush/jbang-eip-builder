Order Confirmation: ${body.orderId}
Customer: ${body.customer.name} (${body.customer.email})

Items Ordered:
<#list body.items as item>
- ${item.name}: $${item.price}
</#list>
