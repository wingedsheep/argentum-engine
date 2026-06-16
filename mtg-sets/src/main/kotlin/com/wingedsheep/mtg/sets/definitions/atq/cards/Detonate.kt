package com.wingedsheep.mtg.sets.definitions.atq.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Detonate
 * {X}{R}
 * Sorcery
 * Destroy target artifact with mana value X. It can't be regenerated.
 * Detonate deals X damage to that artifact's controller.
 *
 * X is chosen as the spell is cast (paid as part of its {X}{R} mana cost). The targeted
 * artifact must have mana value exactly X (TargetFilter.manaValueEqualsX, the same
 * chosen-number target restriction as Repeal / Spell Blast). The X damage is dealt to the
 * artifact's controller ([EffectTarget.TargetController]) *before* the destroy, so the
 * controller is read while the artifact is still on the battlefield (mirrors Crumble); the
 * destroy then composes no-regeneration via [Effects.Destroy] `noRegenerate = true`.
 */
val Detonate = card("Detonate") {
    manaCost = "{X}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Destroy target artifact with mana value X. It can't be regenerated. " +
        "Detonate deals X damage to that artifact's controller."

    spell {
        val artifact = target(
            "artifact with mana value X",
            TargetObject(filter = TargetFilter.Artifact.manaValueEqualsX())
        )
        effect = Effects.DealDamage(DynamicAmount.XValue, EffectTarget.TargetController)
            .then(Effects.Destroy(artifact, noRegenerate = true))
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "24"
        artist = "Randy Asplund-Faith"
        imageUri = "https://cards.scryfall.io/normal/front/f/f/ffd7eb90-ae95-49df-898a-9510187bce1c.jpg?1562949167"
    }
}
