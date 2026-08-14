package com.wingedsheep.mtg.sets.definitions.msh.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.conditions.Compare
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.scripting.values.EntityNumericProperty
import com.wingedsheep.sdk.scripting.values.EntityReference

/**
 * Hulkling, Burgeoning Bruiser — Marvel Super Heroes #173
 * {2}{G} · Legendary Creature — Kree Skrull Hero · Uncommon
 * 2/3
 *
 * Vigilance
 * Whenever another creature you control enters, if it has greater power or toughness than
 * Hulkling, put a +1/+1 counter on Hulkling.
 *
 * [Triggers.OtherCreatureEnters] is the "another creature you control enters" template (OTHER
 * binding + a `Creature.youControl()` filter), so Hulkling's own arrival never fires it.
 *
 * The "if it has greater power or toughness" clause is an intervening-if (CR 603.4): it is
 * checked both when the trigger would go on the stack and again on resolution, which is exactly
 * what `triggerCondition` provides. It compares the *triggering* creature against the **source**
 * — Jackal, Genius Geneticist's Triggering-vs-Source shape — with an OR over the two axes, since
 * either a greater power or a greater toughness is enough. Both sides read projected values, so
 * lords and counters on either creature count, and the resolution-time re-check means a
 * pumped-up Hulkling (or a shrunken newcomer) correctly does nothing.
 */
val HulklingBurgeoningBruiser = card("Hulkling, Burgeoning Bruiser") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Legendary Creature — Kree Skrull Hero"
    power = 2
    toughness = 3
    oracleText = "Vigilance\n" +
        "Whenever another creature you control enters, if it has greater power or toughness than " +
        "Hulkling, put a +1/+1 counter on Hulkling."

    keywords(Keyword.VIGILANCE)

    triggeredAbility {
        trigger = Triggers.OtherCreatureEnters
        triggerCondition = Conditions.Any(
            Compare(
                DynamicAmount.EntityProperty(EntityReference.Triggering, EntityNumericProperty.Power),
                ComparisonOperator.GT,
                DynamicAmount.EntityProperty(EntityReference.Source, EntityNumericProperty.Power)
            ),
            Compare(
                DynamicAmount.EntityProperty(EntityReference.Triggering, EntityNumericProperty.Toughness),
                ComparisonOperator.GT,
                DynamicAmount.EntityProperty(EntityReference.Source, EntityNumericProperty.Toughness)
            )
        )
        effect = Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
        description = "Whenever another creature you control enters, if it has greater power or " +
            "toughness than Hulkling, put a +1/+1 counter on Hulkling."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "173"
        artist = "Wero Gallo"
        flavorText = "\"We all named ourselves after the original Avengers. I picked the Hulk. Be " +
            "honest. Wouldn't you?\""
        imageUri = "https://cards.scryfall.io/normal/front/9/0/9018477f-a67b-4fa4-8661-11ab91fae863.jpg?1783902917"
    }
}
