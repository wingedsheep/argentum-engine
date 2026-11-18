package com.wingedsheep.mtg.sets.definitions.jou.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Riptide Chimera
 * {2}{U}
 * Enchantment Creature — Chimera
 * 3/4
 *
 * Flying
 * At the beginning of your upkeep, return an enchantment you control to its owner's hand.
 *
 * The return doesn't target and isn't optional: the controller chooses one enchantment they control
 * as the trigger resolves (the Chimera itself is an enchantment, so it is always a legal choice).
 * Same pipeline as Roaring Primadox — gather, choose exactly one, move to hand.
 */
val RiptideChimera = card("Riptide Chimera") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Enchantment Creature — Chimera"
    power = 3
    toughness = 4
    oracleText = "Flying\nAt the beginning of your upkeep, return an enchantment you control to its owner's hand."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.UPKEEP)
        effect = Effects.Pipeline {
            val enchantments = gather(
                CardSource.BattlefieldMatching(
                    filter = GameObjectFilter.Enchantment,
                    player = Player.You,
                )
            )
            val returned = chooseExactly(
                1,
                from = enchantments,
                prompt = "Return an enchantment you control to its owner's hand",
                useTargetingUI = true
            )
            toHand(returned)
        }
        description = "At the beginning of your upkeep, return an enchantment you control to its owner's hand."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "48"
        artist = "Ryan Barger"
        flavorText = "\"I want one.\"\n—Kiora"
        imageUri = "https://cards.scryfall.io/normal/front/e/9/e9f88da5-b013-478a-8a0b-da9d1e8eb2c9.jpg?1783939443"
    }
}
