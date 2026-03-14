package com.wingedsheep.mtg.sets.definitions.dominaria.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.predicates.CardPredicate

/**
 * Phyrexian Scriptures
 * {2}{B}{B}
 * Enchantment — Saga
 *
 * (As this Saga enters and after your draw step, add a lore counter. Sacrifice after III.)
 * I — Put a +1/+1 counter on up to one target creature. That creature becomes an artifact
 *     in addition to its other types.
 * II — Destroy all nonartifact creatures.
 * III — Exile all opponents' graveyards.
 */
val PhyrexianScriptures = card("Phyrexian Scriptures") {
    manaCost = "{2}{B}{B}"
    typeLine = "Enchantment — Saga"
    oracleText = "(As this Saga enters and after your draw step, add a lore counter. Sacrifice after III.)\n" +
        "I — Put a +1/+1 counter on up to one target creature. That creature becomes an artifact in addition to its other types.\n" +
        "II — Destroy all nonartifact creatures.\n" +
        "III — Exile all opponents' graveyards."

    sagaChapter(1) {
        val creature = target("creature", Targets.UpToCreatures(1))
        effect = CompositeEffect(listOf(
            Effects.AddCounters("+1/+1", 1, creature),
            Effects.AddCardType("ARTIFACT", creature)
        ))
    }

    sagaChapter(2) {
        effect = Effects.DestroyAll(
            GameObjectFilter(
                cardPredicates = listOf(
                    CardPredicate.IsCreature,
                    CardPredicate.Not(CardPredicate.IsArtifact)
                )
            )
        )
    }

    sagaChapter(3) {
        effect = Effects.ExileOpponentsGraveyards()
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "100"
        artist = "Joseph Meehan"
        imageUri = "https://cards.scryfall.io/normal/front/7/f/7f3423d7-cb81-47bf-b9a6-a279ba6cedf4.jpg?1562738458"
    }
}
