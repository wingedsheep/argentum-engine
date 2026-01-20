package com.wingedsheep.rulesengine.sets.ecl.cards

import com.wingedsheep.rulesengine.ability.AbilityCost
import com.wingedsheep.rulesengine.ability.EffectTarget
import com.wingedsheep.rulesengine.ability.TapUntapEffect
import com.wingedsheep.rulesengine.ability.cardScript
import com.wingedsheep.rulesengine.card.CardDefinition
import com.wingedsheep.rulesengine.card.Rarity
import com.wingedsheep.rulesengine.card.ScryfallMetadata
import com.wingedsheep.rulesengine.core.ManaCost
import com.wingedsheep.rulesengine.core.Subtype

/**
 * Wanderbrine Trapper
 *
 * {W} Creature — Merfolk Scout 2/1
 * {1}, {T}, Tap another untapped creature you control: Tap target creature an opponent controls.
 */
object WanderbrineTrapper {
    val definition = CardDefinition.creature(
        name = "Wanderbrine Trapper",
        manaCost = ManaCost.parse("{W}"),
        subtypes = setOf(Subtype.MERFOLK, Subtype.SCOUT),
        power = 2,
        toughness = 1,
        oracleText = "{1}, {T}, Tap another untapped creature you control: Tap target creature an opponent controls.",
        metadata = ScryfallMetadata(
            collectorNumber = "42",
            rarity = Rarity.UNCOMMON,
            artist = "Iris Compiet",
            imageUri = "https://cards.scryfall.io/normal/front/c/c/cc9c9c9c-9c9c-9c9c-9c9c-9c9c9c9c9c9c.jpg",
            releaseDate = "2026-01-23"
        )
    )

    val script = cardScript("Wanderbrine Trapper") {
        // {1}, {T}, Tap another untapped creature you control: Tap target creature an opponent controls
        activated(
            cost = AbilityCost.Composite(
                costs = listOf(
                    AbilityCost.Mana(generic = 1),
                    AbilityCost.Tap,
                    AbilityCost.TapOtherCreature()
                )
            ),
            effect = TapUntapEffect(
                target = EffectTarget.TargetOpponentCreature,
                tap = true
            )
        )
    }
}
