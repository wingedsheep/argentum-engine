package com.wingedsheep.mtg.sets.definitions.grn.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Chooser

/**
 * Pilfering Imp
 * {B}
 * Creature — Imp
 * 1/1
 * Flying
 * {1}{B}, {T}, Sacrifice this creature: Target opponent reveals their hand. You choose a nonland
 * card from it. That player discards that card. Activate only as a sorcery.
 *
 * The sacrifice ability is the Duress/Distress pipeline (`chk/cards/Distress.kt`) aimed at an opponent.
 */
val PilferingImp = card("Pilfering Imp") {
    manaCost = "{B}"
    colorIdentity = "B"
    typeLine = "Creature — Imp"
    power = 1
    toughness = 1
    oracleText = "Flying\n" +
        "{1}{B}, {T}, Sacrifice this creature: Target opponent reveals their hand. You choose a nonland card " +
        "from it. That player discards that card. Activate only as a sorcery."

    keywords(Keyword.FLYING)

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}{B}"), Costs.Tap, Costs.SacrificeSelf)
        timing = TimingRule.SorcerySpeed
        val opponent = target(Targets.Opponent)
        effect = Effects.Pipeline {
            run(Effects.RevealHand(opponent))
            val hand = gather(CardSource.FromZone(Zone.HAND, opponent.asPlayer))
            val toDiscard = chooseExactly(
                1,
                from = hand,
                chooser = Chooser.Controller,
                filter = GameObjectFilter.Nonland,
                prompt = "Choose a nonland card to discard",
                alwaysPrompt = true,
                showAllCards = true
            )
            discard(toDiscard, opponent.asPlayer)
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "81"
        artist = "Igor Kieryluk"
        imageUri = "https://cards.scryfall.io/normal/front/6/4/646dedff-150c-4ed2-8f2e-3b2e6ba5521a.jpg?1783934171"
    }
}
