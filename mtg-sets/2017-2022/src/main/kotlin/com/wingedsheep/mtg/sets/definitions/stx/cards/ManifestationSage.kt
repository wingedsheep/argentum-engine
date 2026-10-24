package com.wingedsheep.mtg.sets.definitions.stx.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CREATED_TOKENS
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Manifestation Sage — Strixhaven: School of Mages #205 (canonical printing)
 * {G/U}{G/U}{G/U}{G/U} · Creature — Human Wizard · 2/2
 *
 * When this creature enters, create a 0/0 green and blue Fractal creature token. Put X +1/+1
 * counters on it, where X is the number of cards in your hand.
 *
 * The Fractal Summoning shape on an enters trigger: [Effects.CreateToken] then
 * [Effects.AddDynamicCounters] on the created token, counting [DynamicAmounts.cardsInYourHand]
 * as the trigger resolves (ruling). With an empty hand the 0/0 dies to state-based actions.
 */
val ManifestationSage = card("Manifestation Sage") {
    manaCost = "{G/U}{G/U}{G/U}{G/U}"
    colorIdentity = "GU"
    typeLine = "Creature — Human Wizard"
    oracleText = "When this creature enters, create a 0/0 green and blue Fractal creature token. " +
        "Put X +1/+1 counters on it, where X is the number of cards in your hand."
    power = 2
    toughness = 2

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.CreateToken(
            power = 0,
            toughness = 0,
            colors = setOf(Color.GREEN, Color.BLUE),
            creatureTypes = setOf(Subtype.FRACTAL.value)
        ) then
            Effects.AddDynamicCounters(
                CounterType.PLUS_ONE_PLUS_ONE,
                DynamicAmounts.cardsInYourHand(),
                EffectTarget.PipelineTarget(CREATED_TOKENS, 0)
            )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "205"
        artist = "Darren Tan"
        flavorText = "The first thing a Quandrix mage learns is the laws of metaphysics. The second thing is how to break them."
        imageUri = "https://cards.scryfall.io/normal/front/7/6/76fc5cd2-fbb0-4d13-9089-0292b356de48.jpg?1783927305"
        ruling("2021-04-16", "X is number of cards in your hand as Manifestation Sage's triggered ability resolves.")
    }
}
