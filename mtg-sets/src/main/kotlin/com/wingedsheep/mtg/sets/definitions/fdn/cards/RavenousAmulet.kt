package com.wingedsheep.mtg.sets.definitions.fdn.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.events.CounterTypeFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Ravenous Amulet
 * {2}
 * Artifact
 *
 * {1}, {T}, Sacrifice a creature: Draw a card and put a soul counter on this artifact.
 *   Activate only as a sorcery.
 * {4}, {T}, Sacrifice this artifact: Each opponent loses life equal to the number of soul
 *   counters on this artifact.
 *
 * "Soul" is a passive storage counter (see [Counters.SOUL]) — no inherent rule; the first ability
 * accumulates it and the second reads the count.
 */
val RavenousAmulet = card("Ravenous Amulet") {
    manaCost = "{2}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "{1}, {T}, Sacrifice a creature: Draw a card and put a soul counter on this artifact. " +
        "Activate only as a sorcery.\n" +
        "{4}, {T}, Sacrifice this artifact: Each opponent loses life equal to the number of soul " +
        "counters on this artifact."

    // {1}, {T}, Sacrifice a creature: Draw a card and put a soul counter on this artifact.
    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{1}"),
            Costs.Tap,
            Costs.Sacrifice(GameObjectFilter.Creature)
        )
        effect = Effects.Composite(
            Effects.DrawCards(1),
            Effects.AddCounters(Counters.SOUL, 1, EffectTarget.Self)
        )
        timing = TimingRule.SorcerySpeed
    }

    // {4}, {T}, Sacrifice this artifact: Each opponent loses life equal to the soul counters here.
    // SacrificeSelf wipes the amulet's counters before this effect resolves, so the count is read as
    // last-known information (CR 112.7a) rather than off the (now-graveyard) source.
    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{4}"),
            Costs.Tap,
            Costs.SacrificeSelf
        )
        effect = Effects.LoseLife(
            amount = DynamicAmounts.lastKnownSourceCounters(CounterTypeFilter.Named(Counters.SOUL)),
            target = EffectTarget.PlayerRef(Player.EachOpponent)
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "131"
        artist = "Igor Krstic"
        flavorText = "Each of its owners' final moments is remembered within the roiling stone."
        imageUri = "https://cards.scryfall.io/normal/front/8/0/80cadee5-6f26-4440-ad31-a8e573a90436.jpg?1783909089"
    }
}
