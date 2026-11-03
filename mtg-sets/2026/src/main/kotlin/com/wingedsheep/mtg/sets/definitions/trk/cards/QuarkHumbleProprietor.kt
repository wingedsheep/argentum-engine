package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantBeSacrificed
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Quark, Humble Proprietor
 * {2}{R}
 * Legendary Creature — Ferengi Rogue
 * 1/2
 *
 * You can't sacrifice Treasure tokens.
 * At the beginning of each upkeep, create a Treasure token.
 *
 * Only a permanent's controller can sacrifice it, so "you can't sacrifice Treasure tokens" is
 * exactly "Treasure tokens you control can't be sacrificed" — by an effect or to pay a cost,
 * which shuts off the Treasures' own "{T}, Sacrifice this token" mana ability too. Opponents'
 * Treasures are untouched.
 */
val QuarkHumbleProprietor = card("Quark, Humble Proprietor") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Legendary Creature — Ferengi Rogue"
    power = 1
    toughness = 2
    oracleText = "You can't sacrifice Treasure tokens.\n" +
        "At the beginning of each upkeep, create a Treasure token. " +
        "(It's an artifact with \"{T}, Sacrifice this token: Add one mana of any color.\")"

    staticAbility {
        ability = CantBeSacrificed(
            filter = GroupFilter(GameObjectFilter.Token.withSubtype(Subtype.TREASURE).youControl())
        )
    }

    triggeredAbility {
        trigger = Triggers.anyPlayer.beginningOf(Step.UPKEEP)
        effect = Effects.CreateTreasure(1)
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "166"
        artist = "Alex Negrea"
        flavorText = "\"Best stem bolts in the quadrant. Self-sealing!\""
        imageUri = "https://cards.scryfall.io/normal/front/9/6/96104754-57e2-46e9-948d-628aab2aa0ad.jpg?1792508242"
    }
}
