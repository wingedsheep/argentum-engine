package com.wingedsheep.mtg.sets.definitions.m19.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Sai, Master Thopterist — Core Set 2019 #69
 * {2}{U} · Legendary Creature — Human Artificer 1/4
 *
 * Whenever you cast an artifact spell, create a 1/1 colorless Thopter artifact creature token
 * with flying.
 * {1}{U}, Sacrifice two artifacts: Draw a card.
 */
val SaiMasterThopterist = card("Sai, Master Thopterist") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Legendary Creature — Human Artificer"
    power = 1
    toughness = 4
    oracleText = "Whenever you cast an artifact spell, create a 1/1 colorless Thopter artifact creature " +
        "token with flying.\n" +
        "{1}{U}, Sacrifice two artifacts: Draw a card."

    triggeredAbility {
        trigger = Triggers.you.casts(GameObjectFilter.Artifact)
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = emptySet(),
            creatureTypes = setOf("Thopter"),
            keywords = setOf(Keyword.FLYING),
            artifactToken = true,
            imageUri = "https://cards.scryfall.io/normal/front/d/0/d0044cd9-2136-423e-aa2a-829e8f007535.jpg?1783934464",
        )
        description = "Whenever you cast an artifact spell, create a 1/1 colorless Thopter artifact " +
            "creature token with flying."
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}{U}"), Costs.SacrificeMultiple(2, GameObjectFilter.Artifact))
        effect = Effects.DrawCards(1)
        description = "Draw a card."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "69"
        artist = "Adam Paquette"
        flavorText = "\"Industry without artistry is just a fancy way to fill the junkyard.\""
        imageUri = "https://cards.scryfall.io/normal/front/1/9/19316cbb-d1af-4ab7-b588-78637503e986.jpg?1783934583"
        ruling("2018-07-13", "Sai's triggered ability resolves before the spell that caused it to trigger. It resolves even if that spell is countered.")
    }
}
