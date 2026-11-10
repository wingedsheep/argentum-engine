package com.wingedsheep.mtg.sets.definitions.ogw.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Zada's Commando
 * {1}{R}
 * Creature — Goblin Archer Ally
 * 2/1
 * First strike
 * Cohort — {T}, Tap an untapped Ally you control: This creature deals 1 damage to target opponent
 * or planeswalker.
 */
val ZadasCommando = card("Zada's Commando") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Goblin Archer Ally"
    power = 2
    toughness = 1
    oracleText = "First strike\n" +
        "Cohort — {T}, Tap an untapped Ally you control: This creature deals 1 damage to target opponent or planeswalker."

    keywords(Keyword.FIRST_STRIKE)

    activatedAbility {
        cost = Costs.Composite(
            Costs.Tap,
            Costs.TapPermanents(1, GameObjectFilter.Permanent.withSubtype(Subtype.ALLY)),
        )
        val victim = target(Targets.OpponentOrPlaneswalker)
        effect = Effects.DealDamage(1, victim)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "120"
        artist = "Zack Stella"
        flavorText = "\"There's a word for someone who brings too much gear to battle: 'survivor.'\"\n—Zada of Slab Haven"
        imageUri = "https://cards.scryfall.io/normal/front/f/4/f4ad826d-c7e6-4dfd-991f-15d75b87d504.jpg"
    }
}
