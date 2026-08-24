package com.wingedsheep.mtg.sets.definitions.fem.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Icatian Scout
 * {W}
 * Creature — Human Soldier Scout
 * 1/1
 * {1}, {T}: Target creature gains first strike until end of turn.
 */
val IcatianScout = card("Icatian Scout") {
    manaCost = "{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Soldier Scout"
    oracleText = "{1}, {T}: Target creature gains first strike until end of turn."
    power = 1
    toughness = 1

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}"), Costs.Tap)
        val t = target("target creature", TargetCreature(filter = TargetFilter.Creature))
        effect = Effects.GrantKeyword(Keyword.FIRST_STRIKE, t)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "13a"
        artist = "Richard Kane Ferguson"
        flavorText = "\"Because the Orc hordes attacked along the entire border, Scouts were essential to Icatia's defense.\"\n—*Sarpadian Empires, vol. VI*"
        imageUri = "https://cards.scryfall.io/normal/front/8/6/86bf4aaa-a9b1-4798-a96b-c3e35afb77f7.jpg?1783947917"
    }
}
