package com.wingedsheep.mtg.sets.definitions.chk.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.NoMaximumHandSize

/**
 * Graceful Adept
 * {2}{U}
 * Creature — Human Wizard
 * 1/3
 * You have no maximum hand size.
 */
val GracefulAdept = card("Graceful Adept") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Human Wizard"
    oracleText = "You have no maximum hand size."
    power = 1
    toughness = 3

    staticAbility {
        ability = NoMaximumHandSize
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "63"
        artist = "Scott M. Fischer"
        flavorText = "\"When you have mastered my lessons, it will seem as though the whole of the world has opened up to your mind and nothing is beyond your grasp.\"\n—Lady Azami"
        imageUri = "https://cards.scryfall.io/normal/front/6/4/648430cc-80d1-479f-ae31-76687d2eb57c.jpg?1783944327"
        ruling("2009-10-01", "If multiple effects modify your hand size, apply them in timestamp order. For example, if you put Null Profusion (an enchantment that says your maximum hand size is two) onto the battlefield and then put Graceful Adept onto the battlefield, you'll have no maximum hand size. However, if those permanents entered in the opposite order, your maximum hand size would be two.")
    }
}
