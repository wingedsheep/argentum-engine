package com.wingedsheep.mtg.sets.definitions.ori.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Fiery Impulse
 * {R}
 * Instant
 *
 * Fiery Impulse deals 2 damage to target creature.
 * Spell mastery — If there are two or more instant and/or sorcery cards in your graveyard,
 * Fiery Impulse deals 3 damage instead.
 *
 * Spell mastery is a resolution-time [Conditions.CardsInGraveyardMatchingAtLeast] over instant
 * and/or sorcery cards; Fiery Impulse itself is still on the stack then, so it never counts.
 */
val FieryImpulse = card("Fiery Impulse") {
    manaCost = "{R}"
    colorIdentity = "R"
    typeLine = "Instant"
    oracleText = "Fiery Impulse deals 2 damage to target creature.\n" +
        "Spell mastery — If there are two or more instant and/or sorcery cards in your graveyard, " +
        "Fiery Impulse deals 3 damage instead."

    spell {
        val t = target(TargetFilter.Creature)
        effect = Effects.If(
            condition = Conditions.CardsInGraveyardMatchingAtLeast(2, GameObjectFilter.InstantOrSorcery),
            then = Effects.DealDamage(3, t),
            otherwise = Effects.DealDamage(2, t),
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "145"
        artist = "Daarken"
        flavorText = "\"Well, this got out of hand.\""
        imageUri = "https://cards.scryfall.io/normal/front/5/b/5b81a589-44bf-4441-9079-1ae66d067519.jpg?1783938330"
        ruling("2015-06-22", "Check to see if there are two or more instant and/or sorcery cards in your graveyard as the spell resolves to determine whether the spell mastery ability applies. The spell itself won't count because it's still on the stack as you make this check.")
    }
}
