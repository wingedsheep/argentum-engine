package com.wingedsheep.mtg.sets.definitions.lorwyneclipsed.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.effects.CreateDelayedTriggerEffect
import com.wingedsheep.sdk.scripting.effects.CreateTokenEffect
import com.wingedsheep.sdk.scripting.effects.DelayedTriggerExpiry

/**
 * Scarblade's Malice
 * {B}
 * Instant
 *
 * Target creature you control gains deathtouch and lifelink until end of turn.
 * When that creature dies this turn, create a 2/2 black and green Elf creature token.
 */
val ScarbladesMalice = card("Scarblade's Malice") {
    manaCost = "{B}"
    typeLine = "Instant"
    oracleText = "Target creature you control gains deathtouch and lifelink until end of turn. " +
        "When that creature dies this turn, create a 2/2 black and green Elf creature token."

    spell {
        val creature = target("creature you control", Targets.CreatureYouControl)
        effect = CompositeEffect(
            listOf(
                Effects.GrantKeyword(Keyword.DEATHTOUCH, creature),
                Effects.GrantKeyword(Keyword.LIFELINK, creature),
                CreateDelayedTriggerEffect(
                    trigger = Triggers.Dies,
                    watchedTarget = creature,
                    expiry = DelayedTriggerExpiry.EndOfTurn,
                    effect = CreateTokenEffect(
                        count = 1,
                        power = 2,
                        toughness = 2,
                        colors = setOf(Color.BLACK, Color.GREEN),
                        creatureTypes = setOf("Elf"),
                        imageUri = "https://cards.scryfall.io/normal/front/3/9/39b36f22-21f9-44fe-8a49-bdc859503342.jpg?1767955588"
                    )
                )
            )
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "119"
        artist = "Quintin Gleim"
        flavorText = "The poison on their blades is nothing compared to the poison in their hearts."
        imageUri = "https://cards.scryfall.io/normal/front/a/e/aea9b5c0-3b32-44be-9773-566b9daafa6b.jpg?1767957161"
    }
}
