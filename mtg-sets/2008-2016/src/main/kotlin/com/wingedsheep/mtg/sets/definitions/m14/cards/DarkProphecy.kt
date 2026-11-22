package com.wingedsheep.mtg.sets.definitions.m14.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Dark Prophecy
 * {B}{B}{B}
 * Enchantment
 * Whenever a creature you control dies, you draw a card and you lose 1 life.
 */
val DarkProphecy = card("Dark Prophecy") {
    manaCost = "{B}{B}{B}"
    colorIdentity = "B"
    typeLine = "Enchantment"
    oracleText = "Whenever a creature you control dies, you draw a card and you lose 1 life."

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Creature.youControl()).dies()
        effect = Effects.DrawCards(1) then Effects.LoseLife(1, EffectTarget.Controller)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "93"
        artist = "Scott Chou"
        flavorText = "When the bog ran short on small animals, Ekri turned to the surrounding farmlands."
        imageUri = "https://cards.scryfall.io/normal/front/e/c/ecf82c3b-7a35-43dd-8bf3-ebc68dc1b8fc.jpg?1783939925"
    }
}
