package com.wingedsheep.mtg.sets.definitions.ori.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Calculated Dismissal
 * {2}{U}
 * Instant
 *
 * Counter target spell unless its controller pays {3}.
 * Spell mastery — If there are two or more instant and/or sorcery cards in your graveyard, scry 2.
 *
 * Condescend's soft counter followed by Dark Petition's spell mastery gate. The condition is checked
 * after the counter step, as the spell resolves: you scry 2 whether or not {3} was paid, and a spell
 * of yours this countered already counts toward the two (both per the 2015-06-22 rulings).
 */
val CalculatedDismissal = card("Calculated Dismissal") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Counter target spell unless its controller pays {3}.\n" +
        "Spell mastery — If there are two or more instant and/or sorcery cards in your graveyard, " +
        "scry 2. (To scry 2, look at the top two cards of your library, then put any number of them " +
        "on the bottom and the rest on top in any order.)"

    spell {
        target(TargetFilter.SpellOnStack)
        effect = Effects.CounterUnlessPays("{3}") then
            Effects.If(
                condition = Conditions.CardsInGraveyardMatchingAtLeast(2, GameObjectFilter.InstantOrSorcery),
                then = Effects.Scry(2)
            )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "48"
        artist = "Karl Kopinski"
        imageUri = "https://cards.scryfall.io/normal/front/2/c/2c42ab35-6050-42b2-9c3c-3252f2e69442.jpg?1783938354"
        ruling("2015-06-22", "If the spell mastery ability applies, you'll scry 2 even if the controller of the spell pays {3}.")
        ruling("2015-06-22", "In one unusual situation, you can cast Calculated Dismissal targeting an instant or sorcery spell you control while there is one instant or sorcery card in your graveyard. In this situation, if you decline to pay {3}, the spell will be countered and put into your graveyard. The spell mastery ability will then apply and you'll scry 2.")
        ruling("2015-06-22", "Check to see if there are two or more instant and/or sorcery cards in your graveyard as the spell resolves to determine whether the spell mastery ability applies. The spell itself won't count because it's still on the stack as you make this check.")
    }
}
