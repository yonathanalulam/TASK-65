export interface Bundle {
  id: number
  name: string
  description: string | null
  price: number
}

export interface TransactionItem {
  bundleId: number
  bundleName: string
  unitPrice: number
  quantity: number
  lineTotal: number
}

export interface Transaction {
  id: number
  status: string
  totalAmount: number
  receiptNumber: string | null
  initiatedAt: string
  completedAt: string | null
  items: TransactionItem[]
  paymentRefMasked: string | null
}

export interface TransactionDetail extends Transaction {
  // Same as Transaction with all fields populated
}

export interface Receipt {
  receiptNumber: string
  transactionId: number
  generatedAt: string
}
