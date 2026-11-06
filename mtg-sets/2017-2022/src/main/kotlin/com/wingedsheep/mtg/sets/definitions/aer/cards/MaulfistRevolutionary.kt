package com.wingedsheep.mtg.sets.definitions.aer.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Maulfist Revolutionary — Aether Revolt #115 (canonical printing)
 * {1}{G}{G} · Creature — Human Warrior · 3/3
 *
 * Trample
 * When this creature enters or dies, for each kind of counter on target permanent or player, give
 * that permanent or player another counter of that kind.
 *
 * Current Oracle (the printed "or remove one from it" has been errata'd away) is single-target
 * proliferate: `Effects.Proliferate(target)`, the same shape as Powerful Broker. Per the rulings it
 * gives exactly one counter of each kind (not a doubling), and it is not the proliferate keyword
 * action, so the targeted branch emits no ProliferatedEvent. "Enters or dies" is two triggers, one
 * per event; each picks its own target.
 */
val MaulfistRevolutionary = card("Maulfist Revolutionary") {
    manaCost = "{1}{G}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Human Warrior"
    power = 3
    toughness = 3
    oracleText = "Trample\nWhen this creature enters or dies, for each kind of counter on target " +
        "permanent or player, give that permanent or player another counter of that kind."

    keywords(Keyword.TRAMPLE)

    triggeredAbility {
        trigger = Triggers.self.enters()
        val recipient = target(Targets.PermanentOrPlayer)
        effect = Effects.Proliferate(recipient)
        description = "When this creature enters, for each kind of counter on target permanent or " +
            "player, give that permanent or player another counter of that kind."
    }

    triggeredAbility {
        trigger = Triggers.self.dies()
        val recipient = target(Targets.PermanentOrPlayer)
        effect = Effects.Proliferate(recipient)
        description = "When this creature dies, for each kind of counter on target permanent or " +
            "player, give that permanent or player another counter of that kind."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "115"
        artist = "Scott Murphy"
        imageUri = "https://cards.scryfall.io/normal/front/a/8/a83dcdba-419b-41e1-8c9d-8bca6fe2752b.jpg?1783936742"
    }
}
