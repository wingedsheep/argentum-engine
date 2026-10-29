package com.wingedsheep.mtg.sets.definitions.m21.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.namedFromVariable
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource

/**
 * Necromentia
 * {1}{B}{B}
 * Sorcery
 * Choose a card name other than a basic land card name. Search target opponent's graveyard, hand,
 * and library for any number of cards with that name and exile them. That player shuffles, then
 * creates a 2/2 black Zombie creature token for each card exiled from their hand this way.
 *
 * Cranial Extraction's name-and-search pipeline (Desperate Research's `excludeBasicLandNames`
 * naming) with The End's "any number of" selection: the caster picks from every matching card,
 * and the hand subset is filtered *before* the exile so its count survives the move. The tokens
 * are created under the targeted opponent's control.
 */
val Necromentia = card("Necromentia") {
    manaCost = "{1}{B}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Choose a card name other than a basic land card name. Search target opponent's graveyard, " +
        "hand, and library for any number of cards with that name and exile them. That player shuffles, then " +
        "creates a 2/2 black Zombie creature token for each card exiled from their hand this way."

    spell {
        val opponent = target(Targets.Opponent)
        effect = Effects.Pipeline {
            val chosenName = chooseCardName(
                prompt = "Choose a card name other than a basic land card name",
                excludeBasicLandNames = true,
            )
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
            val selectedFromHand = filter(selected, GameObjectFilter.Any.currentlyIn(Zone.HAND))
            exile(selected, owner = opponent.asPlayer)
            run(Effects.ShuffleLibrary(target = opponent))
            run(
                Effects.CreateToken(
                    count = selectedFromHand.count,
                    power = 2,
                    toughness = 2,
                    colors = setOf(Color.BLACK),
                    creatureTypes = setOf("Zombie"),
                    controller = opponent,
                    imageUri = "https://cards.scryfall.io/normal/front/3/f/3f170f84-6c36-43e2-91e6-3c7a136944b1.jpg?1783930590",
                ),
            )
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "116"
        artist = "Mila Pesic"
        imageUri = "https://cards.scryfall.io/normal/front/3/2/32c5252e-ff15-4f86-ad63-d8286427e70f.jpg?1783930700"
    }
}
