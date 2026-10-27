package com.wingedsheep.mtg.sets.definitions.khm.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.predicates.CardPredicate
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Sarulf, Realm Eater
 * {1}{B}{G}
 * Legendary Creature — Wolf
 * 3/3
 *
 * Whenever a permanent an opponent controls is put into a graveyard from the battlefield, put a
 * +1/+1 counter on Sarulf.
 * At the beginning of your upkeep, if Sarulf has one or more +1/+1 counters on it, you may remove
 * all of them. If you do, exile each other nonland permanent with mana value less than or equal to
 * the number of counters removed this way.
 *
 * The upkeep "if" is an intervening-if (CR 603.4): it gates the trigger and is checked again on
 * resolution, so a Sarulf that lost its counters — or left the battlefield, becoming a new object
 * with none — does nothing, and the exile can never run with a count of zero.
 *
 * "The number of counters removed this way" is captured with `storeNumber` *before* the removal
 * (Ashling the Pilgrim's shape); reading `countersOnSelf` afterwards would see 0. Only
 * +1/+1 counters are counted and removed — any other counters on Sarulf stay (the card's ruling).
 */
val SarulfRealmEater = card("Sarulf, Realm Eater") {
    manaCost = "{1}{B}{G}"
    colorIdentity = "BG"
    typeLine = "Legendary Creature — Wolf"
    power = 3
    toughness = 3
    oracleText = "Whenever a permanent an opponent controls is put into a graveyard from the battlefield, " +
        "put a +1/+1 counter on Sarulf.\n" +
        "At the beginning of your upkeep, if Sarulf has one or more +1/+1 counters on it, you may remove " +
        "all of them. If you do, exile each other nonland permanent with mana value less than or equal to " +
        "the number of counters removed this way."

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Permanent.opponentControls()).dies()
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
        description = "Whenever a permanent an opponent controls is put into a graveyard from the " +
            "battlefield, put a +1/+1 counter on Sarulf."
    }

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.UPKEEP)
        interveningIf = Conditions.SourceCounterCountAtLeast(CounterType.PLUS_ONE_PLUS_ONE, 1)
        optional = true
        effect = Effects.Pipeline {
            val removed = storeNumber(
                DynamicAmounts.countersOnSelf(CounterType.PLUS_ONE_PLUS_ONE),
                name = "sarulfRemovedCounters",
            )
            run(Effects.RemoveAllCountersOfType(CounterType.PLUS_ONE_PLUS_ONE, EffectTarget.Self))
            val victims = gather(
                GameObjectFilter(
                    cardPredicates = GameObjectFilter.NonlandPermanent.cardPredicates +
                        CardPredicate.ManaValueAtMostDynamic(removed.amount)
                ),
                excludeSelf = true,
            )
            exile(victims)
        }
        description = "At the beginning of your upkeep, if Sarulf has one or more +1/+1 counters on it, " +
            "you may remove all of them. If you do, exile each other nonland permanent with mana value " +
            "less than or equal to the number of counters removed this way."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "228"
        artist = "Chris Rahn"
        imageUri = "https://cards.scryfall.io/normal/front/b/c/bcecf9ba-36da-490a-9460-99ff87d99fbd.jpg?1783928191"
        ruling("2021-02-05", "It doesn't matter which graveyard the permanent is put into as long as an opponent controlled it when it last existed on the battlefield.")
        ruling("2021-02-05", "If Sarulf doesn't have any +1/+1 counters on it as your upkeep begins, its last ability won't trigger at all. If the ability does trigger, it will check again as it tries to resolve. If Sarulf doesn't have any +1/+1 counters on it at that time, the ability won't do anything. No other permanents will be exiled.")
        ruling("2021-02-05", "If Sarulf isn't on the battlefield as its last ability starts to resolve, you can't remove any +1/+1 counters from it, even if it had +1/+1 counters when it was last on the battlefield. No other permanents will be exiled.")
        ruling("2021-02-05", "Choosing to remove all the +1/+1 counters on Sarulf won't affect any other counters it may have.")
        ruling("2021-02-05", "If you do remove +1/+1 counters, you must remove all of them. You can't fine-tune the carnage by only removing some of them.")
        ruling("2021-02-05", "You may choose to remove Sarulf's +1/+1 counters even if that would cause lethal damage to be marked on Sarulf. If you do this, Sarulf will die after its last ability is finished resolving.")
    }
}
