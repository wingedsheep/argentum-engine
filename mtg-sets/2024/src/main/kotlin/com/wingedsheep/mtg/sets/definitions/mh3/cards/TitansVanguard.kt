package com.wingedsheep.mtg.sets.definitions.mh3.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.predicates.CardPredicate
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Titans' Vanguard {3}{R}{G} — Modern Horizons 3 #206 (uncommon)
 * Creature — Eldrazi 5/5
 * Devoid
 * When you cast this spell and whenever this creature attacks, put a +1/+1 counter on each
 * colorless creature you control.
 * Trample
 *
 * One sentence, two triggers: the cast trigger and the attack trigger share the same untargeted
 * `ForEachInGroup` over colorless creatures you control (read off projected state, so devoid and
 * face-down permanents count). The cast trigger resolves before the spell, so the Vanguard itself
 * never gets a counter from it.
 */
private val putCounterOnEachColorless = Effects.ForEachInGroup(
    GroupFilter(
        GameObjectFilter.Creature
            .withCardPredicate(CardPredicate.IsColorless)
            .youControl(),
    ),
    Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.IterationEntity),
)

val TitansVanguard = card("Titans' Vanguard") {
    manaCost = "{3}{R}{G}"
    colorIdentity = "RG"
    typeLine = "Creature — Eldrazi"
    power = 5
    toughness = 5
    oracleText = "Devoid (This card has no color.)\n" +
        "When you cast this spell and whenever this creature attacks, put a +1/+1 counter on each " +
        "colorless creature you control.\n" +
        "Trample"

    keywords(Keyword.DEVOID, Keyword.TRAMPLE)

    triggeredAbility {
        trigger = Triggers.self.isCast()
        effect = putCounterOnEachColorless
        description = "When you cast this spell, put a +1/+1 counter on each colorless creature you control."
    }

    triggeredAbility {
        trigger = Triggers.self.attacks()
        effect = putCounterOnEachColorless
        description = "Whenever this creature attacks, put a +1/+1 counter on each colorless creature you control."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "206"
        artist = "Richard Kane Ferguson"
        flavorText = "With a shriek, it directed the spawn toward the temple where the refugees sheltered."
        imageUri = "https://cards.scryfall.io/normal/front/f/b/fb3ea14e-44ac-4f69-bfea-cb6bf1bfbd74.jpg?1783911244"
        ruling("2024-06-07", "When you cast Titans' Vanguard, its triggered ability will resolve before Titans' Vanguard does. If Titans' Vanguard is countered or otherwise leaves the stack in response to that triggered ability, the triggered ability will still resolve as normal.")
        ruling("2024-06-07", "Since Titans' Vanguard's triggered ability resolves before Titans' Vanguard does, it won't be on the battlefield when that ability resolves, and it won't receive a +1/+1 counter.")
    }
}
