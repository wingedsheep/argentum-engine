package com.wingedsheep.mtg.sets.definitions.xln.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Ruin Raider
 * {2}{B}
 * Creature — Orc Pirate
 * 3/2
 * Raid — At the beginning of your end step, if you attacked this turn, reveal the top card of your
 * library and put that card into your hand. You lose life equal to the card's mana value.
 *
 * Raid is the intervening-if [Conditions.YouAttackedThisTurn] on the end-step trigger; the body is
 * Dark Confidant's reveal-to-hand pipeline.
 */
val RuinRaider = card("Ruin Raider") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Orc Pirate"
    power = 3
    toughness = 2
    oracleText = "Raid — At the beginning of your end step, if you attacked this turn, reveal the top card of your library and put that card into your hand. You lose life equal to the card's mana value."

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.END)
        interveningIf = Conditions.YouAttackedThisTurn
        effect = Effects.Pipeline {
            val revealed = gather(CardSource.TopOfLibrary(1, Player.You))
            toHand(revealed, revealed = true)
            run(Effects.LoseLife(
                DynamicAmounts.manaValueOf(revealed),
                EffectTarget.Controller
            ))
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "118"
        artist = "Kieran Yanner"
        imageUri = "https://cards.scryfall.io/normal/front/4/5/457ba695-3637-40de-a8a8-e3a43a71674f.jpg?1783935755"
    }
}
