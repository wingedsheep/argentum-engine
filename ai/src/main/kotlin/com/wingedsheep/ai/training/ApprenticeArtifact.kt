package com.wingedsheep.ai.training

import com.wingedsheep.ai.engine.evaluation.RawBoardFeatures
import com.wingedsheep.ai.engine.evaluation.RawEvaluationWeights
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

const val APPRENTICE_ARTIFACT_SCHEMA_VERSION = 1

/** Small, immutable JVM artifact produced offline; card identities are deliberately absent. */
@Serializable
data class ApprenticeArtifact(
    val schemaVersion: Int = APPRENTICE_ARTIFACT_SCHEMA_VERSION,
    val modelId: String,
    val setCode: String? = null,
    val featureNames: List<String>,
    val sharedCoefficients: List<Double>,
    val setOverlayCoefficients: List<Double> = emptyList(),
    val intercept: Double = 0.0,
) {
    fun toEvaluationWeights(applyOverlay: Boolean): RawEvaluationWeights {
        require(validationErrors().isEmpty())
        val coefficients = featureNames.indices.associate { index ->
            val overlay = if (applyOverlay) setOverlayCoefficients.getOrElse(index) { 0.0 } else 0.0
            featureNames[index] to (sharedCoefficients[index] + overlay)
        }
        return RawEvaluationWeights(intercept = intercept, weights = coefficients)
    }
    fun validationErrors(): List<String> = buildList {
        if (schemaVersion != APPRENTICE_ARTIFACT_SCHEMA_VERSION) add("unsupported schema version")
        if (modelId.isBlank()) add("blank model id")
        if (featureNames.toSet() != RawBoardFeatures.names || featureNames.size != RawBoardFeatures.names.size) {
            add("feature schema mismatch")
        }
        if (sharedCoefficients.size != featureNames.size) add("shared coefficient count mismatch")
        if (setOverlayCoefficients.isNotEmpty() && setOverlayCoefficients.size != featureNames.size) {
            add("overlay coefficient count mismatch")
        }
        if (!intercept.isFinite() || sharedCoefficients.any { !it.isFinite() } || setOverlayCoefficients.any { !it.isFinite() }) {
            add("non-finite model value")
        }
        if (setOverlayCoefficients.isNotEmpty() && setCode.isNullOrBlank()) add("overlay requires set code")
    }
}

object ApprenticeArtifactLoader {
    private val json = Json { ignoreUnknownKeys = false }

    fun decodeOrNull(text: String, expectedSet: String? = null): ApprenticeArtifact? = runCatching {
        json.decodeFromString<ApprenticeArtifact>(text)
    }.getOrNull()?.takeIf { artifact ->
        artifact.validationErrors().isEmpty() &&
            (artifact.setCode == null || expectedSet?.uppercase() == artifact.setCode.uppercase())
    }
}
