import { apiRequest } from './api'

export function askChatbot(message, history = [], productId = null) {
  return apiRequest('/api/chatbot', {
    method: 'POST',
    body: JSON.stringify({ message, history, productId }),
  })
}

export function searchChatbotProducts(message, productId = null) {
  return apiRequest('/api/chatbot/products', {
    method: 'POST',
    body: JSON.stringify({ message, productId }),
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
