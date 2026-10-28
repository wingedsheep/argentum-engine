package com.wingedsheep.mtg.sets.definitions.znr.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Expedition Skulker
 * {1}{B}
 * Creature — Vampire Rogue
 * 2/2
 *
 * This creature has deathtouch as long as you control another Rogue.
 *
 * "Another Rogue" is any other permanent with the Rogue subtype (excludeSelf) — same shape as
 * Expedition Healer's "another Cleric".
 */
val ExpeditionSkulker = card("Expedition Skulker") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Vampire Rogue"
    power = 2
    toughness = 2
    oracleText = "This creature has deathtouch as long as you control another Rogue. " +
        "(Any amount of damage it deals to a creature is enough to destroy it.)"

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = GrantKeyword(Keyword.DEATHTOUCH, GroupFilter.source()),
            condition = Conditions.YouControl(
                GameObjectFilter.Permanent.withSubtype(Subtype.ROGUE),
                excludeSelf = true
            )
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "101"
        artist = "Mathias Kollros"
        flavorText = "Rogues bring an array of useful skills to the work of an adventuring party: stealth, " +
            "agility, the ability to locate and disarm traps, and a subtle, deadly fighting style."
        imageUri = "https://cards.scryfall.io/normal/front/2/9/2966b1f0-3d30-4d92-87e5-f39ceacefbd2.jpg?1783929378"
    }
}
