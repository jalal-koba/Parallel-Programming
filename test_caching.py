import urllib.request
import time
import json

# Endpoint to test caching (e.g., Get All Products)
# Assuming products are cached in Redis upon first request
URL = "http://localhost:9999/api/products"

def fetch_products(request_name):
    req = urllib.request.Request(URL, method="GET")
    req.add_header('Accept', 'application/json')
    
    start_time = time.time()
    try:
        response = urllib.request.urlopen(req)
        status_code = response.getcode()
        body = response.read().decode('utf-8')
        elapsed = (time.time() - start_time) * 1000 # in milliseconds
        return status_code, elapsed
    except Exception as e:
        return str(e), 0

def run_cache_test():
    print("==================================================")
    print("Testing Requirement 6: Distributed Caching (Redis)")
    print("==================================================")
    print(f"Target URL: {URL}\n")

    print("Step 1: First Request (Expected Cache Miss - hits Database)")
    status1, time1 = fetch_products("First Request")
    print(f" -> Status: {status1} | Response Time: {time1:.2f} ms")
    
    # Adding a tiny delay to simulate a real user reading the page
    time.sleep(1)

    print("\nStep 2: Second Request (Expected Cache Hit - served from Redis)")
    status2, time2 = fetch_products("Second Request")
    print(f" -> Status: {status2} | Response Time: {time2:.2f} ms")

    print("\nStep 3: Third Request (Cache Hit)")
    status3, time3 = fetch_products("Third Request")
    print(f" -> Status: {status3} | Response Time: {time3:.2f} ms")

    print("\n--------------------------------------------------")
    print("Caching Performance Results:")
    print("--------------------------------------------------")
    if time2 < time1:
        improvement = ((time1 - time2) / time1) * 100
        print(f"Result: SUCCESS! Caching improved response time by {improvement:.2f}%.")
        print("Explanation: The first request took longer because it queried PostgreSQL.")
        print("Subsequent requests were drastically faster as they were served directly from Redis RAM.")
    else:
        print("Result: INCONCLUSIVE. The second request wasn't significantly faster.")
        print("Ensure the @Cacheable annotation is active on the products endpoint.")

if __name__ == "__main__":
    run_cache_test()
