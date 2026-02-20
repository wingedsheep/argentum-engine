package com.wingedsheep.mtg.sets.definitions.portal.cards

import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.effects.SkipCombatPhasesEffect

/**
 * False Peace
 * {W}
 * Sorcery
 * Target player skips all combat phases of their next turn.
 */
val FalsePeace = card("False Peace") {
    manaCost = "{W}"
    typeLine = "Sorcery"

    spell {
        target = Targets.Player
        effect = SkipCombatPhasesEffect(EffectTarget.ContextTarget(0))
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "14"
        artist = "Zina Saunders"
        flavorText = "Mutual consent is not required for war."
        imageUri = "https://cards.scryfall.io/normal/front/d/4/d4234262-56c6-4bd1-b425-12db931829d5.jpg"
    }
}
