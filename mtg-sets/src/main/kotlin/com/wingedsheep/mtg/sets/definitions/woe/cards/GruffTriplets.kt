package com.wingedsheep.mtg.sets.definitions.woe.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.scripting.values.EntityNumericProperty
import com.wingedsheep.sdk.scripting.values.EntityReference

/**
 * Gruff Triplets
 * {3}{G}{G}{G}
 * Creature — Satyr Warrior
 * 3/3
 *
 * Trample
 * When this creature enters, if it isn't a token, create two tokens that are copies of it.
 * When this creature dies, put a number of +1/+1 counters equal to its power on each creature
 * you control named Gruff Triplets.
 *
 * The "if it isn't a token" clause is a genuine intervening-if (CR 603.4) — `triggerCondition`,
 * so it's checked both when the trigger would fire and again on resolution — and it's what stops
 * the card from being an infinite token engine: the two copies enter as tokens, so their own
 * copy trigger never fires.
 *
 * The death trigger reads "its power" off the dying permanent, which is already in the graveyard
 * by resolution, so the amount is [EntityReference.Source] — a last-known-information read
 * (`LkiPolicy.LIVE_THEN_LKI`, CR 608.2h) that picks up any +1/+1 counters or pumps the dying
 * body had. The dying Triplet is not itself in "each creature you control named Gruff Triplets"
 * (that set is enumerated live off the battlefield), so a lone Triplet dying does nothing while
 * the first of three to die hands 3 counters to each of its two siblings — and their deaths then
 * feed the survivor in turn.
 */
val GruffTriplets = card("Gruff Triplets") {
    manaCost = "{3}{G}{G}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Satyr Warrior"
    power = 3
    toughness = 3
    oracleText = "Trample\n" +
        "When this creature enters, if it isn't a token, create two tokens that are copies of it.\n" +
        "When this creature dies, put a number of +1/+1 counters equal to its power on each " +
        "creature you control named Gruff Triplets."

    keywords(Keyword.TRAMPLE)

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        triggerCondition = Conditions.SourceMatches(GameObjectFilter.Any.nontoken())
        effect = Effects.CreateTokenCopyOfSelf(count = 2)
        description = "When this creature enters, if it isn't a token, create two tokens that " +
            "are copies of it."
    }

    triggeredAbility {
        trigger = Triggers.Dies
        effect = Effects.ForEachInGroup(
            filter = GroupFilter(GameObjectFilter.Creature.youControl().named("Gruff Triplets")),
            effect = Effects.AddDynamicCounters(
                counterType = Counters.PLUS_ONE_PLUS_ONE,
                amount = DynamicAmount.EntityProperty(
                    entity = EntityReference.Source,
                    numericProperty = EntityNumericProperty.Power,
                ),
                target = EffectTarget.Self,
            ),
        )
        description = "When this creature dies, put a number of +1/+1 counters equal to its " +
            "power on each creature you control named Gruff Triplets."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "172"
        artist = "Fajareka Setiawan"
        imageUri = "https://cards.scryfall.io/normal/front/f/8/f8760ab9-ac76-4e2e-b82f-0ee2a6dc5634.jpg?1783915082"
    }
}
