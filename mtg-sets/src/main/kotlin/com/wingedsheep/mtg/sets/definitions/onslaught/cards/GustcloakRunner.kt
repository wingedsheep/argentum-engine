package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EffectTarget
import com.wingedsheep.sdk.scripting.MayEffect

/**
 * Gustcloak Runner
 * {W}
 * Creature — Human Soldier
 * 1/1
 * Whenever Gustcloak Runner becomes blocked, you may untap it and remove it from combat.
 */
val GustcloakRunner = card("Gustcloak Runner") {
    manaCost = "{W}"
    typeLine = "Creature — Human Soldier"
    power = 1
    toughness = 1

    triggeredAbility {
        trigger = Triggers.BecomesBlocked
        effect = MayEffect(
            Effects.Untap(EffectTarget.Self) then Effects.RemoveFromCombat(EffectTarget.Self)
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "35"
        artist = "Matt Cavotta"
        flavorText = "\"Troops are fruit to be plucked from the field of battle.\"\n—Gustcloak Savior"
        imageUri = "https://cards.scryfall.io/large/front/e/b/eb227f65-9189-41ed-94a0-2aa21cad26f5.jpg?1562950998"
    }
}
