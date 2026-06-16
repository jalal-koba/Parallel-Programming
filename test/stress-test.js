import http from "k6/http";
import { check, sleep } from "k6";

export const options = {
  vus: 200,
  duration: "20s",
};

const BASE_URL = "http://apache_load_balancer:80";
export default function () {
  const params = {
    headers: { "Content-Type": "application/json" },
  };

  
  const userId = __VU;

 
  const productId = Math.floor(Math.random() * 50) + 1;

  let resProducts = http.get(`${BASE_URL}/api/products`);
  check(resProducts, { "GET /products (200 OK)": (r) => r.status === 200 });

  let resCategories = http.get(`${BASE_URL}/api/categories`);
  check(resCategories, { "GET /categories (200 OK)": (r) => r.status === 200 });

  const cartPayload = JSON.stringify({
    productId: productId,
    quantity: 1,
  });

  let resCart = http.post(
    `${BASE_URL}/api/users/${userId}/cart/items`,
    cartPayload,
    params,
  );
  check(resCart, {
    "POST /cart/items (200/201/404/409)": (r) =>
      [200, 201, 404, 409].includes(r.status),
  });

  let resCheckout = http.post(
    `${BASE_URL}/api/users/${userId}/orders/checkout`,
    null,
    params,
  );
  check(resCheckout, {
    "POST /checkout Handled Safely": (r) =>
      [200, 201, 409, 400, 404].includes(r.status),
  });

  let resBatchHistory = http.get(`${BASE_URL}/batch/job-history`);
  check(resBatchHistory, {
    "GET /job-history (200/404)": (r) => [200, 404].includes(r.status),
  });

  sleep(Math.random() * 0.5); 
}
