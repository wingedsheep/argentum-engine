package com.wingedsheep.sdk.serialization

import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.scripting.AbilityId

/**
 * Post-deserialization processing to assign fresh AbilityIds.
 *
 * Card JSON should not contain ability IDs — they're runtime-generated.
 * After deserializing a CardDefinition, call [withGeneratedIds] to walk
 * the tree and replace any placeholder/deserialized IDs with fresh ones.
 */
fun CardDefinition.withGeneratedIds(): CardDefinition {
    val newTriggeredAbilities = script.triggeredAbilities.map {
        it.copy(id = AbilityId.generate())
    }
    val newActivatedAbilities = script.activatedAbilities.map {
        it.copy(id = AbilityId.generate())
    }

    val newScript = script.copy(
        triggeredAbilities = newTriggeredAbilities,
        activatedAbilities = newActivatedAbilities
    )

    val newBackFace = backFace?.withGeneratedIds()

    return copy(
        script = newScript,
        backFace = newBackFace
    )
}
