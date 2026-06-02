package com.wingedsheep.mtg.sets.definitions.eoe.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EventPattern.OneOrMoreDealCombatDamageToPlayerEvent
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.TriggerSpec

/**
 * Haliya, Ascendant Cadet
 * {2}{G}{W}{W}
 * Legendary Creature — Human Soldier
 * 3/3
 *
 * Whenever Haliya enters or attacks, put a +1/+1 counter on target creature you control.
 * Whenever one or more creatures you control with +1/+1 counters on them deal combat damage
 * to a player, draw a card.
 */
val HaliyaAscendantCadet = card("Haliya, Ascendant Cadet") {
    manaCost = "{2}{G}{W}{W}"
    colorIdentity = "GW"
    typeLine = "Legendary Creature — Human Soldier"
    power = 3
    toughness = 3
    oracleText = "Whenever Haliya enters or attacks, put a +1/+1 counter on target creature you control.\n" +
        "Whenever one or more creatures you control with +1/+1 counters on them deal combat damage to a player, draw a card."

    // Whenever Haliya enters or attacks, put a +1/+1 counter on target creature you control.
    // No combined "enters or attacks" trigger exists, so model it as two abilities sharing the effect.
    val counterDescription = "put a +1/+1 counter on target creature you control"

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        val t = target("target creature you control", Targets.CreatureYouControl)
        effect = Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 1, t)
        description = counterDescription
    }

    triggeredAbility {
        trigger = Triggers.Attacks
        val t = target("target creature you control", Targets.CreatureYouControl)
        effect = Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 1, t)
        description = counterDescription
    }

    // Whenever one or more creatures you control with +1/+1 counters on them deal combat damage
    // to a player, draw a card. Batching trigger — fires at most once per combat damage event.
    // The +1/+1 counter must be present at the time damage is dealt (see ruling).
    triggeredAbility {
        trigger = TriggerSpec(
            OneOrMoreDealCombatDamageToPlayerEvent(
                sourceFilter = GameObjectFilter.Creature.youControl()
                    .withCounter(Counters.PLUS_ONE_PLUS_ONE)
            ),
            TriggerBinding.ANY
        )
        effect = Effects.DrawCards(1)
        description = "Whenever one or more creatures you control with +1/+1 counters on them " +
            "deal combat damage to a player, draw a card."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "218"
        artist = "Justyna Dura"
        flavorText = "\"By the eternal and everflowing light, I swear myself to this cause.\""
        imageUri = "https://cards.scryfall.io/normal/front/6/8/683d6eba-7f98-4105-b318-7f2290012f32.jpg?1752947448"

        ruling(
            "2025-07-25",
            "A creature that deals combat damage to a player must have a +1/+1 counter on it at " +
                "the time damage is dealt in order for Haliya, Ascendant Cadet's last ability to trigger."
        )
    }
}
