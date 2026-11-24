package com.wingedsheep.mtg.sets.definitions.rtr.cards

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
 * Slaughter Games — Return to Ravnica #197
 * {2}{B}{R} · Sorcery
 *
 * This spell can't be countered.
 * Choose a nonland card name. Search target opponent's graveyard, hand, and library for any number
 * of cards with that name and exile them. Then that player shuffles.
 *
 * Stain the Mind's shape (nonland name pool, "any number" selection) aimed at an opponent, with the
 * intrinsic [cantBeCountered] flag.
 *
 * Canonical printing: Return to Ravnica, the card's earliest real printing.
 */
val SlaughterGames = card("Slaughter Games") {
    manaCost = "{2}{B}{R}"
    colorIdentity = "BR"
    typeLine = "Sorcery"
    oracleText = "This spell can't be countered.\n" +
        "Choose a nonland card name. Search target opponent's graveyard, hand, and library for any " +
        "number of cards with that name and exile them. Then that player shuffles."

    cantBeCountered = true

    spell {
        val opponent = target(Targets.Opponent)
        effect = Effects.Pipeline {
            val chosenName = chooseCardName(pool = CardNamePool.NONLAND)
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
        collectorNumber = "197"
        artist = "Steve Prescott"
        imageUri = "https://cards.scryfall.io/normal/front/b/f/bf37391d-db35-40a7-908a-abb53895793c.jpg?1783940332"
    }
}
