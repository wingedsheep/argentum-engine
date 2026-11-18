package com.wingedsheep.mtg.sets.definitions.m15.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.PreventDamage
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Ajani Steadfast — Magic 2015 #1 (canonical printing; the earlier PS14 copy is a promo)
 * {3}{W} · Legendary Planeswalker — Ajani · Loyalty 4
 *
 * +1: Until end of turn, up to one target creature gets +1/+1 and gains first strike, vigilance,
 *     and lifelink.
 * −2: Put a +1/+1 counter on each creature you control and a loyalty counter on each other
 *     planeswalker you control.
 * −7: You get an emblem with "If a source would deal damage to you or a planeswalker you control,
 *     prevent all but 1 of that damage."
 *
 * The emblem holds a standing [PreventDamage.allBut] of its own (`ownedReplacementEffects`), so it
 * applies to each source separately and keeps working after Ajani is gone. "Each other planeswalker"
 * excludes Ajani itself; a creature planeswalker you control gets both counters (ruling).
 */
val AjaniSteadfast = card("Ajani Steadfast") {
    manaCost = "{3}{W}"
    colorIdentity = "W"
    typeLine = "Legendary Planeswalker — Ajani"
    startingLoyalty = 4
    oracleText = "+1: Until end of turn, up to one target creature gets +1/+1 and gains first strike, vigilance, and lifelink.\n" +
        "−2: Put a +1/+1 counter on each creature you control and a loyalty counter on each other planeswalker you control.\n" +
        "−7: You get an emblem with \"$AJANI_STEADFAST_EMBLEM\""

    loyaltyAbility(+1) {
        val creature = target(TargetFilter.Creature, optional = true)
        effect = Effects.ModifyStats(1, 1, creature) then
            Effects.GrantKeyword(Keyword.FIRST_STRIKE, creature) then
            Effects.GrantKeyword(Keyword.VIGILANCE, creature) then
            Effects.GrantKeyword(Keyword.LIFELINK, creature)
    }

    loyaltyAbility(-2) {
        effect = Effects.ForEachInGroup(
            GroupFilter.AllCreaturesYouControl,
            Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.IterationEntity)
        ) then Effects.ForEachInGroup(
            GroupFilter(GameObjectFilter.Planeswalker.youControl(), excludeSelf = true),
            Effects.AddCounters(CounterType.LOYALTY, 1, EffectTarget.IterationEntity)
        )
    }

    loyaltyAbility(-7) {
        effect = Effects.CreatePermanentEmblem(
            ownedReplacementEffects = listOf(
                PreventDamage(
                    allBut = 1,
                    appliesTo = EventPattern.DamageEvent(
                        recipient = Recipient.AnyOf(
                            listOf(Recipient.You, Recipient.Object(GameObjectFilter.Planeswalker.youControl()))
                        )
                    )
                )
            ),
            emblemDescription = AJANI_STEADFAST_EMBLEM,
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "1"
        artist = "Chris Rahn"
        imageUri = "https://cards.scryfall.io/normal/front/f/9/f9302ba8-7ff2-4eaa-a32d-eedd94f0bb79.jpg?1783939205"

        ruling("2014-07-18", "You may activate the first ability with no targets. If you do, you simply put a loyalty counter on Ajani to pay the cost of the ability.")
        ruling("2014-07-18", "If a permanent you control is both a creature and a planeswalker, Ajani's second ability will put both a +1/+1 counter and a loyalty counter on it.")
        ruling("2014-07-18", "The emblem's ability doesn't consider who controls the source dealing damage to you or a planeswalker you control.")
        ruling("2014-07-18", "Each attacking creature is a separate source of damage. For example, if two 3/3 creatures attack you and are unblocked, the emblem will prevent 2 damage from each creature. You'll be dealt a total of 2 damage.")
        ruling("2014-07-18", "If multiple effects modify how damage is dealt, the player being dealt damage or the controller of the permanent being dealt damage chooses the order to apply the effects. For example, Dictate of the Twin Gods says, \"If a source would deal damage to a permanent or player, it deals double that damage to that permanent or player instead.\" Suppose you would be dealt 3 damage while Dictate of the Twin Gods is on the battlefield and you control Ajani's emblem. You can either (a) prevent all but 1 damage first and then let Dictate of the Twin Gods's effect double that, for a result of being dealt 2 damage, or (b) double the damage to 6 and then prevent all but 1 of that damage.")
    }
}

private const val AJANI_STEADFAST_EMBLEM =
    "If a source would deal damage to you or a planeswalker you control, prevent all but 1 of that damage."
