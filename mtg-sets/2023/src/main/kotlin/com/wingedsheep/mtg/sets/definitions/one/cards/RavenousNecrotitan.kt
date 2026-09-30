package com.wingedsheep.mtg.sets.definitions.one.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Ravenous Necrotitan
 * {2}{B}{B}
 * Creature — Phyrexian Horror
 * 6/6
 * Corrupted — When this creature enters, sacrifice a creature unless an opponent has three or more
 * poison counters.
 *
 * "Unless" is not an intervening-if: the trigger always goes on the stack, and the poison check
 * happens on resolution — so it is `Effects.If(Not(Corrupted), SacrificeOwn)`. The Necrotitan
 * itself is a legal creature to sacrifice.
 */
val RavenousNecrotitan = card("Ravenous Necrotitan") {
    manaCost = "{2}{B}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Phyrexian Horror"
    oracleText = "Corrupted — When this creature enters, sacrifice a creature unless an opponent has " +
        "three or more poison counters."
    power = 6
    toughness = 6

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.If(
            condition = Conditions.Not(Conditions.Corrupted),
            then = Effects.SacrificeOwn(GameObjectFilter.Creature),
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "106"
        artist = "Johann Bodin"
        flavorText = "Some of Sheoldred's goliaths spread the glory of Phyrexia. Others just eat."
        imageUri = "https://cards.scryfall.io/normal/front/c/e/ce7e1e9b-ad7f-4f4c-bdb5-2ccb6f147037.jpg?1783918042"
    }
}
