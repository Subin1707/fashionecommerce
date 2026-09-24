import { apiRequest } from './api'

export function askChatbot(message) {
  return apiRequest('/api/chatbot', {
    method: 'POST',
    body: JSON.stringify({ message }),
  })
}

export function recommendSize(productId, payload) {
  return apiRequest(`/api/smart-size/products/${productId}`, {
    method: 'POST',
    body: JSON.stringify(payload),
  })
}

export function recommendStyle(payload) {
  return apiRequest('/api/style/recommend', {
    method: 'POST',
    body: JSON.stringify(payload),
  })
}
