package com.wingedsheep.mtg.sets.definitions.znr.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Shadows' Verdict — Zendikar Rising #124
 * {3}{B}{B} · Sorcery
 *
 * Exile all creatures and planeswalkers with mana value 3 or less from the battlefield and all
 * creature and planeswalker cards with mana value 3 or less from all graveyards.
 *
 * The Farewell / Rest in Peace Gather -> exile pipeline, once over the battlefield and once over
 * every graveyard. Exiling the permanents puts nothing into a graveyard, so the second gather sees
 * exactly the graveyards the spell started with.
 */
val ShadowsVerdict = card("Shadows' Verdict") {
    manaCost = "{3}{B}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Exile all creatures and planeswalkers with mana value 3 or less from the battlefield " +
        "and all creature and planeswalker cards with mana value 3 or less from all graveyards."

    spell {
        effect = Effects.Pipeline {
            exile(gather(GameObjectFilter.CreatureOrPlaneswalker.manaValueAtMost(3)))
            exile(
                gather(
                    CardSource.FromZone(
                        Zone.GRAVEYARD,
                        Player.Each,
                        GameObjectFilter.CreatureOrPlaneswalker.manaValueAtMost(3)
                    )
                )
            )
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "124"
        artist = "Lius Lasahido"
        flavorText = "The shadows of death fall over everyone in the end."
        imageUri = "https://cards.scryfall.io/normal/front/5/2/52470883-b44d-415b-9324-8074e66f79ae.jpg?1783929366"
        ruling("2020-09-25", "If a permanent on the battlefield or a card in a graveyard has {X} in its mana cost, X is considered to be 0.")
    }
}
