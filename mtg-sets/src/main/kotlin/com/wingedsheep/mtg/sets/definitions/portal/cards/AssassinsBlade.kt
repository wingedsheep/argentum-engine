package com.wingedsheep.mtg.sets.definitions.portal.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.DestroyEffect
import com.wingedsheep.sdk.scripting.EffectTarget
import com.wingedsheep.sdk.targeting.CreatureTargetFilter
import com.wingedsheep.sdk.targeting.TargetCreature

/**
 * Assassin's Blade
 * {1}{B}
 * Instant
 * Cast this spell only during the declare attackers step and only if you've been attacked this step.
 * Destroy target nonblack attacking creature.
 */
val AssassinsBlade = card("Assassin's Blade") {
    manaCost = "{1}{B}"
    typeLine = "Instant"

    spell {
        target = TargetCreature(
            filter = CreatureTargetFilter.And(
                listOf(
                    CreatureTargetFilter.Attacking,
                    CreatureTargetFilter.NotColor(Color.BLACK)
                )
            )
        )
        effect = DestroyEffect(EffectTarget.ContextTarget(0))
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "80"
        artist = "Mark Poole"
        imageUri = "https://cards.scryfall.io/normal/front/7/9/79e0d68b-5c76-4d41-a5c8-3b34f0d7ac24.jpg"
    }
}
