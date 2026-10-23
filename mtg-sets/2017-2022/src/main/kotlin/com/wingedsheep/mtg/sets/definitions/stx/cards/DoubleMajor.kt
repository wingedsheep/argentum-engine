package com.wingedsheep.mtg.sets.definitions.stx.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Double Major
 * {G}{U}
 * Instant
 * Copy target creature spell you control, except it isn't legendary if the spell is
 * legendary. (A copy of a creature spell becomes a token.)
 *
 * `CopyTargetSpell` on a permanent spell makes a copy that becomes a token as it resolves
 * (CR 707.10f); `removeLegendary` strips the legendary supertype from the copy (a no-op on a
 * nonlegendary spell), the same shape as Jackal, Genius Geneticist.
 */
val DoubleMajor = card("Double Major") {
    manaCost = "{G}{U}"
    colorIdentity = "GU"
    typeLine = "Instant"
    oracleText = "Copy target creature spell you control, except it isn't legendary if the spell is legendary. " +
        "(A copy of a creature spell becomes a token.)"

    spell {
        val creatureSpell = target(TargetFilter.CreatureSpellOnStack.youControl())
        effect = Effects.CopyTargetSpell(target = creatureSpell, removeLegendary = true)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "179"
        artist = "Suzanne Helmigh"
        flavorText = "\"Now I can dedicate myself fully to zoetimancy and void theory!\""
        imageUri = "https://cards.scryfall.io/normal/front/c/3/c3d35413-8742-4443-8859-93c91112978d.jpg?1783927318"
        ruling("2021-04-16", "If a creature spell is copied, it's put onto the battlefield as a token as the spell resolves rather than putting the copy of the spell onto the battlefield. The rules that apply to a permanent spell becoming a permanent apply to a copy of a spell becoming a token.")
        ruling("2021-04-16", "The token that a resolving copy of a spell becomes isn't said to have been \"created.\"")
    }
}
