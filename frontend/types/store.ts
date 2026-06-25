export interface Product {
  id: string;
  name: string;
  description: string;
  price: number;
  emoji: string;
  category: string;
  image?: string;
}

export interface CartItem {
  product: Product;
  quantity: number;
}

export const PRODUCTS: Product[] = [
  {
    id: "1",
    name: "Tênis Runner Pro X",
    description: "Solado de amortecimento reativo para máxima performance",
    price: 29990,
    emoji: "👟",
    category: "Calçados",
    image: "/products/tenis.jpg",
  },
  {
    id: "2",
    name: "Mochila Urban Pack",
    description: "Compartimento acolchoado para notebook até 15\" e acesso rápido",
    price: 18990,
    emoji: "🎒",
    category: "Acessórios",
    image: "/products/mochila.jpg",
  },
  {
    id: "3",
    name: "Óculos Polarizados UV400",
    description: "Lentes polarizadas com proteção total contra raios UVA e UVB",
    price: 14990,
    emoji: "🕶️",
    category: "Acessórios",
    image: "/products/oculos.jpg",
  },
  {
    id: "4",
    name: "Fone Bluetooth ANC",
    description: "Cancelamento de ruído ativo com 30h de bateria e driver 40mm",
    price: 24990,
    emoji: "🎧",
    category: "Eletrônicos",
    image: "/products/fone.jpg",
  },
  {
    id: "5",
    name: "Relógio Smart Band Pro",
    description: "Monitor cardíaco 24h, GPS integrado e resistência à água IP68",
    price: 39990,
    emoji: "⌚",
    category: "Eletrônicos",
    image: "/products/relogio.jpg",
  },
];
