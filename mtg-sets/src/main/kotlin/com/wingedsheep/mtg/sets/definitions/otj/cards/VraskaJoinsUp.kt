package com.wingedsheep.mtg.sets.definitions.otj.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.events.DamageType
import com.wingedsheep.sdk.scripting.events.RecipientFilter
import com.wingedsheep.sdk.scripting.effects.AddCountersEffect
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Vraska Joins Up
 * {B}{G}
 * Legendary Enchantment
 *
 * When Vraska Joins Up enters, put a deathtouch counter on each creature you control.
 * Whenever a legendary creature you control deals combat damage to a player, draw a card.
 *
 * Part of the OTJ "Joins Up" cycle of Legendary Enchantments: each has an ETB trigger
 * plus an ongoing "whenever a legendary creature you control …" triggered ability.
 *
 * The deathtouch counter grants the DEATHTOUCH keyword via the StateProjector's
 * KEYWORD_COUNTER_MAP, so it's modeled as a plain `AddCounters` over every creature
 * you control. The second ability is the generic outgoing-combat-damage trigger
 * bound to ANY source matching "legendary creature you control".
 */
val VraskaJoinsUp = card("Vraska Joins Up") {
    manaCost = "{B}{G}"
    colorIdentity = "BG"
    typeLine = "Legendary Enchantment"
    oracleText = "When Vraska Joins Up enters, put a deathtouch counter on each creature you control.\n" +
        "Whenever a legendary creature you control deals combat damage to a player, draw a card."

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Effects.ForEachInGroup(
            filter = GroupFilter.AllCreaturesYouControl,
            effect = AddCountersEffect(Counters.DEATHTOUCH, 1, EffectTarget.Self)
        )
    }

    triggeredAbility {
        trigger = Triggers.dealsDamage(
            damageType = DamageType.Combat,
            recipient = RecipientFilter.AnyPlayer,
            sourceFilter = GameObjectFilter.Creature.legendary().youControl(),
            binding = TriggerBinding.ANY
        )
        effect = Effects.DrawCards(1)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "236"
        artist = "Wylie Beckert"
        flavorText = "Letting Oko believe he held all the cards provided the perfect cover for her own scheme to unfold."
        imageUri = "https://cards.scryfall.io/normal/front/0/6/06e546c2-737e-4b17-bf60-3069b1ccdf31.jpg?1712356228"
    }
}
