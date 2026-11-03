package com.wingedsheep.mtg.sets.definitions.m11.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Gargoyle Sentinel — Magic 2011 #207
 * {3} · Artifact Creature — Gargoyle · 3/3
 *
 * Defender (This creature can't attack.)
 * {3}: Until end of turn, this creature loses defender and gains flying.
 *
 * Both halves are separate Layer 6 keyword modifications lasting [Duration.EndOfTurn]: losing
 * defender is [Effects.RemoveKeyword], flying is [Effects.GrantKeyword].
 */
val GargoyleSentinel = card("Gargoyle Sentinel") {
    manaCost = "{3}"
    colorIdentity = ""
    typeLine = "Artifact Creature — Gargoyle"
    power = 3
    toughness = 3
    oracleText = "Defender (This creature can't attack.)\n" +
        "{3}: Until end of turn, this creature loses defender and gains flying."

    keywords(Keyword.DEFENDER)

    activatedAbility {
        cost = Costs.Mana("{3}")
        effect = Effects.RemoveKeyword(Keyword.DEFENDER, EffectTarget.Self, Duration.EndOfTurn) then
            Effects.GrantKeyword(Keyword.FLYING, EffectTarget.Self, Duration.EndOfTurn)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "207"
        artist = "Drew Baker"
        flavorText = "\"Do not mistake a quiet rampart for one that is unguarded. Some things stir only when they need to.\"\n—Bonovar, siege commander"
        imageUri = "https://cards.scryfall.io/normal/front/6/a/6af328bc-bce5-4024-9da5-853ee16eaa8a.jpg?1783941791"
    }
}
