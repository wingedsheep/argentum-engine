package com.wingedsheep.mtg.sets.definitions.dtk.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.CollectionFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Sunbringer's Touch
 * {2}{G}{G}
 * Sorcery
 * Bolster X, where X is the number of cards in your hand. Each creature you control with a +1/+1
 * counter on it gains trample until end of turn. (To bolster X, choose a creature with the least
 * toughness among creatures you control and put X +1/+1 counters on it.)
 *
 * `Patterns.Mechanic.bolster` takes a fixed count, so the bolster is spelled out as its own
 * pipeline (gather your creatures → keep the least toughness → choose one → put X counters on it)
 * with X read when the counters are placed — at resolution, with this spell already off your hand.
 * The trample group is gathered only after the bolster, so the bolstered creature is in it; it is
 * fixed at that moment, so a creature that gets a counter later in the turn doesn't gain trample.
 */
val SunbringersTouch = card("Sunbringer's Touch") {
    manaCost = "{2}{G}{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "Bolster X, where X is the number of cards in your hand. Each creature you control with " +
        "a +1/+1 counter on it gains trample until end of turn. (To bolster X, choose a creature with the " +
        "least toughness among creatures you control and put X +1/+1 counters on it.)"

    spell {
        effect = Effects.Pipeline {
            val creatures = gather(CardSource.ControlledPermanents(Player.You, GameObjectFilter.Creature))
            val leastToughness = filter(creatures, CollectionFilter.LeastToughness)
            val bolstered = chooseExactly(
                1,
                from = leastToughness,
                prompt = "Bolster X (X = cards in your hand) — choose a creature with the least toughness",
                useTargetingUI = true,
            )
            run(
                Effects.AddCountersToCollection(
                    bolstered,
                    CounterType.PLUS_ONE_PLUS_ONE,
                    DynamicAmounts.cardsInYourHand(),
                )
            )
        } then Effects.ForEachInGroup(
            GroupFilter(GameObjectFilter.Creature.withCounter(CounterType.PLUS_ONE_PLUS_ONE).youControl()),
            Effects.GrantKeyword(Keyword.TRAMPLE, EffectTarget.IterationEntity),
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "209"
        artist = "Lucas Graciano"
        imageUri = "https://cards.scryfall.io/normal/front/a/d/ad4d33ea-0193-4ea5-92ea-5f50a533bf6b.jpg?1783938574"
        ruling(
            "2015-02-25",
            "Count the number of cards in your hand as Sunbringer's Touch resolves to determine the value of X.",
        )
        ruling(
            "2015-02-25",
            "Bolster itself doesn't target any creature, though some spells and abilities that bolster may " +
                "have other effects that target creatures. For example, you could put counters on a creature " +
                "with protection from white with Aven Tactician's bolster ability.",
        )
        ruling(
            "2015-02-25",
            "You determine which creature to put counters on as the spell or ability that instructs you to " +
                "bolster resolves. That could be the creature with the bolster ability, if it's still under " +
                "your control and has the least toughness.",
        )
    }
}
