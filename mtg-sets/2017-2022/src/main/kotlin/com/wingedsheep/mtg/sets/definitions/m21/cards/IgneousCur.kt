package com.wingedsheep.mtg.sets.definitions.m21.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Igneous Cur — Core Set 2021 #153 (canonical printing)
 * {1}{R} · Creature — Elemental Dog · 1/2
 *
 * {1}{R}: This creature gets +2/+0 until end of turn.
 */
val IgneousCur = card("Igneous Cur") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Elemental Dog"
    power = 1
    toughness = 2
    oracleText = "{1}{R}: This creature gets +2/+0 until end of turn."

    activatedAbility {
        cost = Costs.Mana("{1}{R}")
        effect = Effects.ModifyStats(2, 0, EffectTarget.Self)
        description = "{1}{R}: This creature gets +2/+0 until end of turn."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "153"
        artist = "Igor Kieryluk"
        flavorText = "Some dogs just aren't meant to be indoor pets."
        imageUri = "https://cards.scryfall.io/normal/front/d/2/d2bfec5d-3182-415a-afe8-0b5511cfd656.jpg?1783930688"
    }
}
