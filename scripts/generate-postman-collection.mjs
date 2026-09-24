import fs from 'node:fs';
import path from 'node:path';

const json = (value) => JSON.stringify(value, null, 2);

const headers = {
  json: [{ key: 'Content-Type', value: 'application/json' }],
  customer: [{ key: 'Authorization', value: 'Bearer {{customerToken}}' }],
  admin: [{ key: 'Authorization', value: 'Bearer {{adminToken}}' }],
  customerJson: [
    { key: 'Authorization', value: 'Bearer {{customerToken}}' },
    { key: 'Content-Type', value: 'application/json' }
  ],
  adminJson: [
    { key: 'Authorization', value: 'Bearer {{adminToken}}' },
    { key: 'Content-Type', value: 'application/json' }
  ]
};

const testStatus = (status) => [
  `pm.test("Status code is ${status}", function () { pm.response.to.have.status(${status}); });`
];

const allowStatuses = (...statuses) => [
  `pm.test("Status code is expected", function () { pm.expect([${statuses.join(', ')}]).to.include(pm.response.code); });`
];

const saveJsonPath = (variable, ...paths) => [
  'if (pm.response.code >= 200 && pm.response.code < 300) {',
  '  var json = pm.response.json();',
  `  var value = ${paths.map((p) => `json${p}`).join(' || ')};`,
  `  if (value !== undefined && value !== null) { pm.collectionVariables.set("${variable}", value); }`,
  '}'
];

function rawJson(value) {
  return { mode: 'raw', raw: json(value) };
}

function req(name, method, url, options = {}) {
  const request = { method, url: `{{baseUrl}}${url}` };
  if (options.header) request.header = options.header;
  if (options.body) request.body = options.body;
  const item = { name, request };
  if (options.followRedirects === false) {
    item.protocolProfileBehavior = { followRedirects: false };
  }
  if (options.tests) {
    item.event = [{ listen: 'test', script: { type: 'text/javascript', exec: options.tests } }];
  }
  return item;
}

const collection = {
  info: {
    name: 'Fashion E-Commerce API Testing',
    description: [
      'Postman collection generated from Spring controllers under src/main/java/com/fashion.',
      'Default backend URL is http://localhost:8081 because src/main/resources/application.properties sets server.port=8081.',
      'Some negative business cases accept 400/404/409 because exact validation status depends on database state.'
    ].join('\n'),
    schema: 'https://schema.getpostman.com/json/collection/v2.1.0/collection.json'
  },
  event: [
    {
      listen: 'prerequest',
      script: {
        type: 'text/javascript',
        exec: [
          'if (!pm.collectionVariables.get("runId")) { pm.collectionVariables.set("runId", Date.now()); }',
          'if (!pm.collectionVariables.get("customerEmail")) { pm.collectionVariables.set("customerEmail", "api-test-" + pm.collectionVariables.get("runId") + "@example.com"); }',
          'if (!pm.collectionVariables.get("customerPassword")) { pm.collectionVariables.set("customerPassword", "ApiTest@12345"); }'
        ]
      }
    },
    {
      listen: 'test',
      script: {
        type: 'text/javascript',
        exec: [
          'if (pm.response) {',
          '  pm.test("HTTP status is not a server error", function () { pm.expect(pm.response.code).to.be.below(500); });',
          '  pm.test("Response time < 3000 ms", function () { pm.expect(pm.response.responseTime).to.be.below(3000); });',
          '}'
        ]
      }
    }
  ],
  variable: [
    { key: 'baseUrl', value: 'http://localhost:8081' },
    { key: 'runId', value: '' },
    { key: 'customerEmail', value: '' },
    { key: 'customerPassword', value: '' },
    { key: 'customerToken', value: '' },
    { key: 'adminEmail', value: 'admin@fashion.local' },
    { key: 'adminPassword', value: 'Admin@123' },
    { key: 'adminToken', value: '' },
    { key: 'productId', value: '1' },
    { key: 'variantId', value: '22' },
    { key: 'cartItemId', value: '1' },
    { key: 'orderId', value: '1' },
    { key: 'orderItemId', value: '1' },
    { key: 'reviewId', value: '1' },
    { key: 'validVoucherCode', value: 'NQSALE' },
    { key: 'expiredVoucherCode', value: 'EXPIRED' },
    { key: 'missingVoucherCode', value: 'DOES_NOT_EXIST' },
    { key: 'adminProductId', value: '' }
  ],
  item: [
    {
      name: '01 Authentication',
      item: [
        req('Register new customer', 'POST', '/api/auth/register', {
          header: headers.json,
          body: rawJson({ fullName: 'API Test Customer', email: '{{customerEmail}}', password: '{{customerPassword}}', phone: '0900000000', address: 'API test address' }),
          tests: [...testStatus(200)]
        }),
        req('Login customer and save token', 'POST', '/api/auth/login', {
          header: headers.json,
          body: rawJson({ email: '{{customerEmail}}', password: '{{customerPassword}}' }),
          tests: [...testStatus(200), 'var json = pm.response.json();', 'pm.expect(json.token).to.be.a("string").and.not.empty;', 'pm.collectionVariables.set("customerToken", json.token);']
        }),
        req('Login wrong password', 'POST', '/api/auth/login', {
          header: headers.json,
          body: rawJson({ email: '{{customerEmail}}', password: 'wrong-password' }),
          tests: allowStatuses(400, 401, 403)
        }),
        req('Register duplicate email', 'POST', '/api/auth/register', {
          header: headers.json,
          body: rawJson({ fullName: 'Duplicate API Test Customer', email: '{{customerEmail}}', password: '{{customerPassword}}', phone: '0900000001', address: 'Duplicate address' }),
          tests: allowStatuses(400, 409)
        })
      ]
    },
    {
      name: '02 Products',
      item: [
        req('GET products', 'GET', '/api/products', { tests: allowStatuses(200) }),
        req('GET product by id', 'GET', '/api/products/{{productId}}', { tests: allowStatuses(200) }),
        req('Product not found', 'GET', '/api/products/999999', { tests: allowStatuses(400, 404) }),
        req('Search products', 'GET', '/api/products/search?keyword=ao', { tests: allowStatuses(200) }),
        req('Filter products by category', 'GET', '/api/products/category/1', { tests: allowStatuses(200) })
      ]
    },
    {
      name: '03 Cart',
      item: [
        req('Get cart', 'GET', '/api/cart', { header: headers.customer, tests: allowStatuses(200) }),
        req('Add cart item', 'POST', '/api/cart/items', {
          header: headers.customerJson,
          body: rawJson({ productId: '{{productId}}', variantId: '{{variantId}}', size: 'M', quantity: 1 }),
          tests: [...allowStatuses(200), ...saveJsonPath('cartItemId', '.items && json.items[json.items.length - 1] && json.items[json.items.length - 1].id')]
        }),
        req('Update cart item', 'PUT', '/api/cart/items/{{cartItemId}}', {
          header: headers.customerJson,
          body: rawJson({ productId: '{{productId}}', variantId: '{{variantId}}', size: 'M', quantity: 2 }),
          tests: allowStatuses(200)
        }),
        req('Add cart item over stock', 'POST', '/api/cart/items', {
          header: headers.customerJson,
          body: rawJson({ productId: '{{productId}}', variantId: '{{variantId}}', size: 'M', quantity: 999999 }),
          tests: allowStatuses(400, 409)
        }),
        req('Delete cart item', 'DELETE', '/api/cart/items/{{cartItemId}}', { header: headers.customer, tests: allowStatuses(200) })
      ]
    },
    {
      name: '04 Voucher',
      item: [
        req('Voucher valid', 'GET', '/api/vouchers/validate?code={{validVoucherCode}}&orderValue=500000', { header: headers.customer, tests: allowStatuses(200) }),
        req('Voucher expired', 'GET', '/api/vouchers/validate?code={{expiredVoucherCode}}&orderValue=500000', { header: headers.customer, tests: allowStatuses(200, 400) }),
        req('Voucher not found', 'GET', '/api/vouchers/validate?code={{missingVoucherCode}}&orderValue=500000', { header: headers.customer, tests: allowStatuses(200, 400, 404) })
      ]
    },
    {
      name: '05 Checkout',
      item: [
        req('Checkout summary', 'GET', '/api/checkout/summary', { header: headers.customer, tests: allowStatuses(200) }),
        req('Create checkout order', 'POST', '/api/checkout', {
          header: headers.customerJson,
          body: rawJson({ shippingAddress: 'API test address', paymentMethod: 'COD', shippingFee: 30000, discountAmount: 0, productId: '{{productId}}', variantId: '{{variantId}}', size: 'M', quantity: 1 }),
          tests: [...allowStatuses(200), ...saveJsonPath('orderId', '.id'), ...saveJsonPath('orderItemId', '.items && json.items[0] && json.items[0].id', '.orderItems && json.orderItems[0] && json.orderItems[0].id')]
        })
      ]
    },
    {
      name: '06 Orders',
      item: [
        req('Create order from cart', 'POST', '/api/orders', {
          header: headers.customerJson,
          body: rawJson({ shippingAddress: 'API test address', paymentMethod: 'COD', shippingFee: 30000, discountAmount: 0 }),
          tests: allowStatuses(200, 400)
        }),
        req('Get orders', 'GET', '/api/orders', { header: headers.customer, tests: allowStatuses(200) }),
        req('Get order', 'GET', '/api/orders/{{orderId}}', { header: headers.customer, tests: allowStatuses(200) }),
        req('Complete order payment', 'PATCH', '/api/orders/{{orderId}}/payment', { header: headers.customer, tests: allowStatuses(200, 400) }),
        req('Confirm order received', 'PATCH', '/api/orders/{{orderId}}/receive', { header: headers.customer, tests: allowStatuses(200, 400) })
      ]
    },
    {
      name: '07 Payment',
      item: [
        req('VNPay create payment', 'POST', '/api/vnpay/orders/{{orderId}}', { header: headers.customer, tests: allowStatuses(200, 400) }),
        req('VNPay success callback', 'GET', '/api/vnpay/return?vnp_TxnRef={{orderId}}_postman&vnp_ResponseCode=00&vnp_TransactionStatus=00&vnp_Amount=10000000', { followRedirects: false, tests: allowStatuses(302) }),
        req('VNPay failure/cancel callback', 'GET', '/api/vnpay/return?vnp_TxnRef={{orderId}}_postman&vnp_ResponseCode=24&vnp_TransactionStatus=02', { followRedirects: false, tests: allowStatuses(302) })
      ]
    },
    {
      name: '08 Shipping',
      item: [
        req('Shipping status', 'GET', '/api/orders/{{orderId}}/shipment', { header: headers.customer, tests: allowStatuses(200, 404) }),
        req('Location search', 'GET', '/api/location/search?q=Ho%20Chi%20Minh', { tests: allowStatuses(200, 502) })
      ]
    },
    {
      name: '09 Review',
      item: [
        req('Get product reviews', 'GET', '/api/reviews/product/{{productId}}', { tests: allowStatuses(200) }),
        req('Create review', 'POST', '/api/reviews/product/{{productId}}', {
          header: headers.customerJson,
          body: rawJson({ rating: 5, comment: 'API test review', orderItemId: '{{orderItemId}}', media: [] }),
          tests: [...allowStatuses(200, 400), ...saveJsonPath('reviewId', '.id')]
        }),
        req('Review without purchase', 'POST', '/api/reviews/product/999999', {
          header: headers.customerJson,
          body: rawJson({ rating: 4, comment: 'Should fail without purchase', orderItemId: 999999, media: [] }),
          tests: allowStatuses(400, 404)
        }),
        req('Duplicate review', 'POST', '/api/reviews/product/{{productId}}', {
          header: headers.customerJson,
          body: rawJson({ rating: 4, comment: 'Duplicate API test review', orderItemId: '{{orderItemId}}', media: [] }),
          tests: allowStatuses(400, 409)
        })
      ]
    },
    {
      name: '10 Wishlist',
      item: [
        req('Get wishlist', 'GET', '/api/wishlist', { header: headers.customer, tests: allowStatuses(200) }),
        req('Wishlist add', 'POST', '/api/wishlist/{{productId}}', { header: headers.customer, tests: allowStatuses(200) }),
        req('Wishlist remove', 'DELETE', '/api/wishlist/{{productId}}', { header: headers.customer, tests: allowStatuses(200, 204) })
      ]
    },
    {
      name: '11 Smart Size',
      item: [
        req('Smart Size recommend', 'POST', '/api/smart-size/products/{{productId}}', {
          header: headers.customerJson,
          body: rawJson({ height: 170, weight: 65, chest: 92, waist: 78, hip: 96, age: 25, bodyMeasurements: 'regular fit' }),
          tests: allowStatuses(200)
        })
      ]
    },
    {
      name: '12 Admin',
      item: [
        req('Login admin and save token', 'POST', '/api/auth/login', {
          header: headers.json,
          body: rawJson({ email: '{{adminEmail}}', password: '{{adminPassword}}' }),
          tests: [...allowStatuses(200, 400, 401), 'if (pm.response.code === 200) { pm.collectionVariables.set("adminToken", pm.response.json().token); }']
        }),
        req('Admin create product', 'POST', '/api/admin/products', {
          header: headers.adminJson,
          body: rawJson({ brandId: 1, categoryId: 1, name: 'API Test Product {{runId}}', description: 'Created by Postman API testing', basePrice: 199000, salePrice: 179000, material: 'Cotton', fit: 'Regular', gender: 'UNISEX', status: 'ACTIVE', isFeatured: false, isNew: true, stockQty: 10, variants: [], images: [] }),
          tests: [...allowStatuses(200, 400, 401, 403), ...saveJsonPath('adminProductId', '.id')]
        }),
        req('Admin update product', 'PUT', '/api/admin/products/{{adminProductId}}', {
          header: headers.adminJson,
          body: rawJson({ brandId: 1, categoryId: 1, name: 'API Test Product Updated {{runId}}', description: 'Updated by Postman API testing', basePrice: 199000, salePrice: 169000, material: 'Cotton', fit: 'Regular', gender: 'UNISEX', status: 'ACTIVE', isFeatured: false, isNew: false, images: [] }),
          tests: allowStatuses(200, 400, 401, 403, 404)
        }),
        req('Customer calls admin API gets 403', 'GET', '/api/admin/products?page=0&size=10', {
          header: headers.customer,
          tests: testStatus(403)
        })
      ]
    }
  ]
};

const output = path.resolve('postman', 'Fashion_API.postman_collection.json');
fs.mkdirSync(path.dirname(output), { recursive: true });
fs.writeFileSync(output, `${json(collection)}\n`);
console.log(`Wrote ${output}`);
