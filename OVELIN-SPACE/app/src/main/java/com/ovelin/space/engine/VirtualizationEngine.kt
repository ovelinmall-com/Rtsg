package com.ovelin.space.engine

import com.ovelin.space.data.ImportedAppEntity
import com.ovelin.space.data.VirtualInstanceEntity

data class EngineStatus(
    val available: Boolean,
    val engineName: String,
    val explanation: String,
)

sealed interface EngineResult {
    data class Completed(val message: String) : EngineResult
    data class Unsupported(val reason: String) : EngineResult
}

/** Optional per-instance device identity shown to the cloned app. Null means "real device". */
data class DeviceIdentity(val manufacturer: String, val model: String)

/**
 * Implementations must run packages in a genuinely isolated Android runtime.
 * A database row or duplicate launcher icon is not a virtual app instance.
 */
interface VirtualizationEngine {
    val status: EngineStatus

    /** How many more instances fit given free storage; the count is resource-bound, not unlimited. */
    suspend fun remainingCapacity(): Int

    suspend fun createInstance(app: ImportedAppEntity, displayName: String, identity: DeviceIdentity?): EngineResult
    suspend fun launchInstance(instance: VirtualInstanceEntity): EngineResult
    suspend fun stopInstance(instance: VirtualInstanceEntity): EngineResult
    suspend fun deleteInstance(instance: VirtualInstanceEntity): EngineResult
    fun hasIdentity(instance: VirtualInstanceEntity): Boolean

    /** Human-readable self-check of the runtime on this device, including recent engine errors. */
    suspend fun diagnostics(): String
}
