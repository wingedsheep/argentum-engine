package com.wingedsheep.mtg.sets.definitions.chk.cards

import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Part the Veil
 * {3}{U}
 * Instant — Arcane
 * Return all creatures you control to their owner's hand.
 *
 * "Their owner's hand": a creature you've stolen goes back to its owner, not to you —
 * `returnAllToHand` moves each card to its owner's hand.
 */
val PartTheVeil = card("Part the Veil") {
    manaCost = "{3}{U}"
    colorIdentity = "U"
    typeLine = "Instant — Arcane"
    oracleText = "Return all creatures you control to their owner's hand."

    spell {
        effect = Patterns.Group.returnAllToHand(GroupFilter.AllCreaturesYouControl)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "77"
        artist = "Arnie Swekel"
        flavorText = "At the waterfall, the border between the humanoid and the spirit worlds was weakest. The kami moved across it with ease and at will."
        imageUri = "https://cards.scryfall.io/normal/front/d/8/d870e607-1607-46f3-bc9f-925d0164bcf9.jpg?1783944324"
    }
}
