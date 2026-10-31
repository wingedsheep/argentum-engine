package com.wingedsheep.mtg.sets.definitions.m20.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Scampering Scorcher (M20 #158)
 * {3}{R}  Creature — Elemental  1/1
 *
 * When this creature enters, create two 1/1 red Elemental creature tokens. Elementals you control
 * gain haste until end of turn. (They can attack and {T} this turn.)
 *
 * "Elementals you control" is read on resolution, after the tokens exist, so the two new tokens,
 * the Scorcher itself and any other Elemental you control all gain haste.
 */
val ScamperingScorcher = card("Scampering Scorcher") {
    manaCost = "{3}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Elemental"
    oracleText = "When this creature enters, create two 1/1 red Elemental creature tokens. " +
        "Elementals you control gain haste until end of turn. (They can attack and {T} this turn.)"
    power = 1
    toughness = 1

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = setOf(Color.RED),
            creatureTypes = setOf("Elemental"),
            count = 2,
        ) then Effects.ForEachInGroup(
            GroupFilter(GameObjectFilter.Permanent.withSubtype(Subtype.ELEMENTAL).youControl()),
            Effects.GrantKeyword(Keyword.HASTE, EffectTarget.IterationEntity),
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "158"
        artist = "Zoltan Boros"
        flavorText = "\"Keep your flammables out of reach.\"\n—Kerin, watch sergeant"
        imageUri = "https://cards.scryfall.io/normal/front/d/c/dccbe031-1547-4fa5-ba9a-caa1b1f5eb5d.jpg"
    }
}
