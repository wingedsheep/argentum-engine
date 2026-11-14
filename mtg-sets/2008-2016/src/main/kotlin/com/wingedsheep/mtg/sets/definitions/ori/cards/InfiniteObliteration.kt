package com.wingedsheep.mtg.sets.definitions.ori.cards

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
 * Infinite Obliteration — Magic Origins #103
 * {1}{B}{B} · Sorcery
 *
 * Choose a creature card name. Search target opponent's graveyard, hand, and library for any
 * number of cards with that name and exile them. Then that player shuffles.
 *
 * Lost Legacy's shape with the creature name pool, an opponent-only target and no draw. The name
 * is chosen on resolution; "any number" lets the caster leave matches behind.
 */
val InfiniteObliteration = card("Infinite Obliteration") {
    manaCost = "{1}{B}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Choose a creature card name. Search target opponent's graveyard, hand, and library " +
        "for any number of cards with that name and exile them. Then that player shuffles."

    spell {
        val opponent = target(Targets.Opponent)
        effect = Effects.Pipeline {
            val chosenName = chooseCardName(pool = CardNamePool.CREATURE)
            val matches = gather(
                CardSource.FromMultipleZones(
                    zones = listOf(Zone.GRAVEYARD, Zone.HAND, Zone.LIBRARY),
                    player = opponent.asPlayer,
                    filter = GameObjectFilter.Any.namedFromVariable(chosenName)
                ),
                search = true
            )
            val toExile = chooseAnyNumber(
                from = matches,
                prompt = "Choose any number of cards with that name to exile",
                selectedLabel = "Exile"
            )
            exile(toExile, owner = opponent.asPlayer)
            run(Effects.ShuffleLibrary(target = opponent))
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "103"
        artist = "Yeong-Hao Han"
        imageUri = "https://cards.scryfall.io/normal/front/7/1/71a6f6a0-3670-4b72-8c8e-668f47252fa0.jpg?1783938341"
    }
}
