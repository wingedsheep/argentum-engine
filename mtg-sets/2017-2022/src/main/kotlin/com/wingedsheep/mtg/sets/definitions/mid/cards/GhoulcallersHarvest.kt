package com.wingedsheep.mtg.sets.definitions.mid.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.divRoundedUp
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility

/**
 * Ghoulcaller's Harvest
 * {B}{G}
 * Sorcery
 * Create X 2/2 black Zombie creature tokens with decayed, where X is half the number of creature
 * cards in your graveyard, rounded up.
 * Flashback {3}{B}{G}
 *
 * X is counted on resolution. Cast with flashback, the card itself is on the stack, so it never
 * counts toward its own X (it isn't a creature card anyway).
 */
val GhoulcallersHarvest = card("Ghoulcaller's Harvest") {
    manaCost = "{B}{G}"
    colorIdentity = "BG"
    typeLine = "Sorcery"
    oracleText = "Create X 2/2 black Zombie creature tokens with decayed, where X is half the number of " +
        "creature cards in your graveyard, rounded up. (A creature with decayed can't block. When it " +
        "attacks, sacrifice it at end of combat.)\n" +
        "Flashback {3}{B}{G} (You may cast this card from your graveyard for its flashback cost. Then exile it.)"

    spell {
        effect = Effects.CreateToken(
            count = DynamicAmounts.creatureCardsInYourGraveyard() divRoundedUp 2,
            power = 2,
            toughness = 2,
            colors = setOf(Color.BLACK),
            creatureTypes = setOf("Zombie"),
            keywords = setOf(Keyword.DECAYED),
            imageUri = "https://cards.scryfall.io/normal/front/6/a/6adb8607-1066-451d-a719-74ad32358278.jpg?1783925226",
        )
    }

    keywordAbility(KeywordAbility.flashback("{3}{B}{G}"))

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "225"
        artist = "Anna Steinbauer"
        imageUri = "https://cards.scryfall.io/normal/front/6/a/6a507ea6-d818-4443-b468-cf65c3e7031c.jpg?1783925560"
    }
}
