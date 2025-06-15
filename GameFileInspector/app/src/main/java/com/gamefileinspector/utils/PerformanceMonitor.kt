package com.gamefileinspector.utils

import android.util.Log
import java.util.concurrent.ConcurrentHashMap

/**
 * Utility for monitoring app performance and timing operations
 */
object PerformanceMonitor {
    
    private const val TAG = "PerformanceMonitor"
    private val timers = ConcurrentHashMap<String, Long>()
    private val metrics = ConcurrentHashMap<String, PerformanceMetric>()
    
    data class PerformanceMetric(
        val name: String,
        var totalTime: Long = 0,
        var callCount: Int = 0,
        var minTime: Long = Long.MAX_VALUE,
        var maxTime: Long = 0,
        var lastTime: Long = 0
    ) {
        val averageTime: Double
            get() = if (callCount > 0) totalTime.toDouble() / callCount else 0.0
    }
    
    /**
     * Start timing an operation
     */
    fun startTimer(operationName: String) {
        timers[operationName] = System.currentTimeMillis()
    }
    
    /**
     * Stop timing an operation and record the result
     */
    fun stopTimer(operationName: String): Long {
        val startTime = timers.remove(operationName)
        if (startTime == null) {
            Log.w(TAG, "Timer '$operationName' was not started")
            return 0
        }
        
        val duration = System.currentTimeMillis() - startTime
        recordMetric(operationName, duration)
        
        Log.d(TAG, "$operationName took ${duration}ms")
        return duration
    }
    
    /**
     * Time a block of code
     */
    inline fun <T> timeOperation(operationName: String, block: () -> T): T {
        startTimer(operationName)
        try {
            return block()
        } finally {
            stopTimer(operationName)
        }
    }
    
    /**
     * Record a performance metric
     */
    private fun recordMetric(name: String, duration: Long) {
        val metric = metrics.getOrPut(name) { PerformanceMetric(name) }
        
        metric.apply {
            totalTime += duration
            callCount++
            minTime = minOf(minTime, duration)
            maxTime = maxOf(maxTime, duration)
            lastTime = duration
        }
    }
    
    /**
     * Get performance metrics for an operation
     */
    fun getMetric(operationName: String): PerformanceMetric? {
        return metrics[operationName]
    }
    
    /**
     * Get all performance metrics
     */
    fun getAllMetrics(): Map<String, PerformanceMetric> {
        return metrics.toMap()
    }
    
    /**
     * Generate a performance report
     */
    fun generateReport(): String {
        val report = StringBuilder()
        report.append("Performance Report\n")
        report.append("==================\n\n")
        
        if (metrics.isEmpty()) {
            report.append("No performance data available.\n")
            return report.toString()
        }
        
        val sortedMetrics = metrics.values.sortedByDescending { it.totalTime }
        
        report.append("%-25s %8s %8s %8s %8s %8s\n".format(
            "Operation", "Calls", "Total", "Avg", "Min", "Max"
        ))
        report.append("-".repeat(75)).append("\n")
        
        sortedMetrics.forEach { metric ->
            report.append("%-25s %8d %6dms %6.1fms %6dms %6dms\n".format(
                metric.name.take(25),
                metric.callCount,
                metric.totalTime,
                metric.averageTime,
                metric.minTime,
                metric.maxTime
            ))
        }
        
        report.append("\nSlowest Operations:\n")
        sortedMetrics.take(5).forEach { metric ->
            report.append("• ${metric.name}: ${metric.maxTime}ms (max)\n")
        }
        
        report.append("\nMost Called Operations:\n")
        metrics.values.sortedByDescending { it.callCount }.take(5).forEach { metric ->
            report.append("• ${metric.name}: ${metric.callCount} calls\n")
        }
        
        return report.toString()
    }
    
    /**
     * Log performance report to console
     */
    fun logReport() {
        Log.i(TAG, generateReport())
    }
    
    /**
     * Clear all performance data
     */
    fun clear() {
        timers.clear()
        metrics.clear()
        Log.d(TAG, "Performance data cleared")
    }
    
    /**
     * Monitor memory usage
     */
    fun logMemoryUsage(context: String = "") {
        val runtime = Runtime.getRuntime()
        val usedMemory = runtime.totalMemory() - runtime.freeMemory()
        val maxMemory = runtime.maxMemory()
        val availableMemory = maxMemory - usedMemory
        
        val usedMB = usedMemory / (1024 * 1024)
        val maxMB = maxMemory / (1024 * 1024)
        val availableMB = availableMemory / (1024 * 1024)
        
        val message = if (context.isNotEmpty()) {
            "Memory usage [$context]: ${usedMB}MB used, ${availableMB}MB available, ${maxMB}MB max"
        } else {
            "Memory usage: ${usedMB}MB used, ${availableMB}MB available, ${maxMB}MB max"
        }
        
        Log.d(TAG, message)
    }
    
    /**
     * Check if memory usage is high
     */
    fun isMemoryUsageHigh(): Boolean {
        val runtime = Runtime.getRuntime()
        val usedMemory = runtime.totalMemory() - runtime.freeMemory()
        val maxMemory = runtime.maxMemory()
        val usagePercentage = (usedMemory.toDouble() / maxMemory) * 100
        
        return usagePercentage > 80.0
    }
    
    /**
     * Suggest garbage collection if memory usage is high
     */
    fun suggestGCIfNeeded() {
        if (isMemoryUsageHigh()) {
            Log.w(TAG, "High memory usage detected, suggesting garbage collection")
            System.gc()
        }
    }
    
    /**
     * Monitor file operation performance
     */
    fun monitorFileOperation(fileName: String, operation: String, sizeBytes: Long, durationMs: Long) {
        val throughputMBps = if (durationMs > 0) {
            (sizeBytes.toDouble() / (1024 * 1024)) / (durationMs.toDouble() / 1000)
        } else {
            0.0
        }
        
        Log.d(TAG, "File operation: $operation on $fileName (${sizeBytes / 1024}KB) took ${durationMs}ms (${String.format("%.2f", throughputMBps)} MB/s)")
        
        recordMetric("file_$operation", durationMs)
    }
    
    /**
     * Get performance summary for UI display
     */
    fun getPerformanceSummary(): Map<String, String> {
        val summary = mutableMapOf<String, String>()
        
        // File operations
        val fileAnalysis = getMetric("file_analysis")
        if (fileAnalysis != null) {
            summary["File Analysis"] = "${fileAnalysis.callCount} files, avg ${fileAnalysis.averageTime.toInt()}ms"
        }
        
        val fileModification = getMetric("file_modification")
        if (fileModification != null) {
            summary["File Modifications"] = "${fileModification.callCount} changes, avg ${fileModification.averageTime.toInt()}ms"
        }
        
        // Game scanning
        val gameScanning = getMetric("game_scanning")
        if (gameScanning != null) {
            summary["Game Scanning"] = "${gameScanning.callCount} scans, avg ${gameScanning.averageTime.toInt()}ms"
        }
        
        // Memory usage
        val runtime = Runtime.getRuntime()
        val usedMemory = runtime.totalMemory() - runtime.freeMemory()
        val usedMB = usedMemory / (1024 * 1024)
        summary["Memory Usage"] = "${usedMB}MB"
        
        return summary
    }
}