package com.wingedsheep.mtg.sets.definitions.gtc.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.evolve
import com.wingedsheep.sdk.model.Rarity

/**
 * Fathom Mage — Gatecrash #162
 * {2}{G}{U} · Creature — Human Wizard · 1/1
 *
 * Evolve
 * Whenever a +1/+1 counter is put on this creature, you may draw a card.
 *
 * "Whenever **a** +1/+1 counter is put on" is the per-counter template (Bloodcrazed Hoplite): per the
 * rulings, simultaneous counters (or entering with counters) trigger it once per counter.
 */
val FathomMage = card("Fathom Mage") {
    manaCost = "{2}{G}{U}"
    colorIdentity = "GU"
    typeLine = "Creature — Human Wizard"
    power = 1
    toughness = 1
    oracleText = "Evolve (Whenever a creature you control enters, if that creature has greater power or " +
        "toughness than this creature, put a +1/+1 counter on this creature.)\n" +
        "Whenever a +1/+1 counter is put on this creature, you may draw a card."

    evolve()

    triggeredAbility {
        trigger = Triggers.self.getsCounters(CounterType.PLUS_ONE_PLUS_ONE, perCounter = true)
        effect = Effects.May(Effects.DrawCards(1))
        description = "Whenever a +1/+1 counter is put on this creature, you may draw a card."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "162"
        artist = "Ryan Pancoast"
        imageUri = "https://cards.scryfall.io/normal/front/4/f/4fa311f1-f11e-492d-9f18-e7489f950be7.jpg?1783940109"
        ruling("2013-01-24", "Fathom Mage's last ability will trigger whenever any +1/+1 counter is placed on it, not just ones due to the evolve ability.")
        ruling("2013-01-24", "If Fathom Mage enters with a +1/+1 counter on it (perhaps due to Master Biomancer), its last ability will trigger once for each +1/+1 counter it entered with.")
        ruling("2013-01-24", "If multiple +1/+1 counters are placed on Fathom Mage simultaneously, its last ability will trigger once for each of those counters.")
        ruling(
            "2013-04-15",
            "If evolve triggers, the stat comparison will happen again when the ability tries to resolve. If " +
                "neither stat of the new creature is greater, the ability will do nothing. If the creature that " +
                "entered the battlefield leaves the battlefield before evolve tries to resolve, use its last known " +
                "power and toughness to compare the stats."
        )
    }
}
