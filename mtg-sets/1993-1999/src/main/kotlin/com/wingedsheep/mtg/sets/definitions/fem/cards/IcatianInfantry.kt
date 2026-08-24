package com.wingedsheep.mtg.sets.definitions.fem.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Icatian Infantry
 * {W}
 * Creature — Human Soldier
 * 1/1
 * {1}: This creature gains first strike until end of turn.
 * {1}: This creature gains banding until end of turn.
 */
val IcatianInfantry = card("Icatian Infantry") {
    manaCost = "{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Soldier"
    oracleText = "{1}: This creature gains first strike until end of turn.\n" +
        "{1}: This creature gains banding until end of turn. (Any creatures with banding, and up " +
        "to one without, can attack in a band. Bands are blocked as a group. If any creatures with " +
        "banding you control are blocking or being blocked by a creature, you divide that " +
        "creature's combat damage, not its controller, among any of the creatures it's being " +
        "blocked by or is blocking.)"
    power = 1
    toughness = 1

    activatedAbility {
        cost = Costs.Mana("{1}")
        effect = Effects.GrantKeyword(Keyword.FIRST_STRIKE, EffectTarget.Self)
    }

    activatedAbility {
        cost = Costs.Mana("{1}")
        effect = Effects.GrantKeyword(Keyword.BANDING, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "7a"
        artist = "Edward P. Beard, Jr."
        flavorText = "The Icatian army easily repelled early surprise attacks by the Orcs on border towns like Montford."
        imageUri = "https://cards.scryfall.io/normal/front/f/9/f95d42d8-ba75-43bf-81b8-b02374f03e83.jpg?1783947919"
    }
}
