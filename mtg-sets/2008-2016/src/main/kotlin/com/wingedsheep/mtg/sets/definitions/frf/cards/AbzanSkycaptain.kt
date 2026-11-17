package com.wingedsheep.mtg.sets.definitions.frf.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Abzan Skycaptain
 * {3}{W}
 * Creature — Bird Soldier
 * 2/2
 *
 * Flying
 * When this creature dies, bolster 2. (Choose a creature with the least toughness among
 * creatures you control and put two +1/+1 counters on it.)
 *
 * The creature is chosen as the trigger resolves, from the creatures still on the battlefield.
 * Skycaptain is already in the graveyard by then, so it can't receive the counters.
 */
val AbzanSkycaptain = card("Abzan Skycaptain") {
    manaCost = "{3}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Bird Soldier"
    power = 2
    toughness = 2
    oracleText = "Flying\n" +
        "When this creature dies, bolster 2. (Choose a creature with the least toughness among " +
        "creatures you control and put two +1/+1 counters on it.)"

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.dies()
        effect = Patterns.Mechanic.bolster(2)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "4"
        artist = "Matt Stewart"
        flavorText = "\"A tempest is coming. Turn your faces toward the wind!\""
        imageUri = "https://cards.scryfall.io/normal/front/c/4/c4195373-e902-4fc3-aead-1f6e728dbbae.jpg?1783938715"
        ruling("2014-11-24", "Bolster itself doesn't target any creature, though some spells and abilities that bolster may have other effects that target creatures. For example, you could put counters on a creature with protection from white with Abzan Skycaptain's bolster ability.")
        ruling("2014-11-24", "You determine which creature to put counters on as the spell or ability that instructs you to bolster resolves.")
    }
}
