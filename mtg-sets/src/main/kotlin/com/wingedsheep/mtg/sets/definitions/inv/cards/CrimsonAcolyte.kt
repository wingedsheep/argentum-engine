package com.wingedsheep.mtg.sets.definitions.inv.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.ProtectionScope

/**
 * Crimson Acolyte
 * {1}{W}
 * Creature — Human Cleric
 * 1/1
 * Protection from red
 * {W}: Target creature gains protection from red until end of turn.
 */
val CrimsonAcolyte = card("Crimson Acolyte") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Cleric"
    power = 1
    toughness = 1
    oracleText = "Protection from red\n{W}: Target creature gains protection from red until end of turn."

    keywordAbility(KeywordAbility.Protection(ProtectionScope.Color(Color.RED)))

    activatedAbility {
        cost = Costs.Mana("{W}")
        val t = target("target", Targets.Creature)
        effect = Effects.GrantProtectionFromColor(Color.RED, t)
        description = "{W}: Target creature gains protection from red until end of turn."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "11"
        artist = "Dany Orizio"
        imageUri = "https://cards.scryfall.io/normal/front/c/1/c1718028-3009-4bdd-9f6f-59c17edd1344.jpg?1562933869"
    }
}
