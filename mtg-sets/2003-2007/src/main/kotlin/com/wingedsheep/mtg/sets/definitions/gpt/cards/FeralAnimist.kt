package com.wingedsheep.mtg.sets.definitions.gpt.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Feral Animist — Guildpact #112
 * {1}{R}{G} · Creature — Goblin Shaman · 2/1
 *
 * {3}: This creature gets +X/+0 until end of turn, where X is its power.
 *
 * X is read once, as the ability resolves, and a negative power counts as 0 — hence
 * [DynamicAmounts.nonNegative] over the source's projected power (same shape as
 * Electrostatic Pummeler).
 */
val FeralAnimist = card("Feral Animist") {
    manaCost = "{1}{R}{G}"
    colorIdentity = "RG"
    typeLine = "Creature — Goblin Shaman"
    power = 2
    toughness = 1
    oracleText = "{3}: This creature gets +X/+0 until end of turn, where X is its power."

    activatedAbility {
        cost = Costs.Mana("{3}")
        effect = Effects.ModifyStats(
            DynamicAmounts.nonNegative(DynamicAmounts.sourcePower()),
            DynamicAmounts.fixed(0),
            EffectTarget.Self
        )
        description = "{3}: This creature gets +X/+0 until end of turn, where X is its power."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "112"
        artist = "Ron Spears"
        flavorText = "He chanted over a pot of boiling blood and honey. His brain buzzed like a nest of hornets, and his muscles rippled with the might of the bear."
        imageUri = "https://cards.scryfall.io/normal/front/a/d/ad65c721-b601-46f9-ba0b-ac4d96567cee.jpg?1783943479"
        ruling("2013-04-15", "You don't choose the value of X. The bonus Feral Animist gets is based on its power when the ability resolves. For example, if you activate the ability twice, the first ability will give it +2/+0 and the second one will give it +4/+0.")
        ruling("2017-07-14", "If this creature's power is negative as its ability resolves, X is considered to be 0.")
    }
}
