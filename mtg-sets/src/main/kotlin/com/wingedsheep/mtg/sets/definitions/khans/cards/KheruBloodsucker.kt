package com.wingedsheep.mtg.sets.definitions.khans.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameEvent.ZoneChangeEvent
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.TriggerSpec
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Kheru Bloodsucker
 * {2}{B}
 * Creature — Vampire
 * 2/2
 * Whenever a creature you control with toughness 4 or greater dies, each opponent loses 2 life
 * and you gain 2 life.
 * {2}{B}, Sacrifice another creature: Put a +1/+1 counter on Kheru Bloodsucker.
 */
val KheruBloodsucker = card("Kheru Bloodsucker") {
    manaCost = "{2}{B}"
    typeLine = "Creature — Vampire"
    power = 2
    toughness = 2
    oracleText = "Whenever a creature you control with toughness 4 or greater dies, each opponent loses 2 life and you gain 2 life.\n{2}{B}, Sacrifice another creature: Put a +1/+1 counter on Kheru Bloodsucker."

    triggeredAbility {
        trigger = TriggerSpec(
            event = ZoneChangeEvent(
                filter = GameObjectFilter.Creature.youControl().toughnessAtLeast(4),
                from = Zone.BATTLEFIELD,
                to = Zone.GRAVEYARD
            ),
            binding = TriggerBinding.ANY
        )
        effect = Effects.LoseLife(2, EffectTarget.PlayerRef(Player.EachOpponent))
            .then(Effects.GainLife(2))
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}{B}"), Costs.SacrificeAnother(GameObjectFilter.Creature))
        effect = Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "75"
        artist = "Daniel Ljunggren"
        flavorText = "It stares through the empty, pain-twisted faces of those it has drained."
        imageUri = "https://cards.scryfall.io/normal/front/b/a/baf15cd4-13be-48e7-b3f5-a5106eb02c45.jpg?1562792642"
    }
}
