package com.wingedsheep.mtg.sets.definitions.m15.cards

import com.wingedsheep.sdk.core.Keyword
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
 * Stain the Mind — Magic 2015 #117
 * {4}{B} · Sorcery
 *
 * Convoke
 * Choose a nonland card name. Search target player's graveyard, hand, and library for any number
 * of cards with that name and exile them. Then that player shuffles.
 *
 * Cranial Extraction's nonland name pool with Lost Legacy's "any number" selection: every match is
 * gathered, then the caster picks which of them to exile and may leave the rest where they are.
 *
 * Canonical printing: Magic 2015, the card's earliest real-expansion printing.
 */
val StainTheMind = card("Stain the Mind") {
    manaCost = "{4}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Convoke (Your creatures can help cast this spell. Each creature you tap while " +
        "casting this spell pays for {1} or one mana of that creature's color.)\n" +
        "Choose a nonland card name. Search target player's graveyard, hand, and library for any " +
        "number of cards with that name and exile them. Then that player shuffles."

    keywords(Keyword.CONVOKE)

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
            val toExile = chooseAnyNumber(
                from = matches,
                prompt = "Choose any number of cards with that name to exile",
                selectedLabel = "Exile"
            )
            exile(toExile, owner = player.asPlayer)
            run(Effects.ShuffleLibrary(target = player))
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "117"
        artist = "Jason Rainville"
        imageUri = "https://cards.scryfall.io/normal/front/c/7/c7d2b046-59bc-4e39-94ce-981179307bd3.jpg?1783939180"
        ruling(
            "2014-07-18",
            "You can leave any cards with that name in the zone they're in. You don't have to exile them."
        )
    }
}
