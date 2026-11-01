package com.wingedsheep.mtg.sets.definitions.rna.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

private const val SPIRIT_TOKEN_IMAGE =
    "https://cards.scryfall.io/normal/front/4/5/45b3bdd7-b093-4bfd-8a9a-965e72bfefb3.jpg?1783933602"

/**
 * Knight of the Last Breath — Ravnica Allegiance #188
 * {5}{W}{B} · Creature — Giant Knight · 4/4
 *
 * {3}, Sacrifice another nontoken creature: Create a 1/1 white and black Spirit creature token with flying.
 * Afterlife 3 (When this creature dies, create three 1/1 white and black Spirit creature tokens with flying.)
 *
 * Afterlife is composed as its reminder text — a self-dies trigger creating the Spirit tokens — the
 * same shape as Ministrant of Obligation.
 */
val KnightOfTheLastBreath = card("Knight of the Last Breath") {
    manaCost = "{5}{W}{B}"
    colorIdentity = "WB"
    typeLine = "Creature — Giant Knight"
    power = 4
    toughness = 4
    oracleText = "{3}, Sacrifice another nontoken creature: Create a 1/1 white and black Spirit creature " +
        "token with flying.\nAfterlife 3 (When this creature dies, create three 1/1 white and black Spirit " +
        "creature tokens with flying.)"

    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{3}"),
            Costs.SacrificeAnother(GameObjectFilter.Creature.nontoken()),
        )
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = setOf(Color.WHITE, Color.BLACK),
            creatureTypes = setOf("Spirit"),
            keywords = setOf(Keyword.FLYING),
            imageUri = SPIRIT_TOKEN_IMAGE,
        )
        description = "{3}, Sacrifice another nontoken creature: Create a 1/1 white and black Spirit " +
            "creature token with flying."
    }

    triggeredAbility {
        trigger = Triggers.self.dies()
        effect = Effects.CreateToken(
            count = 3,
            power = 1,
            toughness = 1,
            colors = setOf(Color.WHITE, Color.BLACK),
            creatureTypes = setOf("Spirit"),
            keywords = setOf(Keyword.FLYING),
            imageUri = SPIRIT_TOKEN_IMAGE,
        )
        description = "Afterlife 3 (When this creature dies, create three 1/1 white and black Spirit " +
            "creature tokens with flying.)"
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "188"
        artist = "Milivoj Ćeran"
        imageUri = "https://cards.scryfall.io/normal/front/7/6/76fccf60-2df5-4c14-893f-41d3f86c545f.jpg?1783933644"
    }
}
