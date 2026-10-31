package com.wingedsheep.mtg.sets.definitions.eld.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.predicates.CardPredicate
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Wandermare — Throne of Eldraine #204
 * {1}{G}{W} · Creature — Horse · 3/3
 *
 * Whenever you cast a creature spell that has an Adventure, put a +1/+1 counter on this creature.
 *
 * Same `Creature ∧ HasAdventure` cast filter as Edgewall Innkeeper / Garenbrig Squire: an adventurer
 * cast as its Adventure is an instant/sorcery spell on the stack, so it doesn't trigger this.
 */
val Wandermare = card("Wandermare") {
    manaCost = "{1}{G}{W}"
    colorIdentity = "GW"
    typeLine = "Creature — Horse"
    power = 3
    toughness = 3
    oracleText = "Whenever you cast a creature spell that has an Adventure, put a +1/+1 counter on this creature. " +
        "(It doesn't need to have gone on the adventure first.)"

    triggeredAbility {
        trigger = Triggers.you.casts(GameObjectFilter.Creature.withCardPredicate(CardPredicate.HasAdventure))
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "204"
        artist = "Kari Christensen"
        flavorText = "\"A good steed will carry you home. A great steed will carry you to glory.\"\n—Syr Alin, the Lion's Claw"
        imageUri = "https://cards.scryfall.io/normal/front/9/a/9a72fb5b-5c55-4ce9-b9d0-98f924d6f338.jpg?1783932592"
        ruling(
            "2019-10-04",
            "Wandermare's ability resolves before the spell that caused it to trigger. It resolves even if " +
                "that spell is countered."
        )
        ruling(
            "2019-10-04",
            "An effect may refer to a card, spell, or permanent that \"has an Adventure.\" This refers to a " +
                "card, spell, or permanent that has an adventurer card's set of alternative characteristics, " +
                "even if they're not being used and even if that card was never cast as an Adventure."
        )
        ruling(
            "2019-10-04",
            "If an effect refers to a card, spell, or permanent that has an Adventure, it won't find an " +
                "instant or sorcery spell on the stack that's been cast as an Adventure."
        )
    }
}
