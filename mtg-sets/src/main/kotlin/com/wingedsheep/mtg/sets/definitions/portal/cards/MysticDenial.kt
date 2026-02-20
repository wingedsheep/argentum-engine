package com.wingedsheep.mtg.sets.definitions.portal.cards

import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CounterSpellEffect
import com.wingedsheep.sdk.scripting.conditions.OpponentSpellOnStack

/**
 * Mystic Denial
 * {1}{U}{U}
 * Instant
 * Cast this spell only after an opponent casts a creature or sorcery spell.
 * Counter target creature or sorcery spell.
 */
val MysticDenial = card("Mystic Denial") {
    manaCost = "{1}{U}{U}"
    typeLine = "Instant"

    spell {
        castOnlyIf(OpponentSpellOnStack)
        target = Targets.CreatureOrSorcerySpell
        effect = CounterSpellEffect
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "61"
        artist = "Hannibal King"
        imageUri = "https://cards.scryfall.io/normal/front/5/2/52d60f29-6da0-4ce6-9c92-96f313007271.jpg"
        ruling(
            "10/4/2004",
            "This card was originally printed as a sorcery and has received errata to make it an instant."
        )
    }
}
