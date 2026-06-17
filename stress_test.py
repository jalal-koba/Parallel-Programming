import urllib.request
import urllib.error
import concurrent.futures
import time
import json

# إعدادات الاختبار
URL = "http://localhost:9999/api/users/1/orders/checkout"
CONCURRENT_REQUESTS = 50  # عدد الطلبات المتزامنة التي سيتم إرسالها في نفس اللحظة

def send_checkout_request(request_id):
    req = urllib.request.Request(URL, method="POST")
    req.add_header('Content-Type', 'application/json')
    req.add_header('Accept', 'application/json')
    
    start_time = time.time()
    try:
        # إرسال الطلب
        response = urllib.request.urlopen(req)
        status_code = response.getcode()
        body = response.read().decode('utf-8')
        elapsed = time.time() - start_time
        return {"id": request_id, "status": status_code, "body": body, "time": elapsed, "success": True}
    except urllib.error.HTTPError as e:
        body = e.read().decode('utf-8')
        elapsed = time.time() - start_time
        return {"id": request_id, "status": e.code, "body": body, "time": elapsed, "success": False}
    except urllib.error.URLError as e:
        elapsed = time.time() - start_time
        return {"id": request_id, "status": "Connection Error", "body": str(e.reason), "time": elapsed, "success": False}

def run_stress_test():
    print(f"Starting Stress Test on endpoint:")
    print(f"URL: {URL}")
    print(f"Concurrency Level: {CONCURRENT_REQUESTS}\n")
    
    results = []
    
    # استخدام ThreadPoolExecutor لإطلاق الطلبات في نفس اللحظة
    with concurrent.futures.ThreadPoolExecutor(max_workers=CONCURRENT_REQUESTS) as executor:
        # تجهيز المهام
        futures = [executor.submit(send_checkout_request, i) for i in range(1, CONCURRENT_REQUESTS + 1)]
        
        # تجميع النتائج
        for future in concurrent.futures.as_completed(futures):
            results.append(future.result())

    # تحليل النتائج
    successful_requests = [r for r in results if r['status'] in [200, 201]]
    failed_requests = [r for r in results if r['status'] not in [200, 201]]
    
    print("-" * 50)
    print("Stress Test Summary:")
    print("-" * 50)
    print(f"Successful Requests (Acquired Lock): {len(successful_requests)}")
    print(f"Failed/Rejected Requests (Blocked by Lock or Errors): {len(failed_requests)}")
    
    print("\nSample of rejected requests (up to 5):")
    for r in failed_requests[:5]:
        print(f"   [Request #{r['id']}] - Status: {r['status']} - Response: {r['body']}")
        
    print("-" * 50)
    print("Conclusion:")
    if len(successful_requests) <= 1:
        print("SUCCESS: Distributed lock works perfectly. Double submit prevented!")
    else:
        print("WARNING: More than one request succeeded. Check lock configuration!")

if __name__ == "__main__":
    run_stress_test()
