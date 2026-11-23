package com.wingedsheep.mtg.sets.definitions.gtc.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.evolve
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Experiment One — Gatecrash #119
 * {G} · Creature — Human Ooze · 1/1
 *
 * Evolve
 * Remove two +1/+1 counters from this creature: Regenerate it.
 */
val ExperimentOne = card("Experiment One") {
    manaCost = "{G}"
    colorIdentity = "G"
    typeLine = "Creature — Human Ooze"
    power = 1
    toughness = 1
    oracleText = "Evolve (Whenever a creature you control enters, if that creature has greater power or " +
        "toughness than this creature, put a +1/+1 counter on this creature.)\n" +
        "Remove two +1/+1 counters from this creature: Regenerate it. (The next time this creature would be " +
        "destroyed this turn, instead tap it, remove it from combat, and heal all damage on it.)"

    evolve()

    activatedAbility {
        cost = Costs.RemoveCounterFromSelf(CounterType.PLUS_ONE_PLUS_ONE, 2)
        effect = Effects.Regenerate(EffectTarget.Self)
        description = "Remove two +1/+1 counters from this creature: Regenerate it."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "119"
        artist = "Chase Stone"
        imageUri = "https://cards.scryfall.io/normal/front/2/c/2cc1d8d0-bb43-4962-ad29-bb6478aa986b.jpg?1783940118"
        ruling(
            "2024-01-12",
            "Because damage remains marked on a creature until it's removed as the turn ends, nonlethal damage " +
                "dealt to Experiment One may become lethal if you remove +1/+1 counters from it during that turn. " +
                "In this case, it dies before the activated ability that would regenerate it resolves."
        )
        ruling(
            "2013-04-15",
            "If evolve triggers, the stat comparison will happen again when the ability tries to resolve. If " +
                "neither stat of the new creature is greater, the ability will do nothing. If the creature that " +
                "entered the battlefield leaves the battlefield before evolve tries to resolve, use its last known " +
                "power and toughness to compare the stats."
        )
        ruling(
            "2013-04-15",
            "If a creature enters the battlefield with +1/+1 counters on it, consider those counters when " +
                "determining if evolve will trigger. For example, a 1/1 creature that enters the battlefield with " +
                "two +1/+1 counters on it will cause the evolve ability of a 2/2 creature to trigger."
        )
    }
}
