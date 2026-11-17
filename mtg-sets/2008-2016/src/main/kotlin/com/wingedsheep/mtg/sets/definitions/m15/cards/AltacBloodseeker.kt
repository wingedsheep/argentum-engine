package com.wingedsheep.mtg.sets.definitions.m15.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Altac Bloodseeker
 * {1}{R}
 * Creature — Human Berserker
 * 2/1
 * Whenever a creature an opponent controls dies, this creature gets +2/+0 and gains first strike and haste until end of turn. (It deals combat damage before creatures without first strike, and it can attack and {T} as soon as it comes under your control.)
 */
val AltacBloodseeker = card("Altac Bloodseeker") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Human Berserker"
    power = 2
    toughness = 1
    oracleText = "Whenever a creature an opponent controls dies, this creature gets +2/+0 and gains first strike and haste until end of turn. (It deals combat damage before creatures without first strike, and it can attack and {T} as soon as it comes under your control.)"

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Creature.opponentControls()).dies()
        effect = Effects.ModifyStats(2, 0, EffectTarget.Self) then
            Effects.GrantKeyword(Keyword.FIRST_STRIKE, EffectTarget.Self) then
            Effects.GrantKeyword(Keyword.HASTE, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "128"
        artist = "Cynthia Sheppard"
        imageUri = "https://cards.scryfall.io/normal/front/f/5/f5dacedc-f532-43b3-a783-e1dc7ccea53b.jpg"
    }
}
