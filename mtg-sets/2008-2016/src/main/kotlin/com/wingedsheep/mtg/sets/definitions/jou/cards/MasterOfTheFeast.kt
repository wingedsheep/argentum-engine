package com.wingedsheep.mtg.sets.definitions.jou.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Master of the Feast
 * {1}{B}{B}
 * Enchantment Creature — Demon
 * 5/5
 * Flying
 * At the beginning of your upkeep, each opponent draws a card.
 */
val MasterOfTheFeast = card("Master of the Feast") {
    manaCost = "{1}{B}{B}"
    colorIdentity = "B"
    typeLine = "Enchantment Creature — Demon"
    power = 5
    toughness = 5
    oracleText = "Flying\nAt the beginning of your upkeep, each opponent draws a card."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.UPKEEP)
        effect = Effects.DrawCards(1, EffectTarget.PlayerRef(Player.EachOpponent))
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "75"
        artist = "Chris Rahn"
        flavorText = "Any pleasure, when taken to excess, becomes torment. It's a formula that demons have perfected."
        imageUri = "https://cards.scryfall.io/normal/front/d/5/d528f7a3-0a7b-42e3-8e4f-f1eeab02e23a.jpg?1783939433"
    }
}
