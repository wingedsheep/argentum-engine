package com.wingedsheep.mtg.sets.definitions.mid.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.dsl.LibraryPatterns

/**
 * Fading Hope
 * {U}
 * Instant
 * Return target creature to its owner's hand. If its mana value was 3 or less, scry 1.
 */
val FadingHope = card("Fading Hope") {
    manaCost = "{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Return target creature to its owner's hand. If its mana value was 3 or less, scry 1. (Look at the top card of your library. You may put that card on the bottom.)"

    spell {
        val creature = target("target creature", Targets.Creature)
        effect = Effects.ReturnToHand(creature)
            .then(
                ConditionalEffect(
                    condition = Conditions.TargetSpellManaValueAtMost(DynamicAmount.Fixed(3)),
                    effect = LibraryPatterns.scry(1)
                )
            )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "51"
        artist = "Rovina Cai"
        flavorText = "\"At least I won't become one of . . . those things.\""
        imageUri = "https://cards.scryfall.io/normal/front/c/2/c2fb1fff-12be-4bd5-8dba-c36e84d49651.jpg?1634348819"
    }
}
