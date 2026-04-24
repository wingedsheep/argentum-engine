package com.wingedsheep.mtg.sets.definitions.lorwyneclipsed.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.MoveToZoneEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Crib Swap
 * {2}{W}
 * Kindred Instant — Shapeshifter
 *
 * Changeling (This card is every creature type.)
 * Exile target creature. Its controller creates a 1/1 colorless Shapeshifter
 * creature token with changeling.
 */
val CribSwap = card("Crib Swap") {
    manaCost = "{2}{W}"
    typeLine = "Kindred Instant — Shapeshifter"
    oracleText = "Changeling (This card is every creature type.)\n" +
        "Exile target creature. Its controller creates a 1/1 colorless Shapeshifter creature token with changeling."

    keywords(Keyword.CHANGELING)

    spell {
        val creature = target("creature", Targets.Creature)
        effect = MoveToZoneEffect(creature, Zone.EXILE)
            .then(
                Effects.CreateToken(
                    power = 1,
                    toughness = 1,
                    creatureTypes = setOf("Shapeshifter"),
                    keywords = setOf(Keyword.CHANGELING),
                    controller = EffectTarget.TargetController,
                    imageUri = "https://cards.scryfall.io/normal/front/c/2/c2963ce1-f9d8-437a-9489-e0913a8b8d26.jpg?1767660071"
                )
            )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "11"
        artist = "Pete Venters"
        flavorText = "Elsewhere in Lorwyn, nestled in a faerie ring, the coos of a small giant turned to cries."
        imageUri = "https://cards.scryfall.io/normal/front/8/f/8f2fb3c6-af75-47a3-9f97-521872c32890.jpg?1767732469"
        ruling("2020-08-07", "Kindred is a card type (like creature or instant), not a supertype (like legendary).")
        ruling("2020-08-07", "Changeling applies in all zones, not just the battlefield.")
        ruling("2020-08-07", "If the target creature is an illegal target by the time Crib Swap tries to resolve, the spell won't resolve. No player will create a Shapeshifter token.")
    }
}
