package com.wingedsheep.mtg.sets.definitions.ori.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Unholy Hunger
 * {3}{B}{B}
 * Instant
 *
 * Destroy target creature.
 * Spell mastery — If there are two or more instant and/or sorcery cards in your graveyard,
 * you gain 2 life.
 *
 * Spell mastery is a resolution-time [Conditions.CardsInGraveyardMatchingAtLeast] over instant
 * and/or sorcery cards; Unholy Hunger itself is still on the stack then, so it never counts. If the
 * target is illegal on resolution the spell doesn't resolve, so no life is gained.
 */
val UnholyHunger = card("Unholy Hunger") {
    manaCost = "{3}{B}{B}"
    colorIdentity = "B"
    typeLine = "Instant"
    oracleText = "Destroy target creature.\n" +
        "Spell mastery — If there are two or more instant and/or sorcery cards in your graveyard, " +
        "you gain 2 life."

    spell {
        val creature = target(TargetFilter.Creature)
        effect = Effects.Destroy(creature) then
            Effects.If(
                condition = Conditions.CardsInGraveyardMatchingAtLeast(2, GameObjectFilter.InstantOrSorcery),
                then = Effects.GainLife(2)
            )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "125"
        artist = "Lius Lasahido"
        flavorText = "Liliana viewed the attention of the angels as a testament to her power. Even so, they were an annoyance."
        imageUri = "https://cards.scryfall.io/normal/front/5/9/5994b7b0-3bca-480b-b265-ed269f15c17e.jpg?1783938335"
        ruling("2015-06-22", "Check to see if there are two or more instant and/or sorcery cards in your graveyard as the spell resolves to determine whether the spell mastery ability applies. The spell itself won't count because it's still on the stack as you make this check.")
    }
}
