package com.wingedsheep.mtg.sets.definitions.aer.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Disallow
 * {1}{U}{U}
 * Instant
 * Counter target spell, activated ability, or triggered ability. (Mana abilities can't be targeted.)
 *
 * Mana abilities never use the stack, so [TargetFilter.SpellOrAbilityOnStack] can't reach them.
 */
val Disallow = card("Disallow") {
    manaCost = "{1}{U}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Counter target spell, activated ability, or triggered ability. (Mana abilities can't be targeted.)"

    spell {
        target(TargetFilter.SpellOrAbilityOnStack)
        effect = Effects.CounterSpellOrAbility()
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "31"
        artist = "Min Yum"
        flavorText = "\"How easily your flames are quenched.\"\n—Baral, Chief of Compliance"
        imageUri = "https://cards.scryfall.io/normal/front/2/5/25f05814-a5a5-460f-9d29-0ab03efecf4c.jpg?1783936774"
        ruling(
            "2017-02-09",
            "If you counter a delayed triggered ability that triggers at the beginning of the \"next\" " +
                "occurrence of a specified step or phase, that ability won't trigger again the following time " +
                "that phase or step occurs."
        )
        ruling(
            "2017-02-09",
            "An activated mana ability is one that could add mana as it resolves, doesn't have a target, and " +
                "isn't a loyalty ability. A triggered mana ability is one that could add mana and triggers on an " +
                "activated mana ability."
        )
        ruling(
            "2017-02-09",
            "Abilities that create replacement effects, such as a permanent entering the battlefield tapped or " +
                "with counters on it, can't be targeted. Abilities that apply \"as [this creature] enters the " +
                "battlefield\" are also replacement effects and can't be targeted."
        )
    }
}
