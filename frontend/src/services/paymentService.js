import { apiRequest } from './api'

export const paymentMethods = ['COD', 'BANK_TRANSFER', 'E_WALLET', 'VNPAY']

export function createVNPayPayment(orderId) {
	return apiRequest(`/api/vnpay/orders/${orderId}`, { method: 'POST' })
}
