package com.wingedsheep.mtg.sets.definitions.mh3.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Bespoke Battlewagon
 * {3}{U}
 * Artifact — Vehicle
 * 5/6
 *
 * {T}: You get {E}{E} (two energy counters).
 * {T}, Pay {E}{E}: Tap target creature.
 * {T}, Pay {E}{E}{E}: Draw a card.
 * Pay {E}{E}{E}{E}: This Vehicle becomes an artifact creature until end of turn.
 * Crew 4
 *
 * The {T} abilities are only subject to summoning sickness while the Vehicle is a creature
 * (ruling) — the engine already gates {T} costs on projected creature-ness, so nothing extra
 * is needed. The self-animate is [Effects.BecomeCreature] on self at the printed 5/6, the same
 * shape crew produces; a Vehicle is already an artifact, so no type is added.
 */
val BespokeBattlewagon = card("Bespoke Battlewagon") {
    manaCost = "{3}{U}"
    colorIdentity = "U"
    typeLine = "Artifact — Vehicle"
    power = 5
    toughness = 6
    oracleText = "{T}: You get {E}{E} (two energy counters).\n" +
        "{T}, Pay {E}{E}: Tap target creature.\n" +
        "{T}, Pay {E}{E}{E}: Draw a card.\n" +
        "Pay {E}{E}{E}{E}: This Vehicle becomes an artifact creature until end of turn.\n" +
        "Crew 4"

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.GetEnergy(2)
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Tap, Costs.PayPlayerCounters(CounterType.ENERGY, 2))
        val creature = target(TargetFilter.Creature)
        effect = Effects.Tap(creature)
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Tap, Costs.PayPlayerCounters(CounterType.ENERGY, 3))
        effect = Effects.DrawCards(1)
    }

    activatedAbility {
        cost = Costs.PayPlayerCounters(CounterType.ENERGY, 4)
        effect = Effects.BecomeCreature(
            target = EffectTarget.Self,
            power = 5,
            toughness = 6,
            duration = Duration.EndOfTurn
        )
        description = "This Vehicle becomes an artifact creature until end of turn."
    }

    keywordAbility(KeywordAbility.Numeric(Keyword.CREW, 4))

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "52"
        artist = "Zoltan Boros"
        imageUri = "https://cards.scryfall.io/normal/front/3/f/3feccebc-c792-420d-b96c-51494b5c41db.jpg?1783911293"

        ruling(
            "2024-06-07",
            "As long as Bespoke Battlewagon isn't a creature, its {T} abilities can be activated " +
                "even if you haven't controlled it continuously since the beginning of your most " +
                "recent turn. If it is a creature, however, it's still subject to \"summoning " +
                "sickness\" during that time period."
        )
    }
}
