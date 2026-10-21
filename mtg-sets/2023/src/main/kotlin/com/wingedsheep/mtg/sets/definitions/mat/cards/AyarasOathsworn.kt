package com.wingedsheep.mtg.sets.definitions.mat.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.effects.SearchDestination
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Ayara's Oathsworn — March of the Machine: The Aftermath #11
 * {1}{B} · Creature — Human Knight · 2/2
 *
 * Menace
 * Whenever this creature deals combat damage to a player, if it has fewer than four +1/+1
 * counters on it, put a +1/+1 counter on it. Then if it has exactly four +1/+1 counters on it,
 * search your library for a card, put it into your hand, then shuffle.
 *
 * "Fewer than four" is an intervening-if (CR 603.4): checked when it triggers and again on
 * resolution. The "exactly four" check is a plain resolution-time `If` that runs *after* the
 * counter lands, so it reads the new count. If the creature has left the battlefield the counter
 * can't be put on it, so the count can never reach four and there is no search (third ruling).
 */
val AyarasOathsworn = card("Ayara's Oathsworn") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Human Knight"
    power = 2
    toughness = 2
    oracleText = "Menace\n" +
        "Whenever this creature deals combat damage to a player, if it has fewer than four +1/+1 " +
        "counters on it, put a +1/+1 counter on it. Then if it has exactly four +1/+1 counters on " +
        "it, search your library for a card, put it into your hand, then shuffle."

    keywords(Keyword.MENACE)

    triggeredAbility {
        trigger = Triggers.self.dealsCombatDamage(Recipient.AnyPlayer)
        interveningIf = Conditions.CompareAmounts(
            DynamicAmounts.countersOnSelf(CounterType.PLUS_ONE_PLUS_ONE),
            ComparisonOperator.LT,
            4,
        )
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self) then
            Effects.If(
                condition = Conditions.CompareAmounts(
                    DynamicAmounts.countersOnSelf(CounterType.PLUS_ONE_PLUS_ONE),
                    ComparisonOperator.EQ,
                    4,
                ),
                then = Patterns.Library.searchLibrary(
                    filter = GameObjectFilter.Any,
                    destination = SearchDestination.HAND,
                ),
            )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "11"
        artist = "Johann Bodin"
        imageUri = "https://cards.scryfall.io/normal/front/5/3/5330e6b7-eca3-46a4-8905-8f1de16f76af.jpg?1783916526"
        ruling(
            "2023-05-12",
            "If Ayara's Oathsworn has four or more +1/+1 counters on it as it deals combat damage to a " +
                "player, its ability won't trigger at all. None of its effects will happen. You won't put a " +
                "+1/+1 counter on Ayara's Oathsworn and you won't search for a card.",
        )
        ruling(
            "2023-05-12",
            "If the triggered ability does trigger, that ability will check Ayara's Oathsworn again as it " +
                "tries to resolve. If Ayara's Oathsworn has four or more +1/+1 counters on it at that time, " +
                "the ability won't resolve. None of its effects will happen. You won't put a +1/+1 counter " +
                "on Ayara's Oathsworn and you won't search for a card.",
        )
        ruling(
            "2023-05-12",
            "If Ayara's Oathsworn isn't on the battlefield as its ability tries to resolve, you won't " +
                "search for a card, even if Ayara's Oathsworn had three +1/+1 counters on it before if left " +
                "the battlefield, because it won't be around to have the fourth +1/+1 counter put on it.",
        )
    }
}
