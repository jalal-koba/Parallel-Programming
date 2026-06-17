package com.example.ecosystem.aop;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * ===================================================================
 * AOP Aspect: مراقبة أداء المهام غير المتزامنة
 * ===================================================================
 *
 * البرمجة الموجهة للجوانب (Aspect-Oriented Programming):
 * --------------------------------------------------------
 * بدلاً من وضع كود قياس الأداء في كل service يدوياً،
 * نعرّف هنا "جانب" (Aspect) يُطبَّق تلقائياً على كل
 * الدوال في packages المحددة.
 *
 * كيف يعمل؟
 * ----------
 * Spring يعترض (Intercept) استدعاء الدالة قبل تنفيذها،
 * يقيس الوقت، ثم يُسجّله في اللوقس — كل هذا بدون تعديل
 * الكود الأصلي للـ Service أو الـ Listener.
 *
 * المصطلحات:
 *   @Aspect    → تعريف الكلاس كـ "جانب" يُعالج cross-cutting concerns
 *   @Pointcut  → تعريف "أين" يُطبَّق هذا الجانب (على أي دوال)
 *   @Around    → "يلتف" حول الدالة: ينفذ كود قبلها وبعدها
 *   joinPoint  → نقطة الالتقاء = الدالة المُعترَضة نفسها
 */
@Aspect
@Component
public class PerformanceMonitoringAspect {

    private static final Logger log = LoggerFactory.getLogger(PerformanceMonitoringAspect.class);

    /**
     * Pointcut 1: كل دوال طبقة الـ Service
     * يشمل: OrderService, CartService, ProductService, ...
     */
    @Pointcut("execution(* com.example.ecosystem.service.*.*(..))")
    public void serviceLayer() {}

    /**
     * Pointcut 2: دالة handleOrderCreated في الـ Listener (المهام غير المتزامنة)
     * يُثبت أن مراقبة الأداء تشمل المسار الرئيسي والمسار الخلفي معاً
     */
    @Pointcut("execution(* com.example.ecosystem.listener.*.*(..))")
    public void asyncListenerLayer() {}

    /**
     * Around Advice على الـ Service Layer:
     * يقيس زمن تنفيذ كل دالة service ويُسجّله
     *
     * الفائدة لمتطلب رقم 10 (Benchmarking):
     * هذا اللوق يعطينا بيانات زمن الاستجابة لكل عملية
     * مما يُسهّل تحديد الاختناقات (Bottlenecks)
     */
    @Around("serviceLayer()")
    public Object measureServicePerformance(ProceedingJoinPoint joinPoint) throws Throwable {

        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();
        String fullMethod = className + "." + methodName + "()";

        long startTime = System.currentTimeMillis();

        try {
            // تنفيذ الدالة الأصلية
            Object result = joinPoint.proceed();

            long duration = System.currentTimeMillis() - startTime;
            log.info("[AOP-PERF] {} | خيط: {} | زمن: {} ms | حالة: SUCCESS",
                    fullMethod,
                    Thread.currentThread().getName(),
                    duration);

            // تحذير إذا تجاوزت الدالة عتبة الأداء (500ms)
            if (duration > 500) {
                log.warn("[AOP-PERF] ⚠ اختناق محتمل! {} استغرقت {} ms > 500ms",
                        fullMethod, duration);
            }

            return result;

        } catch (Throwable ex) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("[AOP-PERF] {} | خيط: {} | زمن: {} ms | حالة: FAILED | خطأ: {}",
                    fullMethod,
                    Thread.currentThread().getName(),
                    duration,
                    ex.getMessage());
            throw ex; // أعد رمي الاستثناء حتى يعالجه الـ caller الأصلي
        }
    }

    /**
     * Around Advice على الـ Async Listener:
     * يثبت أن المهام الخلفية تعمل على خيوط منفصلة
     * وتُقيس وقت إنجاز كل مهمة خلفية
     */
    @Around("asyncListenerLayer()")
    public Object measureAsyncTaskPerformance(ProceedingJoinPoint joinPoint) throws Throwable {

        String methodName = joinPoint.getSignature().getName();
        long startTime = System.currentTimeMillis();

        log.info("[AOP-ASYNC] بدأت مهمة خلفية: {} | الخيط: {}",
                methodName, Thread.currentThread().getName());

        try {
            Object result = joinPoint.proceed();
            long duration = System.currentTimeMillis() - startTime;

            log.info("[AOP-ASYNC] ✓ انتهت مهمة خلفية: {} | الزمن الكلي: {} ms",
                    methodName, duration);
            return result;

        } catch (Throwable ex) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("[AOP-ASYNC] ✗ فشلت مهمة خلفية: {} | الزمن: {} ms | خطأ: {}",
                    methodName, duration, ex.getMessage());
            throw ex;
        }
    }
}