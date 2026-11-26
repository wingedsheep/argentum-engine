package com.wingedsheep.mtg.sets.definitions.m13.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantAttackUnless
import com.wingedsheep.sdk.scripting.CantBlockUnless
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Slumbering Dragon — Magic 2013 #148
 * {R} · Creature — Dragon 3/3
 *
 * Flying
 * This creature can't attack or block unless it has five or more +1/+1 counters on it.
 * Whenever a creature attacks you or a planeswalker you control, put a +1/+1 counter on this creature.
 *
 * Canonical printing: Magic 2013, the card's earliest real printing.
 *
 * "Can't attack or block" is two restrictions over one condition, Rhonas the Indomitable's shape;
 * the restrictions evaluate with the Dragon as source, so the counter count is its own. The attack
 * trigger is Isperia's — once per creature declared attacking you or your planeswalkers — and it
 * resolves before blockers are declared, so the fifth counter lets it block that same combat.
 */
val SlumberingDragon = card("Slumbering Dragon") {
    manaCost = "{R}"
    colorIdentity = "R"
    typeLine = "Creature — Dragon"
    power = 3
    toughness = 3
    oracleText = "Flying\nThis creature can't attack or block unless it has five or more +1/+1 counters on it.\n" +
        "Whenever a creature attacks you or a planeswalker you control, put a +1/+1 counter on this creature."

    keywords(Keyword.FLYING)

    val awake = Conditions.SourceCounterCountAtLeast(CounterType.PLUS_ONE_PLUS_ONE, 5)

    staticAbility {
        ability = CantAttackUnless(awake)
    }
    staticAbility {
        ability = CantBlockUnless(awake)
    }

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Creature.attackingYouOrYourPlaneswalkers()).attacks()
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "148"
        artist = "Chris Rahn"
        imageUri = "https://cards.scryfall.io/normal/front/2/7/277cbd0d-c8da-4a37-965c-6a60771df2f7.jpg?1783940479"
        ruling("2012-07-01", "Slumbering Dragon's second ability counts all +1/+1 counters on it, not just the ones added by its third ability.")
        ruling("2012-07-01", "The +1/+1 counter is put on Slumbering Dragon before blockers are declared. This may allow Slumbering Dragon to \"wake up\" and block the creature that attacked and caused its ability to trigger.")
        ruling("2012-07-01", "Slumbering Dragon's third ability will trigger once for each creature that attacks you or a planeswalker you control.")
    }
}
