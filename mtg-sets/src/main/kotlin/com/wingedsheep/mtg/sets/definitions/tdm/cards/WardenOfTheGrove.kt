package com.wingedsheep.mtg.sets.definitions.tdm.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EventPattern.ZoneChangeEvent
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.TriggerSpec
import com.wingedsheep.sdk.scripting.events.CounterTypeFilter
import com.wingedsheep.sdk.scripting.predicates.CardPredicate
import com.wingedsheep.sdk.scripting.predicates.ControllerPredicate
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.scripting.values.EntityNumericProperty
import com.wingedsheep.sdk.scripting.values.EntityReference

/**
 * Warden of the Grove — Tarkir: Dragonstorm #166
 * {2}{G} · Creature — Hydra · 2/2
 *
 * At the beginning of your end step, put a +1/+1 counter on this creature.
 * Whenever another nontoken creature you control enters, it endures X, where X
 * is the number of counters on this creature. (Put X +1/+1 counters on the
 * creature that entered or create an X/X white Spirit creature token.)
 *
 * The second ability endures the *entering* creature ([EffectTarget.TriggeringEntity]),
 * not Warden itself, with N read dynamically as the number of counters on Warden
 * ([DynamicAmount.EntityProperty] of [EntityReference.Source]).
 */
val WardenOfTheGrove = card("Warden of the Grove") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Hydra"
    power = 2
    toughness = 2
    oracleText = "At the beginning of your end step, put a +1/+1 counter on this creature.\n" +
        "Whenever another nontoken creature you control enters, it endures X, where X is the number of counters on this creature. " +
        "(Put X +1/+1 counters on the creature that entered or create an X/X white Spirit creature token.)"

    triggeredAbility {
        trigger = Triggers.YourEndStep
        effect = Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
        description = "At the beginning of your end step, put a +1/+1 counter on this creature."
    }

    triggeredAbility {
        trigger = TriggerSpec(
            event = ZoneChangeEvent(
                filter = GameObjectFilter(
                    cardPredicates = listOf(CardPredicate.IsCreature, CardPredicate.IsNontoken),
                    controllerPredicate = ControllerPredicate.ControlledByYou
                ),
                to = Zone.BATTLEFIELD
            ),
            binding = TriggerBinding.OTHER
        )
        effect = Effects.Endure(
            amount = DynamicAmount.EntityProperty(
                EntityReference.Source,
                EntityNumericProperty.CounterCount(CounterTypeFilter.Any)
            ),
            target = EffectTarget.TriggeringEntity
        )
        description = "Whenever another nontoken creature you control enters, it endures X, " +
            "where X is the number of counters on this creature."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "166"
        artist = "Alexander Ostrowski"
        imageUri = "https://cards.scryfall.io/normal/front/2/4/2414db96-0e2b-4f7c-9b97-41f8e310b752.jpg?1743697653"
    }
}
