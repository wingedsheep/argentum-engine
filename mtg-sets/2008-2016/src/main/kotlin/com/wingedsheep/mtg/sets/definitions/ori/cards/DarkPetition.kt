package com.wingedsheep.mtg.sets.definitions.ori.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Dark Petition
 * {3}{B}{B}
 * Sorcery
 *
 * Search your library for a card, put that card into your hand, then shuffle.
 * Spell mastery — If there are two or more instant and/or sorcery cards in your graveyard,
 * add {B}{B}{B}.
 *
 * Spell mastery is a resolution-time [Conditions.CardsInGraveyardMatchingAtLeast] over instant
 * and/or sorcery cards; Dark Petition itself is still on the stack then, so it never counts.
 */
val DarkPetition = card("Dark Petition") {
    manaCost = "{3}{B}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Search your library for a card, put that card into your hand, then shuffle.\n" +
        "Spell mastery — If there are two or more instant and/or sorcery cards in your graveyard, " +
        "add {B}{B}{B}."

    spell {
        effect = Patterns.Library.searchLibrary() then
            Effects.If(
                condition = Conditions.CardsInGraveyardMatchingAtLeast(2, GameObjectFilter.InstantOrSorcery),
                then = Effects.AddMana(Color.BLACK, 3)
            )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "90"
        artist = "Igor Kieryluk"
        imageUri = "https://cards.scryfall.io/normal/front/e/9/e9df9c5e-7087-42b2-9001-c89d40a66c68.jpg?1783938344"
        ruling("2015-06-22", "Check to see if there are two or more instant and/or sorcery cards in your graveyard as the spell resolves to determine whether the spell mastery ability applies. The spell itself won't count because it's still on the stack as you make this check.")
    }
}
