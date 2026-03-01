package com.wingedsheep.mtg.sets.definitions.legions.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Clickslither
 * {1}{R}{R}{R}
 * Creature — Insect
 * 3/3
 * Haste
 * Sacrifice a Goblin: Clickslither gets +2/+2 and gains trample until end of turn.
 */
val Clickslither = card("Clickslither") {
    manaCost = "{1}{R}{R}{R}"
    typeLine = "Creature — Insect"
    power = 3
    toughness = 3
    oracleText = "Haste\nSacrifice a Goblin: This creature gets +2/+2 and gains trample until end of turn."

    keywords(Keyword.HASTE)

    activatedAbility {
        cost = AbilityCost.Sacrifice(GameObjectFilter.Creature.withSubtype("Goblin"))
        effect = Effects.ModifyStats(2, 2, EffectTarget.Self)
            .then(Effects.GrantKeyword(Keyword.TRAMPLE, EffectTarget.Self))
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "90"
        artist = "Kev Walker"
        flavorText = "The least popular goblins get the outer caves."
        imageUri = "https://cards.scryfall.io/normal/front/b/f/bf1c3f62-f275-46e1-8c26-c219683effb1.jpg?1562933424"
    }
}
