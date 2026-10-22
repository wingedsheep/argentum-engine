package com.wingedsheep.mtg.sets.definitions.mid.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Flip the Switch
 * {2}{U}
 * Instant
 * Counter target spell unless its controller pays {4}. Create a 2/2 black Zombie creature token
 * with decayed.
 *
 * The token is created whether or not the tax is paid; it only fails to appear if the spell
 * fizzles because its target left the stack.
 */
val FlipTheSwitch = card("Flip the Switch") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Counter target spell unless its controller pays {4}. Create a 2/2 black Zombie creature " +
        "token with decayed. (It can't block. When it attacks, sacrifice it at end of combat.)"

    spell {
        target(TargetFilter.SpellOnStack)
        effect = Effects.CounterUnlessPays("{4}") then Effects.CreateToken(
            power = 2,
            toughness = 2,
            colors = setOf(Color.BLACK),
            creatureTypes = setOf("Zombie"),
            keywords = setOf(Keyword.DECAYED),
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "54"
        artist = "Campbell White"
        flavorText = "\"Works every time! Well, every time they don't explode, anyway.\"\n—Barton, stitcher"
        imageUri = "https://cards.scryfall.io/normal/front/5/c/5cdbe4e3-f030-46fa-ae84-edf261b61706.jpg?1783925640"

        ruling("2021-09-24", "You will create a Zombie creature token even if the spell's controller pays {4}.")
    }
}
