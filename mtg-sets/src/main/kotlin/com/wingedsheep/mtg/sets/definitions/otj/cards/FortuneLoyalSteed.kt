package com.wingedsheep.mtg.sets.definitions.otj.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.CreateDelayedTriggerEffect

/**
 * Fortune, Loyal Steed
 * {2}{W}
 * Legendary Creature — Beast Mount
 * 2/4
 *
 * When Fortune enters, scry 2.
 * Whenever Fortune attacks while saddled, at end of combat, exile it and up to one creature
 * that saddled it this turn, then return those cards to the battlefield under their owner's
 * control.
 * Saddle 1 (Tap any number of other creatures you control with total power 1 or more: This Mount
 * becomes saddled until end of turn. Saddle only as a sorcery.)
 *
 * "While saddled" is the intervening-if [Conditions.SourceIsSaddled] (CR 603.4) on the attack
 * trigger. The attack trigger doesn't blink immediately — it schedules a delayed
 * [Step.END_COMBAT] trigger ([CreateDelayedTriggerEffect]) that does the exile/return, so the
 * creatures stay in combat and the blink happens after damage.
 *
 * The delayed trigger is a linked-exile flicker (CR 603.6e / 707-style blink): it gathers the
 * creatures that saddled Fortune this turn ([CardSource.CreaturesThatSaddledSource], read off
 * Fortune's crew/saddle-contributors record) and lets the controller pick **up to one**; it then
 * exiles that chosen saddler and Fortune itself ([CardSource.Self]) linked to Fortune, and finally
 * returns everything from the linked exile to the battlefield under each card's owner's control
 * ([Patterns.Exile.returnLinkedExile] with `underOwnersControl = true`). A creature that already
 * left play is dropped by the saddler gather, and if Fortune itself has left, its linked exile is
 * empty and the return is a harmless no-op.
 */
val FortuneLoyalSteed = card("Fortune, Loyal Steed") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Legendary Creature — Beast Mount"
    power = 2
    toughness = 4
    oracleText = "When Fortune enters, scry 2.\n" +
        "Whenever Fortune attacks while saddled, at end of combat, exile it and up to one " +
        "creature that saddled it this turn, then return those cards to the battlefield under " +
        "their owner's control.\n" +
        "Saddle 1 (Tap any number of other creatures you control with total power 1 or more: " +
        "This Mount becomes saddled until end of turn. Saddle only as a sorcery.)"

    keywordAbility(KeywordAbility.saddle(1))

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Patterns.Library.scry(2)
    }

    triggeredAbility {
        trigger = Triggers.Attacks
        triggerCondition = Conditions.SourceIsSaddled
        effect = CreateDelayedTriggerEffect(
            step = Step.END_COMBAT,
            effect = Effects.Pipeline {
                // Up to one of the creatures that saddled Fortune this turn.
                val saddlers = gather(CardSource.CreaturesThatSaddledSource)
                val chosenSaddler = chooseUpTo(
                    1,
                    from = saddlers,
                    useTargetingUI = true,
                    prompt = "Choose up to one creature that saddled Fortune this turn to exile with it"
                )
                // Exile Fortune itself and the chosen saddler, linked to Fortune.
                val self = gather(CardSource.Self)
                exile(self, linkToSource = true)
                exile(chosenSaddler, linkToSource = true)
                // Return everything Fortune exiled, under each card's owner's control.
                val returning = gather(CardSource.FromLinkedExile())
                move(
                    returning,
                    CardDestination.ToZone(Zone.BATTLEFIELD),
                    underOwnersControl = true
                )
            }
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "12"
        artist = "Artur Nakhodkin"
        imageUri = "https://cards.scryfall.io/normal/front/0/6/069294a8-e65a-47af-942f-7e99d18658f2.jpg?1712355270"

        ruling("2024-04-12", "If Fortune leaves the battlefield before its triggered ability resolves at end of combat, the creature that saddled it (if any) is still exiled and returned.")
        ruling("2024-04-12", "The exiled cards return to the battlefield as new objects with no relation to their previous existence. Any Auras or Equipment that were attached to them are put into their owners' graveyards.")
    }
}
