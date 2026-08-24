package com.wingedsheep.mtg.sets.definitions.mmq.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.ModifyStats

/**
 * Cave Sense
 * {1}{R}
 * Enchantment — Aura
 *
 * Enchant creature
 * Enchanted creature gets +1/+1 and has mountainwalk.
 *
 * Note there is **no** flash here — Cave Sense sits outside Mercadian Masques' flash-Aura cycle
 * despite sharing the colour and the cost with [FlamingSword].
 *
 * Granted landwalk is engine-live: `BlockEvasionRules.LandwalkRule` reads the keyword out of
 * projected state and maps `MOUNTAINWALK` to the Mountain subtype, so the grant works exactly as
 * a printed one would. Same two-static shape as Cursed Flesh (`-1/-1` + fear).
 */
val CaveSense = card("Cave Sense") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\n" +
        "Enchanted creature gets +1/+1 and has mountainwalk. (It can't be blocked as long as defending player controls a Mountain.)"

    auraTarget = Targets.Creature

    staticAbility {
        ability = ModifyStats(1, 1)
    }

    staticAbility {
        ability = GrantKeyword(Keyword.MOUNTAINWALK)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "179"
        artist = "Mark Romanoski"
        imageUri = "https://cards.scryfall.io/normal/front/2/d/2d718421-c742-489c-a243-3adb19f6716a.jpg"
    }
}
