# Price Comparator Application

## Overview

The Price Comparator is a Java Spring Boot application designed to help users optimize shopping, monitor price alerts, view product recommendations, analyze discount opportunities, and track price trends over time. It processes price data from CSV files and discount data, provides APIs for frontend integration, and supports efficient optimization with discounts.

---

## Key Functionalities

### 1. Basket Optimization

- Optimizes a shopping list against available product prices and discounts.
- Matches items by product name and optionally by brand.
- If brand is not specified, all brands with that product name are considered.
- Handles unmatched items separately.
- Calculates total cost and total savings, timestamps the optimization result.
- Discounts are considered only from the current and previous week.
- Removes duplicates from the shopping list before processing.
- Returns results as JSON, ready for UI consumption.

---

### 2. Discount Management and Queries

- Supports listing **Top N discounts** for a product, a store, or across all stores.
- Can filter discounts added within the last M days.
- Sorts discounts by percentage and money saved; if savings are equal, sorts by saved amount.
- Assumes discount periods don’t overlap (max 2 weeks span).

---

### 3. Product Recommendations

- Shows substitutes sorted by best value per unit (lowest price per standardized unit).
- Can filter recommendations by product name, optionally by brand and store.
- Supports unit conversion for fair comparison (e.g., grams to kilograms).
- Defaults to today’s date if no date filter is provided.

---

### 4. Price Alerts

- Allows users to set target prices for products.
- Considers discounts when calculating effective price.
- Uses a compound key (`productId|store`) for discount mapping to avoid collisions.
- Chooses the discount that yields the best savings when duplicates exist.
- Notifies when product prices drop below or match the target.

---

### 5. Dynamic Price History Graphs

- Provides historical price data points over time for products.
- Supports filtering by product name, brand, store, or category.
- Aggregates prices by average when multiple products match brand/store/category.
- Default history span is 2 weeks but can extend to 1 month, 3 months, etc.
- Frontend can plot price trends using returned date-price points.

---

## APIs

### Basket Optimization

- **POST** `/api/basket/optimize`
- Accepts JSON shopping list array.
- Returns `BasketOptimizationResultDTO` with matched items, unmatched items, total cost, total savings, and timestamp.
- Example HTTP test files:
  - `test_basketController.http`

---

### Discounts

- **POST** `/api/discounts/top` — Top N discounts for a product.
- **POST** `/api/discounts/top-store` — Top N discounts for a single store.
- **POST** `/api/discounts/top-all` — Top N discounts across all stores.
- **POST** `/api/discounts/new` — Discounts added in the last M days (supports filtering by productName and brand).
- Example HTTP test files:
  - `test_discountController1.http`

---

### Product Recommendations

- **GET** `/api/recommendations/substitutes`
- Query params: `productName` (required), `brand` (optional), `store` (optional), `date` (optional, defaults to today)
- Returns substitutes sorted by best price per unit.
- Example HTTP test files:
  - `test_productRecommendationController1.http`
  - `test_productRecommendationController2.http`

---

### Price Alerts

- **GET** `/api/alerts/price`
- Query params: `productName`, `targetPrice`, optional `brand`, `store`, `date`.
- Returns alerts where discounted price is at or below target price.
- Example HTTP test file:
  - `test_alertPrinceController1.http`

---

### Price History

- **GET** `/api/price-history`
- Query params: optional `productName`, `brand`, `store`, `category`, required `startDate`, optional `endDate` (default 2 weeks after startDate).
- Returns list of `{ date: "YYYY-MM-DD", price: <average or product price>, productCount: N }`.
- Example HTTP test files:
  - `test_priceHistoryController1.http`

---

## Data Model Assumptions & Details

### BasketOptimizationResultDTO

- The `shoppingList` can contain items that are not available in any stores; these are placed in `unmatchedItems`.
- Stores the time when the `BasketOptimizerService` is run into the `timestamp`.
- Saves `totalCost` of the shopping list and the `totalSavings` from discounts.

### ShoppingItemDTO-BasketOptimizationResultDTO

- Multiple products with the same name but different brands can exist.
- If the user specifies a brand, matching requires both product name and brand.
- If no brand is specified, the service looks at product name across all brands.
- The shopping list can be submitted as a JSON Array of Objects from the UI.
- Example shopping lists can be found in:  
  `Price Comparator\src\main\resources\data\ShoppingList2`
  `test_basketController.http`
- Default `topN` discounts shown per item is 5, but the user can change it to 10, 15, or 20.

### BasketOptimizerService

- Returns JSON data that can be sent directly to a UI.
- Ensures the shopping list has no duplicates before processing.
- The `allDiscounts` list contains discount info only from the current week and the previous week.
- Discounts cannot span more than 2 weeks.
- If no discount applies, price alone is considered.
- Discounts are assumed not to overlap in real life.

### BasketController

- Endpoint:  
  `POST http://localhost:8080/api/basket/optimize`
- Related HTTP test files:  
  `test_basketController.http`, `basketController1.http`

### DiscountService

- When discounts have equal percentage, sorts based on money saved.

### DiscountController

- `/discounts/top` — Top N discounts for a product.
- `/discounts/top-store` — Top N discounts in a single store.
- `/discounts/top-all` — Top N discounts across all stores.
- `/discounts/new` — Discounts added within the last N days (supports filtering by productName and brand). Returns top N (default 5) sorted by discount percentage and saved amount.
- HTTP test files:  
  `test_discountController1.http` through `test_discountController5.http`

### ProductRecommendationService

- Shows substitutes sorted by best value per unit (lowest price per unit first).
- If a store is selected, filters to products only from that store; otherwise, shows all stores.
- Filters by productName (and optionally brand), not productId (because different packaging results in different IDs).
- Required: product name.
- Optional filters: store, brand, date (defaults to today).
- Converts common units to standard base units (`kg`, `l`, `buc`) for fair conversion (e.g., g → kg, ml → l).

### ProductRecommendationController

- Endpoint:  
  `/api/recommendations/substitutes`
- HTTP test files:  
  `test_productRecommendationController1.http`, `test_productRecommendationController2.http`

### PriceAlertService

- Takes discounts into consideration for price calculations.
- Uses compound key `productId|store` in discount maps to avoid key collisions.
- Chooses the discount that results in the best savings when merging duplicates.

### PriceAlertController

- Endpoint:  
  `GET http://localhost:8080/api/alerts/price`  
- Allows users to set a target price and identifies when a product price drops to or below that target.
- HTTP test file:  
  `test_alertPrinceController1.http`

### PriceHistoryService

- Loads all relevant CSV files within the selected date range.
- Parses products and prices from each file.
- For a single product, creates a list of data points `{date, price}`.
- For filters by brand/store/category, calculates the average price per day of all matching products.

### PriceHistoryController

- Endpoint:  
  `/api/price-history`  
- Query parameters:  
  `productName` (optional), `brand` (optional), `store` (optional), `category` (optional), `startDate` (required), `endDate` (optional, defaults to 2 weeks after `startDate`)
- Returns:  
  List of `{ date: "YYYY-MM-DD", price: <average or product price> }`
- Example GET request:  
  `http://localhost:8080/api/price-history?brand=lidl&startDate=2025-05-01&endDate=2025-05-14`
- HTTP test files:  
  `test_priceHistoryController1.http`, `test_priceHistoryController2.http`

---

## Performance Considerations

- Sorting costs grow with very large lists (e.g., discounts).
- Consider pagination or caching in the future to improve performance.

---

## Running the Application

The main entry point of the application is the `PriceComparatorApplication` class.  
To start the app, simply run this class from your IDE or command line. The application will start on `http://localhost:8080`.

## Testing & HTTP Files

HTTP files for API testing are located in the `Price Comparator/testHTTP` directory.

Run these directly in IntelliJ IDEA by opening the file and clicking the **Run** icon next to each request.

The files test all major APIs, including basket optimization, discounts, product recommendations, alerts, and price history.

## Unit Testing

Some unit tests have been implemented to ensure the correctness of key services.  
The test classes include, but are not limited to:

- `BasketOptimizerServiceTest`
- `DiscountServiceTest`
- `PriceAlertServiceTest`
- `ProductRecommendationServiceTest`
- `PriceHistoryServiceTest`

These test files are located in the `src/test/java/com/market/pricecomparator` directory.

---

### Dynamic Price History Graphs

- Provides historical price data points over time for products, allowing frontend applications to visualize price trends.
- Supports filtering by product name, brand, store, or category.
- When multiple products match a filter (e.g., all products from a brand or store), returns the average price per day.
- Default history span is 2 weeks but can be extended to 1 month, 3 months, or custom ranges.
- The frontend can use this data to plot dynamic graphs showing price evolution.

#### Current Limitations & Possible Improvements

- **Aggregation Logic:** Currently, when filtering by brand, store, or category, the system returns average prices across all matching products. This simple average may not fully reflect actual market trends, especially when product mix varies over time.
- **Data Volume & Performance:** Loading and processing CSV files for longer date ranges could impact performance. Implementing caching or pagination on the API could help.
- **Granularity:** Data points are daily averages, which may not capture intra-day price changes.
- **User Experience:** Adding options to customize aggregation strategies (e.g., median price, weighted average by sales volume) could improve accuracy.
- **Extensibility:** Integrating a dedicated time-series database or analytics engine could enable more advanced queries and visualizations.

This functionality serves as a foundational implementation that can be iteratively improved based on user feedback and performance profiling.
