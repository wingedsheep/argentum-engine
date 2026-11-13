package com.wingedsheep.mtg.sets.definitions.bfz.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.predicates.CardPredicate

/**
 * Ruination Guide
 * {2}{U}
 * Creature — Eldrazi Drone
 * 3/2
 * Devoid (This card has no color.)
 * Ingest (Whenever this creature deals combat damage to a player, that player exiles the top card of their library.)
 * Other colorless creatures you control get +1/+0.
 *
 * The lord reads projected colour, so devoid creatures (colorless only through their devoid
 * colour-setting effect) and colorless artifact creatures both get the bonus.
 */
val RuinationGuide = card("Ruination Guide") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Eldrazi Drone"
    power = 3
    toughness = 2
    oracleText = "Devoid (This card has no color.)\n" +
        "Ingest (Whenever this creature deals combat damage to a player, that player exiles the top card of their library.)\n" +
        "Other colorless creatures you control get +1/+0."

    keywords(Keyword.DEVOID, Keyword.INGEST)

    staticAbility {
        ability = ModifyStats(
            powerBonus = 1,
            toughnessBonus = 0,
            filter = GroupFilter(
                GameObjectFilter.Creature.withCardPredicate(CardPredicate.IsColorless).youControl(),
                excludeSelf = true
            )
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "64"
        artist = "Mathias Kollros"
        imageUri = "https://cards.scryfall.io/normal/front/f/0/f0bdbe64-d985-4bc5-bb1d-4cbf12ef60b2.jpg?1783938211"
        ruling("2015-08-25", "The card exiled by the ingest ability is exiled face up.")
        ruling("2015-08-25", "If the player has no cards in their library when the ingest ability resolves, nothing happens. That player won't lose the game (until they have to draw a card from an empty library).")
    }
}
