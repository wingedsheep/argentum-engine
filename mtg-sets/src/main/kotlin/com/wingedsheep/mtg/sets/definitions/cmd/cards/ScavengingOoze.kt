package com.wingedsheep.mtg.sets.definitions.cmd.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Scavenging Ooze — Commander 2011 #170 (canonical printing)
 * {1}{G} · Creature — Ooze · 2/2
 *
 * {G}: Exile target card from a graveyard. If it was a creature card, put a +1/+1 counter on
 * this creature and you gain 1 life.
 *
 * The activated ability exiles a single (non-optional) targeted graveyard card, then gates the
 * growth on [Conditions.TargetIsCreatureCard] — which reads the card's printed type in exile, so
 * it is correctly true only when the exiled card was a creature card. Same "exile then check the
 * exiled card's type" shape as Raven Eagle.
 */
val ScavengingOoze = card("Scavenging Ooze") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Ooze"
    power = 2
    toughness = 2
    oracleText = "{G}: Exile target card from a graveyard. If it was a creature card, " +
        "put a +1/+1 counter on this creature and you gain 1 life."

    activatedAbility {
        cost = Costs.Mana(ManaCost.parse("{G}"))
        val exiled = target("target card in a graveyard", Targets.CardInGraveyard)
        effect = Effects.Composite(
            Effects.Exile(exiled),
            ConditionalEffect(
                condition = Conditions.TargetIsCreatureCard(0),
                effect = Effects.Composite(
                    Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self),
                    Effects.GainLife(1),
                ),
            ),
        )
        description = "{G}: Exile target card from a graveyard. If it was a creature card, " +
            "put a +1/+1 counter on this creature and you gain 1 life."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "170"
        artist = "Austin Hsu"
        flavorText = "In nature, not a single bone or scrap of flesh goes to waste."
        imageUri = "https://cards.scryfall.io/normal/front/3/7/371ceb58-f498-4616-a7f0-eb118fe2e4ff.jpg?1782715022"
    }
}
