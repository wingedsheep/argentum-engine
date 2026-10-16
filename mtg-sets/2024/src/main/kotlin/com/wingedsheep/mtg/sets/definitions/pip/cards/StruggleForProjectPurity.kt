package com.wingedsheep.mtg.sets.definitions.pip.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.CollectionSlot
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.minus
import com.wingedsheep.sdk.dsl.times
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ChoiceType
import com.wingedsheep.sdk.scripting.EntersWithChoice
import com.wingedsheep.sdk.scripting.ModeOption
import com.wingedsheep.sdk.scripting.conditions.SourceChosenModeIs
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Struggle for Project Purity {3}{U}
 * Enchantment
 *
 * Brotherhood: "cards drawn this way" is the growth of the opponents' cards-drawn-this-turn tally
 * across their draws, so an opponent with an empty library adds nothing. Enclave: "that many" is
 * the attackers that player declared at you, captured when the trigger fires (the card's ruling) —
 * removing one from combat doesn't lower it, and a creature put onto the battlefield attacking
 * doesn't raise it.
 */
val StruggleForProjectPurity = card("Struggle for Project Purity") {
    manaCost = "{3}{U}"
    colorIdentity = "U"
    typeLine = "Enchantment"
    oracleText = "As this enchantment enters, choose Brotherhood or Enclave.\n" +
        "• Brotherhood — At the beginning of your upkeep, each opponent draws a card. You draw a card for each card drawn this way.\n" +
        "• Enclave — Whenever a player attacks you with one or more creatures, that player gets twice that many rad counters."

    replacementEffect(EntersWithChoice(
        choiceType = ChoiceType.MODE,
        modeOptions = listOf(
            ModeOption(id = "brotherhood", label = "Brotherhood", description = "At your upkeep, each opponent draws a card; you draw that many."),
            ModeOption(id = "enclave", label = "Enclave", description = "A player attacking you gets two rad counters per attacker.")
        )
    ))

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.UPKEEP)
        triggerRestriction = SourceChosenModeIs("brotherhood")
        effect = Effects.Pipeline {
            val opponentDraws = DynamicAmounts.cardsDrawnThisTurn(Player.EachOpponent)
            val drawnBefore = storeNumber(opponentDraws)
            run(Effects.ForEachPlayer(Player.EachOpponent, Effects.DrawCards(1, EffectTarget.Controller)))
            run(Effects.DrawCards(opponentDraws - drawnBefore.amount))
        }
    }

    triggeredAbility {
        trigger = Triggers.you.isAttacked()
        triggerRestriction = SourceChosenModeIs("enclave")
        effect = Effects.AddDynamicCounters(
            CounterType.RAD,
            DynamicAmounts.distinctEntitiesIn(CollectionSlot.TriggerCaptured) * 2,
            EffectTarget.PlayerRef(Player.TriggeringPlayer)
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "39"
        artist = "Billy Christian"
        imageUri = "https://cards.scryfall.io/normal/front/0/4/04c60155-9337-4ea2-b9a2-5b72d96743ff.jpg?1783912410"
        ruling("2024-03-08", "The number of rad counters the attacking player gets when the “Enclave” triggered ability resolves is based on the number of creatures they declared as attackers. Removing creatures from combat won’t reduce the number of rad counters they get. Similarly, creatures that enter the battlefield attacking but weren’t declared as attackers won’t increase the number of rad counters they get.")
        ruling("2024-03-08", "If you somehow control Struggle for Project Purity and no choice was made for it (perhaps because another permanent on the battlefield became a copy of it), it has neither of the two triggered abilities.")
    }
}
