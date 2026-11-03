package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Hemmer, Fixing What Is Broken
 * {W}{U}
 * Legendary Creature — Aenar Artificer
 * 2/3
 *
 * During your turn, Spacecraft you control have hexproof.
 * Whenever Hemmer becomes tapped, draw a card, then discard a card.
 */
val HemmerFixingWhatIsBroken = card("Hemmer, Fixing What Is Broken") {
    manaCost = "{W}{U}"
    typeLine = "Legendary Creature — Aenar Artificer"
    colorIdentity = "WU"
    oracleText = "During your turn, Spacecraft you control have hexproof. (They can't be the targets of " +
        "spells or abilities your opponents control.)\n" +
        "Whenever Hemmer becomes tapped, draw a card, then discard a card."
    power = 2
    toughness = 3

    staticAbility {
        condition = Conditions.IsYourTurn
        ability = GrantKeyword(
            Keyword.HEXPROOF,
            GroupFilter(GameObjectFilter.Permanent.withSubtype("Spacecraft").youControl())
        )
    }

    triggeredAbility {
        trigger = Triggers.self.becomesTapped()
        effect = Patterns.Hand.loot(draw = 1, discard = 1)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "239"
        artist = "Riccardo Federici"
        flavorText = "\"I will not fight for Starfleet, but I will defend its ideals. Pacifism is not passivity.\""
        imageUri = "https://cards.scryfall.io/normal/front/f/3/f3e730d7-3fd1-4bd2-a35b-6fb85bfd3c65.jpg?1792550956"
    }
}
