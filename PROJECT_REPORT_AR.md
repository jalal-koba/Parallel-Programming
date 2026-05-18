# تقرير حالة مشروع Parallel Ecommerce Ecosystem

## نظرة عامة

المشروع هو تطبيق تجارة إلكترونية مبني باستخدام Spring Boot و PostgreSQL، والهدف الأساسي منه تنفيذ دورة شراء حقيقية مع التركيز على سلامة العمليات عند وجود طلبات متزامنة على نفس المخزون.

حتى الآن تم بناء الأساس الوظيفي للنظام:

- إدارة المستخدمين.
- إدارة التصنيفات.
- إدارة المنتجات وربطها بالتصنيفات.
- إدارة السلة.
- تنفيذ checkout آمن على المخزون.
- عرض الطلبات وتفاصيلها وإلغاء الطلب.
- Postman Collection لتجربة الـ APIs.
- اختبار تزامن يثبت عدم حدوث overselling.

## التقنيات المستخدمة

- Java 17
- Spring Boot 3.4
- Spring Web
- Spring Data JPA
- PostgreSQL
- H2 للاختبارات
- Docker Compose
- Maven

## تشغيل المشروع

> **ملاحظة بعد الدمج:** الكود المصدر موجود في جذر المستودع (`src/`، `pom.xml`). مجلد `ecosystem/` نسخة متزامنة منه — يمكن البناء من أي منهما.

### تشغيل محلي

```powershell
.\mvnw.cmd spring-boot:run
```

الرابط الافتراضي (تطوير محلي + batch):

```text
http://localhost:8080
```

### Docker Compose — تطبيق واحد + Postgres (batch / تطوير)

```powershell
docker compose -f docker-compose.dev.yml up --build
```

- التطبيق: `http://localhost:8080`
- Postgres: `localhost:5432` (قاعدة `parallel-ecommerce`)

### Docker Compose — موازنة حمل (نسختان + Apache)

```powershell
docker compose down
docker compose up --build
```

- الموازن: `http://localhost:9999`
- كل نسخة تطبيق داخلياً على المنفذ `8081`
- Postgres: `localhost:5434` (قاعدة `ecosystem`)

## ملف Postman

تم إنشاء Collection جاهزة:

```text
postman_collection.json
```

المتغيرات الأساسية داخل الـ collection:

- `baseUrl`: رابط التطبيق، افتراضيا `http://localhost:8081`
- `userId`: رقم المستخدم الحالي
- `categoryId`: رقم التصنيف الحالي
- `productId`: رقم المنتج الحالي
- `orderId`: رقم الطلب الحالي

## التسلسل المقترح للتجربة

أفضل ترتيب لتجربة النظام من Postman:

1. Create User
2. Create Category
3. Create Product
4. Get Or Create Cart
5. Add Product To Cart
6. Update Cart Item Quantity
7. Checkout
8. Get User Orders
9. Get Order By Id
10. Cancel Order

## Users API

### إنشاء مستخدم

```http
POST /api/users
```

Body:

```json
{
  "username": "buyer1",
  "email": "buyer1@example.com",
  "password": "password",
  "role": "customer"
}
```

### عرض كل المستخدمين

```http
GET /api/users
```

### عرض مستخدم حسب id

```http
GET /api/users/{userId}
```

### تعديل مستخدم

```http
PUT /api/users/{userId}
```

Body:

```json
{
  "username": "buyer1_updated",
  "email": "buyer1.updated@example.com",
  "password": "new-password",
  "role": "customer"
}
```

### حذف مستخدم

```http
DELETE /api/users/{userId}
```

## Categories API

### إنشاء تصنيف

```http
POST /api/categories
```

Body:

```json
{
  "name": "Electronics"
}
```

### عرض كل التصنيفات

```http
GET /api/categories
```

### عرض تصنيف حسب id

```http
GET /api/categories/{categoryId}
```

### تعديل تصنيف

```http
PUT /api/categories/{categoryId}
```

Body:

```json
{
  "name": "Updated Electronics"
}
```

### حذف تصنيف

```http
DELETE /api/categories/{categoryId}
```

ملاحظة: حذف التصنيف لا يحذف المنتجات التابعة له، بل يفصل المنتجات عن التصنيف.

## Products API

### إنشاء منتج

```http
POST /api/products
```

Body:

```json
{
  "name": "Parallel Laptop",
  "description": "Laptop used for concurrency checkout testing",
  "price": 1000,
  "stockQuantity": 10,
  "categoryId": 1
}
```

### عرض كل المنتجات

```http
GET /api/products
```

### عرض منتج حسب id

```http
GET /api/products/{productId}
```

### عرض منتجات تصنيف معين

```http
GET /api/products/category/{categoryId}
```

### تعديل منتج

```http
PUT /api/products/{productId}
```

Body:

```json
{
  "name": "Parallel Laptop Pro",
  "description": "Updated product",
  "price": 1250,
  "stockQuantity": 15,
  "categoryId": 1
}
```

### حذف منتج

```http
DELETE /api/products/{productId}
```

## Cart API

### عرض أو إنشاء سلة للمستخدم

```http
GET /api/users/{userId}/cart
```

إذا لم تكن السلة موجودة، يتم إنشاؤها تلقائيا.

### إضافة منتج إلى السلة

```http
POST /api/users/{userId}/cart/items
```

Body:

```json
{
  "productId": 1,
  "quantity": 1
}
```

إذا كان المنتج موجودا مسبقا في السلة، يتم زيادة الكمية.

### تعديل كمية منتج داخل السلة

```http
PUT /api/users/{userId}/cart/items/{productId}
```

Body:

```json
{
  "quantity": 2
}
```

### حذف منتج من السلة

```http
DELETE /api/users/{userId}/cart/items/{productId}
```

### تفريغ السلة

```http
DELETE /api/users/{userId}/cart/items
```

## Orders API

### تنفيذ Checkout

```http
POST /api/users/{userId}/orders/checkout
```

العملية تقوم بالخطوات التالية:

- قراءة عناصر السلة.
- قفل المنتجات المطلوبة باستخدام pessimistic lock.
- التحقق من توفر المخزون.
- إنقاص المخزون.
- إنشاء طلب وعناصر الطلب.
- تفريغ السلة بعد نجاح الطلب.

### عرض طلبات مستخدم

```http
GET /api/users/{userId}/orders
```

### عرض تفاصيل طلب محدد

```http
GET /api/users/{userId}/orders/{orderId}
```

### إلغاء طلب

```http
POST /api/users/{userId}/orders/{orderId}/cancel
```

عند إلغاء الطلب:

- يتم تغيير حالة الطلب إلى `CANCELLED`.
- يتم إرجاع كميات المنتجات إلى المخزون.
- إذا كان الطلب ملغى مسبقا، يتم إرجاع خطأ مناسب.

## حماية المخزون من الطلبات المتزامنة

تم تنفيذ حماية المخزون في `ProductRepository` من خلال:

```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("SELECT p FROM Product p WHERE p.id = :id")
Optional<Product> findByIdWithPessimisticLock(@Param("id") Long id);
```

الفكرة: عند تنفيذ checkout، يتم قفل صف المنتج داخل قاعدة البيانات قبل تعديل المخزون، حتى لا يستطيع طلبان متزامنان إنقاص نفس المخزون بطريقة تؤدي إلى قيمة خاطئة أو سالبة.

## اختبار التزامن

يوجد اختبار باسم:

```text
OrderServiceConcurrencyTests
```

سيناريو الاختبار:

- منتج واحد مخزونه 10.
- 50 مستخدما يحاولون الشراء في نفس الوقت.
- المتوقع: 10 طلبات فقط تنجح.
- المخزون النهائي يجب أن يصبح 0.
- لا يجب أن يحدث overselling.

تشغيل الاختبارات:

```powershell
.\mvnw.cmd test
```

آخر نتيجة:

```text
BUILD SUCCESS
Tests run: 2, Failures: 0, Errors: 0
```

## الأخطاء التي يتم التعامل معها

تمت إضافة معالجة أخطاء مركزية عبر `ApiExceptionHandler`.

أمثلة:

- مستخدم غير موجود: `404`
- منتج غير موجود: `404`
- تصنيف مكرر: `409`
- مستخدم أو إيميل مكرر: `409`
- مخزون غير كاف: `409`
- checkout لسلة فارغة: `409`
- body غير صالح: `400`

## أهم الملفات التي تم العمل عليها

- `controller/UserController.java`
- `controller/CategoryController.java`
- `controller/ProductController.java`
- `controller/CartController.java`
- `controller/OrderController.java`
- `service/UserService.java`
- `service/CategoryService.java`
- `service/ProductService.java`
- `service/CartService.java`
- `service/OrderService.java`
- `repository/ProductRepository.java`
- `repository/CartItemRepository.java`
- `repository/OrderRepository.java`
- `dto/*`
- `postman_collection.json`
- `docker-compose.yml`
- `dockerfile`

## ما الذي يستطيع الفريق فعله الآن من خلال المشروع؟

الفريق يستطيع حاليا:

- إنشاء مستخدمين.
- تعديل وحذف المستخدمين.
- إنشاء تصنيفات للمنتجات.
- تعديل وحذف التصنيفات.
- إنشاء منتجات وربطها بتصنيف.
- عرض المنتجات كلها أو حسب التصنيف.
- تعديل وحذف المنتجات.
- إنشاء سلة لكل مستخدم.
- إضافة منتجات إلى السلة.
- تعديل كمية المنتجات في السلة.
- حذف منتج من السلة.
- تفريغ السلة.
- تنفيذ عملية شراء كاملة.
- عرض الطلبات بعد الشراء.
- عرض تفاصيل طلب معين.
- إلغاء الطلب وإرجاع المخزون.
- تجربة كل ذلك من Postman.
- تشغيل اختبار تزامن لإثبات سلامة المخزون.

## الخطوة القادمة المقترحة

الخطوة الوظيفية القادمة هي تنفيذ Wishlist:

- إضافة منتج إلى قائمة الرغبات.
- حذف منتج من قائمة الرغبات.
- عرض قائمة رغبات المستخدم.

بعدها يمكن الانتقال إلى حالات الطلب بشكل أوسع مثل `PENDING`, `PAID`, `SHIPPED`, `CANCELLED` إذا احتاج الفريق ذلك.
