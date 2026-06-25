package com.wingedsheep.mtg.sets.definitions.tmt.cards

import com.wingedsheep.sdk.core.CardType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Don & Raph, Hard Science
 * {1}{U/R}{U/R}
 * Legendary Creature — Mutant Ninja Turtle
 * 2/4
 *
 * Menace
 * Whenever Don & Raph attack, the next noncreature spell you cast this turn has
 * affinity for artifacts. (It costs {1} less to cast for each artifact you control.)
 */
val DonAndRaphHardScience = card("Don & Raph, Hard Science") {
    manaCost = "{1}{U/R}{U/R}"
    colorIdentity = "UR"
    typeLine = "Legendary Creature — Mutant Ninja Turtle"
    oracleText = "Menace\nWhenever Don & Raph attack, the next noncreature spell you cast this turn has affinity for artifacts. (It costs {1} less to cast for each artifact you control.)"
    power = 2
    toughness = 4

    keywords(Keyword.MENACE)

    triggeredAbility {
        trigger = Triggers.Attacks
        effect = Effects.GrantNextSpellAffinity(GameObjectFilter.Noncreature, CardType.ARTIFACT)
        description = "Whenever Don & Raph attack, the next noncreature spell you cast this turn has affinity for artifacts."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "144"
        artist = "Anthony Devine"
        imageUri = "https://cards.scryfall.io/normal/front/4/f/4f38126c-26a9-4447-801f-4f19e84c4aa5.jpg?1769006276"
    }
}
