package com.wingedsheep.mtg.sets.definitions.rna.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Rakdos, the Showstopper — Ravnica Allegiance #199
 * {4}{B}{R} · Legendary Creature — Demon · 6 / 6
 *
 * One coin per non-Demon/Devil/Imp creature, flipped one creature at a time
 * (`forEachInGroupCollecting`), each creature recorded only when its coin comes up tails
 * (`storeEntity(onlyIf = …)`); then every recorded creature is destroyed in one simultaneous
 * destroy, after all the flips. Rakdos is a Demon, so it never flips for itself.
 */
val RakdosTheShowstopper = card("Rakdos, the Showstopper") {
    manaCost = "{4}{B}{R}"
    colorIdentity = "BR"
    typeLine = "Legendary Creature — Demon"
    power = 6
    toughness = 6
    oracleText = "Flying, trample\n" +
        "When Rakdos enters, flip a coin for each creature that isn't a Demon, Devil, or Imp. " +
        "Destroy each creature whose coin comes up tails."

    keywords(Keyword.FLYING, Keyword.TRAMPLE)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.Pipeline {
            val (tails) = forEachInGroupCollecting(
                GroupFilter(
                    GameObjectFilter.Creature
                        .notSubtype(Subtype.DEMON)
                        .notSubtype(Subtype.DEVIL)
                        .notSubtype(Subtype.IMP)
                )
            ) {
                val heads = runStoringNumber { Effects.FlipCoins(1, storeHeadsAs = it) }
                listOf(storeEntity(onlyIf = Conditions.CompareAmounts(heads.amount, ComparisonOperator.EQ, 0)))
            }
            destroy(tails)
        }
        description = "When Rakdos enters, flip a coin for each creature that isn't a Demon, Devil, " +
            "or Imp. Destroy each creature whose coin comes up tails."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "199"
        artist = "Viktor Titov"
        flavorText = "\"Entertain me.\""
        imageUri = "https://cards.scryfall.io/normal/front/4/e/4e3c30c7-c52e-41a0-b7c2-21d39c05160b.jpg?1783933639"
    }
}
