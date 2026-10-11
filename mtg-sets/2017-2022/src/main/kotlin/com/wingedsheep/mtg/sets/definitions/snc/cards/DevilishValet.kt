package com.wingedsheep.mtg.sets.definitions.snc.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Devilish Valet — Streets of New Capenna #105 (canonical printing)
 * {2}{R} · Creature — Devil Warrior · 1/3
 *
 * Trample, haste
 * Alliance — Whenever another creature you control enters, double this creature's power until end of turn.
 *
 * "Double" is +X/+0 where X is its power as the ability resolves (the Reckless Amplimancer
 * shape), so each resolution reads the power left by the previous ones.
 */
val DevilishValet = card("Devilish Valet") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Devil Warrior"
    oracleText = "Trample, haste\nAlliance — Whenever another creature you control enters, double this creature's power until end of turn."
    power = 1
    toughness = 3

    keywords(Keyword.TRAMPLE, Keyword.HASTE)

    triggeredAbility {
        trigger = Triggers.another(GameObjectFilter.Creature.youControl()).enters()
        effect = Effects.ModifyStats(DynamicAmounts.sourcePower(), DynamicAmounts.fixed(0), EffectTarget.Self)
        description = "Alliance — Whenever another creature you control enters, double this creature's power until end of turn."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "105"
        artist = "Bud Cook"
        flavorText = "Nobody thought twice about checking their weapons at the door, until they were informed there was no weapon check at the door."
        imageUri = "https://cards.scryfall.io/normal/front/4/5/45d3fc69-6c5e-4ede-9f8d-c1cee096a78a.jpg?1783923120"
        ruling("2022-04-29", "To double a creature's power means that creature gets +X/+0, where X is its power as the ability resolves.")
    }
}
