package com.wingedsheep.mtg.sets.definitions.dka.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Bone to Ash
 * {2}{U}{U}
 * Instant
 *
 * Counter target creature spell.
 * Draw a card.
 */
val BoneToAsh = card("Bone to Ash") {
    manaCost = "{2}{U}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Counter target creature spell.\nDraw a card."

    spell {
        target(TargetFilter.CreatureSpellOnStack)
        effect = Effects.CounterSpell() then Effects.DrawCards(1)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "29"
        artist = "Clint Cearley"
        flavorText = "\"I can think of worse ways to go. On second thought, maybe not.\"\n—Ludevic, necro-alchemist"
        imageUri = "https://cards.scryfall.io/normal/front/c/4/c4a75cef-9551-45e2-b1ff-80662c76ec20.jpg?1783940846"
    }
}
