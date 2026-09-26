package com.wingedsheep.mtg.sets.definitions.chk.cards

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
 * Cranial Extraction — Champions of Kamigawa #105
 * {3}{B} · Sorcery — Arcane
 *
 * Choose a nonland card name. Search target player's graveyard, hand, and library for all cards
 * with that name and exile them. Then that player shuffles.
 *
 * Ancient Vendetta's shape with two differences: the name comes from the *nonland* pool (land
 * names are never offered), and every match is exiled rather than up to four. The name is chosen
 * on resolution, so it can't be named in response to anything the target does with the spell on
 * the stack. The target is any player — naming a card in your own deck is legal.
 */
val CranialExtraction = card("Cranial Extraction") {
    manaCost = "{3}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery — Arcane"
    oracleText = "Choose a nonland card name. Search target player's graveyard, hand, and library for " +
        "all cards with that name and exile them. Then that player shuffles."

    spell {
        val player = target(Targets.Player)
        effect = Effects.Pipeline {
            val chosenName = chooseCardName(pool = CardNamePool.NONLAND)
            val matches = gather(
                CardSource.FromMultipleZones(
                    zones = listOf(Zone.GRAVEYARD, Zone.HAND, Zone.LIBRARY),
                    player = player.asPlayer,
                    filter = GameObjectFilter.Any.namedFromVariable(chosenName)
                ),
                search = true
            )
            exile(matches, owner = player.asPlayer)
            run(Effects.ShuffleLibrary(target = player))
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "105"
        artist = "Dave Allsop"
        imageUri = "https://cards.scryfall.io/normal/front/a/8/a8cad7a3-777e-4c84-b404-5f504844ab3b.jpg?1783944317"
    }
}
