package com.wingedsheep.mtg.sets.definitions.bloomburrow.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Tangle Tumbler {3}
 * Artifact — Vehicle
 * 6/6
 *
 * Vigilance
 * {3}, {T}: Put a +1/+1 counter on target creature.
 * Tap two untapped tokens you control: This Vehicle becomes an artifact creature until end of turn.
 */
val TangleTumbler = card("Tangle Tumbler") {
    manaCost = "{3}"
    typeLine = "Artifact — Vehicle"
    oracleText = "Vigilance\n{3}, {T}: Put a +1/+1 counter on target creature.\nTap two untapped tokens you control: This Vehicle becomes an artifact creature until end of turn."
    power = 6
    toughness = 6

    keywords(Keyword.VIGILANCE)

    // {3}, {T}: Put a +1/+1 counter on target creature.
    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{3}"), Costs.Tap)
        val creature = target("target creature", Targets.Creature)
        effect = Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 1, creature)
    }

    // Tap two untapped tokens you control: This Vehicle becomes an artifact creature until end of turn.
    activatedAbility {
        cost = Costs.TapPermanents(2, GameObjectFilter.Token)
        effect = Effects.BecomeCreature(
            target = EffectTarget.Self,
            power = 6,
            toughness = 6,
            duration = Duration.EndOfTurn
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "250"
        artist = "Victor Adame Minguez"
        imageUri = "https://cards.scryfall.io/normal/front/2/5/258ef349-5042-4992-bae9-9f8f54b55db0.jpg?1721427304"
    }
}
