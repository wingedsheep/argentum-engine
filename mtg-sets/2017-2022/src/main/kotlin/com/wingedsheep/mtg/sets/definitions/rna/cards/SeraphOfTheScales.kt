package com.wingedsheep.mtg.sets.definitions.rna.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Seraph of the Scales — Ravnica Allegiance #205
 * {2}{W}{B} · Creature — Angel · 4/3
 *
 * Flying
 * {W}: This creature gains vigilance until end of turn.
 * {B}: This creature gains deathtouch until end of turn.
 * Afterlife 2 (When this creature dies, create two 1/1 white and black Spirit creature tokens with flying.)
 *
 * Afterlife is composed as its reminder text — a self-dies trigger creating the Spirit tokens — the
 * same shape as Knight of the Last Breath.
 */
val SeraphOfTheScales = card("Seraph of the Scales") {
    manaCost = "{2}{W}{B}"
    colorIdentity = "WB"
    typeLine = "Creature — Angel"
    power = 4
    toughness = 3
    oracleText = "Flying\n{W}: This creature gains vigilance until end of turn.\n{B}: This creature gains " +
        "deathtouch until end of turn.\nAfterlife 2 (When this creature dies, create two 1/1 white and black " +
        "Spirit creature tokens with flying.)"

    keywords(Keyword.FLYING)

    activatedAbility {
        cost = Costs.Mana("{W}")
        effect = Effects.GrantKeyword(Keyword.VIGILANCE, EffectTarget.Self)
        description = "{W}: This creature gains vigilance until end of turn."
    }

    activatedAbility {
        cost = Costs.Mana("{B}")
        effect = Effects.GrantKeyword(Keyword.DEATHTOUCH, EffectTarget.Self)
        description = "{B}: This creature gains deathtouch until end of turn."
    }

    triggeredAbility {
        trigger = Triggers.self.dies()
        effect = Effects.CreateToken(
            count = 2,
            power = 1,
            toughness = 1,
            colors = setOf(Color.WHITE, Color.BLACK),
            creatureTypes = setOf("Spirit"),
            keywords = setOf(Keyword.FLYING),
            imageUri = "https://cards.scryfall.io/normal/front/4/5/45b3bdd7-b093-4bfd-8a9a-965e72bfefb3.jpg?1783933602",
        )
        description = "Afterlife 2 (When this creature dies, create two 1/1 white and black Spirit creature " +
            "tokens with flying.)"
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "205"
        artist = "Magali Villeneuve"
        imageUri = "https://cards.scryfall.io/normal/front/d/6/d602e9e6-31ed-4d17-b39c-457b3b182943.jpg?1783933636"
    }
}
