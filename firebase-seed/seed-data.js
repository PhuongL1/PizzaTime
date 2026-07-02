const { initializeApp, cert } = require("firebase-admin/app");
const { getFirestore, FieldValue } = require("firebase-admin/firestore");

const serviceAccount = require("./serviceAccountKey.json");

initializeApp({
  credential: cert(serviceAccount),
});

const db = getFirestore();

const categories = [
  {
    id: "pizza",
    name: "Pizza",
    displayOrder: 1,
    active: true,
  },
  {
    id: "combo",
    name: "Combo",
    displayOrder: 2,
    active: true,
  },
  {
    id: "drink",
    name: "Drink",
    displayOrder: 3,
    active: true,
  },
  {
    id: "side",
    name: "Side",
    displayOrder: 4,
    active: true,
  },
];

const products = [
  {
    id: "margherita",
    name: "Margherita Pizza",
    description: "Classic tomato sauce, mozzarella, and basil.",
    categoryId: "pizza",
    basePrice: 99000,
    imageUrl: "",
    rating: 4.8,
    available: true,
  },
  {
    id: "pepperoni",
    name: "Pepperoni Pizza",
    description: "Mozzarella cheese with spicy pepperoni slices.",
    categoryId: "pizza",
    basePrice: 129000,
    imageUrl: "",
    rating: 4.9,
    available: true,
  },
  {
    id: "hawaiian",
    name: "Hawaiian Pizza",
    description: "Ham, pineapple, mozzarella, and tomato sauce.",
    categoryId: "pizza",
    basePrice: 119000,
    imageUrl: "",
    rating: 4.6,
    available: true,
  },
  {
    id: "family-combo",
    name: "Family Combo",
    description: "Two pizzas, one side, and two drinks.",
    categoryId: "combo",
    basePrice: 299000,
    imageUrl: "",
    rating: 4.7,
    available: true,
  },
  {
    id: "coke",
    name: "Coca-Cola",
    description: "Cold soft drink.",
    categoryId: "drink",
    basePrice: 19000,
    imageUrl: "",
    rating: 4.5,
    available: true,
  },
  {
    id: "garlic-bread",
    name: "Garlic Bread",
    description: "Toasted bread with garlic butter.",
    categoryId: "side",
    basePrice: 39000,
    imageUrl: "",
    rating: 4.4,
    available: true,
  },
];

async function seedCollection(collectionName, items) {
  const now = FieldValue.serverTimestamp();

  for (const item of items) {
    await db.collection(collectionName).doc(item.id).set({
      ...item,
      createdAt: now,
      updatedAt: now,
    });

    console.log(`Seeded ${collectionName}/${item.id}`);
  }
}

async function main() {
  await seedCollection("categories", categories);
  await seedCollection("products", products);

  console.log("Done seeding Firestore data.");
}

main().catch((error) => {
  console.error(error);
  process.exit(1);
});