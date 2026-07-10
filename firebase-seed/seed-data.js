const { initializeApp, cert } = require("firebase-admin/app");
const { getFirestore, FieldValue } = require("firebase-admin/firestore");

const serviceAccount = require("./serviceAccountKey.json");

initializeApp({
  credential: cert(serviceAccount),
});

const db = getFirestore();

const categories = [
  { id: "pizza", name: "Pizza", displayOrder: 1, active: true },
  { id: "combo", name: "Combo", displayOrder: 2, active: true },
  { id: "drink", name: "Drink", displayOrder: 3, active: true },
  { id: "dessert", name: "Dessert", displayOrder: 4, active: true },
];

const products = [
  // 11 PIZZA
  {
    id: "margherita",
    name: "Margherita Pizza",
    description: "Classic tomato sauce, mozzarella, fresh basil, and olive oil.",
    categoryId: "pizza",
    basePrice: 8.99,
    imageUrl: "",
    rating: 4.8,
    available: true,
  },
  {
    id: "pepperoni",
    name: "Pepperoni Pizza",
    description: "Mozzarella cheese, tomato sauce, and spicy pepperoni slices.",
    categoryId: "pizza",
    basePrice: 10.99,
    imageUrl: "",
    rating: 4.9,
    available: true,
  },
  {
    id: "hawaiian",
    name: "Hawaiian Pizza",
    description: "Ham, pineapple, mozzarella, and tomato sauce.",
    categoryId: "pizza",
    basePrice: 9.99,
    imageUrl: "",
    rating: 4.6,
    available: true,
  },
  {
    id: "bbq-chicken",
    name: "BBQ Chicken Pizza",
    description: "Grilled chicken, BBQ sauce, red onion, and mozzarella.",
    categoryId: "pizza",
    basePrice: 12.49,
    imageUrl: "",
    rating: 4.7,
    available: true,
  },
  {
    id: "four-cheese",
    name: "Four Cheese Pizza",
    description: "Mozzarella, cheddar, parmesan, and blue cheese.",
    categoryId: "pizza",
    basePrice: 11.99,
    imageUrl: "",
    rating: 4.7,
    available: true,
  },
  {
    id: "seafood-deluxe",
    name: "Seafood Deluxe Pizza",
    description: "Shrimp, squid, crab stick, onion, bell pepper, and mozzarella.",
    categoryId: "pizza",
    basePrice: 15.99,
    imageUrl: "",
    rating: 4.8,
    available: true,
  },
  {
    id: "meat-lovers",
    name: "Meat Lovers Pizza",
    description: "Pepperoni, sausage, ham, bacon, beef, and mozzarella.",
    categoryId: "pizza",
    basePrice: 14.99,
    imageUrl: "",
    rating: 4.9,
    available: true,
  },
  {
    id: "veggie-garden",
    name: "Veggie Garden Pizza",
    description: "Mushroom, bell pepper, onion, tomato, olive, and mozzarella.",
    categoryId: "pizza",
    basePrice: 9.49,
    imageUrl: "",
    rating: 4.5,
    available: true,
  },
  {
    id: "spicy-beef",
    name: "Spicy Beef Pizza",
    description: "Beef, jalapeño, onion, chili sauce, and mozzarella.",
    categoryId: "pizza",
    basePrice: 12.99,
    imageUrl: "",
    rating: 4.6,
    available: true,
  },
  {
    id: "truffle-mushroom",
    name: "Truffle Mushroom Pizza",
    description: "Mushroom, parmesan, mozzarella, cream sauce, and truffle oil.",
    categoryId: "pizza",
    basePrice: 16.99,
    imageUrl: "",
    rating: 4.9,
    available: true,
  },
  {
    id: "midnight-special",
    name: "Midnight Special Pizza",
    description: "Smoked beef, mushroom, black olive, garlic cream, and mozzarella.",
    categoryId: "pizza",
    basePrice: 17.49,
    imageUrl: "",
    rating: 4.8,
    available: true,
  },

  // 4 COMBO
  {
    id: "solo-combo",
    name: "Solo Combo",
    description: "One regular pizza, one drink, and one dessert.",
    categoryId: "combo",
    basePrice: 13.99,
    imageUrl: "",
    rating: 4.6,
    available: true,
  },
  {
    id: "couple-combo",
    name: "Couple Combo",
    description: "One large pizza, two drinks, and garlic bread.",
    categoryId: "combo",
    basePrice: 21.99,
    imageUrl: "",
    rating: 4.7,
    available: true,
  },
  {
    id: "family-combo",
    name: "Family Combo",
    description: "Two large pizzas, four drinks, and two desserts.",
    categoryId: "combo",
    basePrice: 34.99,
    imageUrl: "",
    rating: 4.8,
    available: true,
  },
  {
    id: "party-combo",
    name: "Party Combo",
    description: "Three pizzas, six drinks, garlic bread, and desserts.",
    categoryId: "combo",
    basePrice: 49.99,
    imageUrl: "",
    rating: 4.9,
    available: true,
  },

  // 3 DRINK
  {
    id: "coke",
    name: "Coca-Cola",
    description: "Cold Coca-Cola soft drink.",
    categoryId: "drink",
    basePrice: 1.99,
    imageUrl: "",
    rating: 4.5,
    available: true,
  },
  {
    id: "orange-soda",
    name: "Orange Soda",
    description: "Chilled sparkling orange soda.",
    categoryId: "drink",
    basePrice: 2.49,
    imageUrl: "",
    rating: 4.4,
    available: true,
  },
  {
    id: "sparkling-water",
    name: "Sparkling Water",
    description: "Refreshing sparkling mineral water.",
    categoryId: "drink",
    basePrice: 1.49,
    imageUrl: "",
    rating: 4.3,
    available: true,
  },

  // 2 DESSERT
  {
    id: "tiramisu",
    name: "Classic Tiramisu",
    description: "Coffee-soaked ladyfingers with mascarpone cream.",
    categoryId: "dessert",
    basePrice: 5.49,
    imageUrl: "",
    rating: 4.7,
    available: true,
  },
  {
    id: "chocolate-lava",
    name: "Chocolate Lava Cake",
    description: "Warm chocolate cake with molten chocolate center.",
    categoryId: "dessert",
    basePrice: 5.99,
    imageUrl: "",
    rating: 4.8,
    available: true,
  },
];

async function deleteCollection(collectionName) {
  const snapshot = await db.collection(collectionName).get();

  if (snapshot.empty) {
    console.log(`No old documents in ${collectionName}.`);
    return;
  }

  const batch = db.batch();

  snapshot.docs.forEach((doc) => {
    batch.delete(doc.ref);
  });

  await batch.commit();
  console.log(`Deleted ${snapshot.size} old documents from ${collectionName}.`);
}

async function seedCollection(collectionName, items) {
  const now = FieldValue.serverTimestamp();
  const batch = db.batch();

  items.forEach((item) => {
    const ref = db.collection(collectionName).doc(item.id);
    batch.set(ref, {
      ...item,
      createdAt: now,
      updatedAt: now,
    });
  });

  await batch.commit();
  console.log(`Seeded ${items.length} documents into ${collectionName}.`);
}

async function main() {
  await deleteCollection("products");
  await deleteCollection("categories");

  await seedCollection("categories", categories);
  await seedCollection("products", products);

  console.log("Done. Products reset cleanly with USD prices.");
}

main().catch((error) => {
  console.error("Seed failed:", error);
  process.exit(1);
});