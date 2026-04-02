package com.wingedsheep.mtg.sets.definitions.khans.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Trap Essence
 * {G}{U}{R}
 * Instant
 * Counter target creature spell. Put two +1/+1 counters on up to one target creature.
 *
 * Rulings:
 * - You don't have to target a creature to cast Trap Essence,
 *   although you must target a creature spell on the stack.
 */
val TrapEssence = card("Trap Essence") {
    manaCost = "{G}{U}{R}"
    typeLine = "Instant"
    oracleText = "Counter target creature spell. Put two +1/+1 counters on up to one target creature."

    spell {
        target("creature spell", Targets.CreatureSpell)
        val creature = target("creature", TargetCreature(optional = true))
        effect = Effects.CounterSpell()
            .then(Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 2, creature))
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "209"
        artist = "Raymond Swanland"
        flavorText = "\"Meat sustains the body. The spirit requires different sustenance.\" —Arel the Whisperer"
        imageUri = "https://cards.scryfall.io/normal/front/2/b/2bb37bd9-10a0-48d5-87f0-23a03b5c1072.jpg?1562784208"
        ruling("2014-09-20", "You don't have to target a creature to cast Trap Essence, although you must target a creature spell on the stack.")
    }
}
