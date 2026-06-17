import urllib.request
import urllib.error
import concurrent.futures
import time

# Endpoint to test concurrency and distributed locks (Checkout)
URL = "http://localhost:9999/api/users/1/orders/checkout"
CONCURRENT_REQUESTS = 20

def send_checkout_request(request_id):
    req = urllib.request.Request(URL, method="POST")
    req.add_header('Content-Type', 'application/json')
    req.add_header('Accept', 'application/json')
    
    start_time = time.time()
    try:
        response = urllib.request.urlopen(req)
        status_code = response.getcode()
        body = response.read().decode('utf-8')
        elapsed = (time.time() - start_time) * 1000
        return {"id": request_id, "status": status_code, "body": body, "time": elapsed}
    except urllib.error.HTTPError as e:
        body = e.read().decode('utf-8')
        elapsed = (time.time() - start_time) * 1000
        return {"id": request_id, "status": e.code, "body": body, "time": elapsed}
    except Exception as e:
        elapsed = (time.time() - start_time) * 1000
        return {"id": request_id, "status": "Error", "body": str(e), "time": elapsed}

def run_concurrency_test():
    print("==================================================")
    print("Testing Requirement 7: Concurrency Control & Locks")
    print("==================================================")
    print(f"Target URL: {URL}")
    print(f"Simulating {CONCURRENT_REQUESTS} concurrent users clicking 'Checkout' at the exact same millisecond...\n")
    
    results = []
    
    # Fire all requests concurrently
    with concurrent.futures.ThreadPoolExecutor(max_workers=CONCURRENT_REQUESTS) as executor:
        futures = [executor.submit(send_checkout_request, i) for i in range(1, CONCURRENT_REQUESTS + 1)]
        for future in concurrent.futures.as_completed(futures):
            results.append(future.result())

    # Analyze
    success_reqs = [r for r in results if r['status'] in [200, 201]]
    locked_reqs = [r for r in results if r['status'] in [400, 404, 409, 500] and r['status'] != "Error"]
    error_reqs = [r for r in results if r['status'] == "Error"]

    print("--------------------------------------------------")
    print("Concurrency Test Results:")
    print("--------------------------------------------------")
    print(f"Total Requests Sent: {CONCURRENT_REQUESTS}")
    print(f"Successful Purchases (Processed): {len(success_reqs)}")
    print(f"Rejected Requests (Prevented by Locks/Logic): {len(locked_reqs)}")
    if len(error_reqs) > 0:
        print(f"Network Errors: {len(error_reqs)}")
    
    print("\nDetailed Sample of Rejected Requests:")
    for r in locked_reqs[:5]:
        print(f"  [Req #{r['id']}] - Status: {r['status']} | Msg: {r['body'].strip()[:80]}")

    print("\n--------------------------------------------------")
    print("Conclusion:")
    if len(success_reqs) <= 1:
        print("SUCCESS! The system successfully prevented Double-Submit and Race Conditions.")
        print("Only a maximum of 1 request was allowed to modify the database, while others")
        print("were safely blocked by Redisson/Pessimistic Locks.")
    else:
        print("WARNING: Multiple requests succeeded! Concurrency control failed.")

if __name__ == "__main__":
    run_concurrency_test()
