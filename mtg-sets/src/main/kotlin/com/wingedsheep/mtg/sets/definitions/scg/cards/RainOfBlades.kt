package com.wingedsheep.mtg.sets.definitions.scg.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.DealDamageEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.dsl.Effects

/**
 * Rain of Blades
 * {W}
 * Instant
 * Rain of Blades deals 1 damage to each attacking creature.
 */
val RainOfBlades = card("Rain of Blades") {
    manaCost = "{W}"
    colorIdentity = "W"
    typeLine = "Instant"
    oracleText = "Rain of Blades deals 1 damage to each attacking creature."

    spell {
        effect = Effects.ForEachInGroup(
            filter = GroupFilter.AttackingCreatures,
            effect = DealDamageEffect(1, EffectTarget.Self)
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "20"
        artist = "Rob Alexander"
        flavorText = "Some say they are the weapons of heroes fallen in battle, eager for one last chance at glory."
        imageUri = "https://cards.scryfall.io/normal/front/4/1/418476cd-94da-47a5-ba77-6bb4771e9c89.jpg?1562528049"
    }
}
