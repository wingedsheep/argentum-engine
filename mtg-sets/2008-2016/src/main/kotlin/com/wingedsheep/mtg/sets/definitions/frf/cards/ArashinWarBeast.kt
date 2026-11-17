package com.wingedsheep.mtg.sets.definitions.frf.cards

import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.events.DamageType
import com.wingedsheep.sdk.scripting.events.Recipient

/**
 * Arashin War Beast
 * {5}{G}{G}
 * Creature — Beast
 * 6/6
 * Whenever this creature deals combat damage to one or more blocking creatures, manifest the top
 * card of your library.
 *
 * "One or more" is a batch trigger: it fires once per combat damage step however many blockers
 * were dealt damage. A blocker killed by that damage still counts — the recipient is read as it
 * last existed, while it was blocking (CR 603.10).
 */
val ArashinWarBeast = card("Arashin War Beast") {
    manaCost = "{5}{G}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Beast"
    power = 6
    toughness = 6
    oracleText = "Whenever this creature deals combat damage to one or more blocking creatures, manifest the top " +
        "card of your library. (Put it onto the battlefield face down as a 2/2 creature. Turn it face up any time " +
        "for its mana cost if it's a creature card.)"

    triggeredAbility {
        trigger = Triggers.self.dealsDamage(
            to = Recipient.Object(GameObjectFilter.Creature.blocking()),
            damageType = DamageType.Combat,
            batch = true,
        )
        effect = Patterns.Library.manifest()
        description = "Whenever this creature deals combat damage to one or more blocking creatures, " +
            "manifest the top card of your library."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "123"
        artist = "Eric Deschamps"
        imageUri = "https://cards.scryfall.io/normal/front/6/6/66aed11a-0831-4619-931f-7dfded999c66.jpg?1783938682"
        ruling(
            "2014-11-24",
            "Arashin War Beast's ability will trigger only once per combat damage step, no matter how many " +
                "creatures are blocking it."
        )
    }
}
