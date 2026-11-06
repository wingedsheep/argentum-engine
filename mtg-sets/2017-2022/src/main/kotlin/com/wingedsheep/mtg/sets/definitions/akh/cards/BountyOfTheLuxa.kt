package com.wingedsheep.mtg.sets.definitions.akh.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.SuccessCriterion
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Bounty of the Luxa — Amonkhet #196 (canonical and earliest real-expansion printing)
 * {2}{G}{U} · Enchantment
 *
 * At the beginning of your first main phase, remove all flood counters from this enchantment. If
 * no counters were removed this way, put a flood counter on this enchantment and draw a card.
 * Otherwise, add {C}{G}{U}.
 *
 * The removal is the gated action of an [Effects.IfYouDo] judged by
 * [SuccessCriterion.CountersRemoved]: any number of flood counters coming off yields exactly
 * {C}{G}{U} (ruling 2017-04-18), and none coming off — including because the enchantment left the
 * battlefield in response, where the counter placement then finds nothing — draws a card. The
 * first-main trigger is the Black Market shape, `beginningOf(Step.PRECOMBAT_MAIN)`, so the mana
 * lands in the pool at the start of the main phase it can be spent in.
 */
val BountyOfTheLuxa = card("Bounty of the Luxa") {
    manaCost = "{2}{G}{U}"
    colorIdentity = "GU"
    typeLine = "Enchantment"
    oracleText = "At the beginning of your first main phase, remove all flood counters from this enchantment. " +
        "If no counters were removed this way, put a flood counter on this enchantment and draw a card. " +
        "Otherwise, add {C}{G}{U}."

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.PRECOMBAT_MAIN)
        effect = Effects.IfYouDo(
            action = Effects.RemoveAllCountersOfType(CounterType.FLOOD, EffectTarget.Self),
            then = Effects.AddColorlessMana(1) then
                Effects.AddMana(Color.GREEN) then
                Effects.AddMana(Color.BLUE),
            otherwise = Effects.AddCounters(CounterType.FLOOD, 1, EffectTarget.Self) then
                Effects.DrawCards(1),
            successCriterion = SuccessCriterion.CountersRemoved,
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "196"
        artist = "Jonas De Ro"
        imageUri = "https://cards.scryfall.io/normal/front/5/7/574dab03-b8e6-4051-9dd5-31439c817223.jpg?1783936465"
        ruling("2017-04-18", "If Bounty of the Luxa somehow has multiple flood counters on it, you'll still get only {C}{G}{U} when you remove them all.")
        ruling("2017-04-18", "If Bounty of the Luxa leaves the battlefield while its triggered ability is on the stack, you can't remove any flood counters from it, even if it had one before it left the battlefield. You won't put one on it as the ability resolves, but you will draw a card.")
    }
}
