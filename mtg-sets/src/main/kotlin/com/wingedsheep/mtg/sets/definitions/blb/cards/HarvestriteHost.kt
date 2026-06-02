package com.wingedsheep.mtg.sets.definitions.blb.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EventPattern.ZoneChangeEvent
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.TriggerSpec
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
import com.wingedsheep.sdk.scripting.effects.IncrementAbilityResolutionCountEffect

/**
 * Harvestrite Host
 * {2}{W}
 * Creature — Rabbit Citizen
 * 3/3
 *
 * Whenever this creature or another Rabbit you control enters, target creature you control
 * gets +1/+0 until end of turn. Then draw a card if this is the second time this ability
 * has resolved this turn.
 */
val HarvestriteHost = card("Harvestrite Host") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Rabbit Citizen"
    power = 3
    toughness = 3
    oracleText = "Whenever this creature or another Rabbit you control enters, target creature you control gets +1/+0 until end of turn. Then draw a card if this is the second time this ability has resolved this turn."

    triggeredAbility {
        trigger = TriggerSpec(
            event = ZoneChangeEvent(
                filter = GameObjectFilter.Creature
                    .withSubtype(Subtype("Rabbit"))
                    .youControl(),
                to = Zone.BATTLEFIELD
            ),
            binding = TriggerBinding.ANY
        )
        val creature = target("creature you control", Targets.CreatureYouControl)
        effect = Effects.ModifyStats(1, 0, creature)
            .then(IncrementAbilityResolutionCountEffect)
            .then(
                ConditionalEffect(
                    condition = Conditions.SourceAbilityResolvedNTimes(2),
                    effect = Effects.DrawCards(1)
                )
            )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "15"
        artist = "Julie Dillon"
        flavorText = "\"Many paws grew this feast, and many paws should share it!\""
        imageUri = "https://cards.scryfall.io/normal/front/4/1/41762689-0c13-4d45-9d81-ba2afad980f8.jpg?1721425845"
        ruling("2024-07-26", "If the target creature is an illegal target as Harvestrite Host's ability tries to resolve, it won't resolve and none of its effects will happen. It won't count toward the number of times the ability has resolved this turn.")
    }
}
