package com.wingedsheep.mtg.sets.definitions.akh.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.namedFromVariable
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CardNamePool
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource

/**
 * Dispossess — Amonkhet #86
 * {2}{B} · Sorcery
 *
 * Choose an artifact card name. Search target opponent's graveyard, hand, and library for any
 * number of cards with the chosen name and exile them. Then that player shuffles.
 *
 * Necromentia's search-and-pick shape with the name drawn from the artifact pool: only artifact
 * card names are offered, and the caster picks any number of the found cards to exile.
 */
val Dispossess = card("Dispossess") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Choose an artifact card name. Search target opponent's graveyard, hand, and library for " +
        "any number of cards with the chosen name and exile them. Then that player shuffles."

    spell {
        val opponent = target(Targets.Opponent)
        effect = Effects.Pipeline {
            val chosenName = chooseCardName(pool = CardNamePool.ARTIFACT)
            val matches = gather(
                CardSource.FromMultipleZones(
                    zones = listOf(Zone.GRAVEYARD, Zone.HAND, Zone.LIBRARY),
                    player = opponent.asPlayer,
                    filter = GameObjectFilter.Any.namedFromVariable(chosenName),
                ),
                search = true,
            )
            val selected = chooseAnyNumber(
                from = matches,
                prompt = "Choose any number of cards with that name to exile",
                showAllCards = true,
                alwaysPrompt = true,
            )
            exile(selected, owner = opponent.asPlayer)
            run(Effects.ShuffleLibrary(target = opponent))
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "86"
        artist = "Mark Behm"
        imageUri = "https://cards.scryfall.io/normal/front/4/e/4eb88be3-f790-45d7-a6d1-c78f6ba00208.jpg?1783936508"
    }
}
