package com.wingedsheep.mtg.sets.definitions.dgm.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.unaryMinus
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Warped Physique
 * {U}{B}
 * Instant
 * Target creature gets +X/-X until end of turn, where X is the number of cards in your hand.
 *
 * Nightmarish End's shape with the power sign flipped: X is read once on resolution (the spell is
 * on the stack, not in hand) and locked in.
 */
val WarpedPhysique = card("Warped Physique") {
    manaCost = "{U}{B}"
    colorIdentity = "UB"
    typeLine = "Instant"
    oracleText = "Target creature gets +X/-X until end of turn, where X is the number of cards in your hand."

    spell {
        val t = target(TargetFilter.Creature)
        val handCount = DynamicAmounts.cardsInYourHand()
        effect = Effects.ModifyStats(power = handCount, toughness = -handCount, target = t)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "117"
        artist = "Karl Kopinski"
        flavorText = "The Gruul seek unbounded strength. The Dimir deliver cruel irony."
        imageUri = "https://cards.scryfall.io/normal/front/1/3/134802b2-7c5c-4eda-a879-b29bc06faaed.jpg?1783940018"
        ruling(
            "2013-04-15",
            "Use the number of cards in your hand as Warped Physique resolves to determine the value of X. Warped Physique won't be in your hand at that time."
        )
        ruling("2013-04-15", "Once Warped Physique resolves, the effect won't change if the number of cards in your hand changes.")
    }
}
