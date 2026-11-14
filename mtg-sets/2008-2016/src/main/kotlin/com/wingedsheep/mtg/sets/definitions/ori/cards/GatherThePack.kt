package com.wingedsheep.mtg.sets.definitions.ori.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardOrder
import com.wingedsheep.sdk.scripting.effects.SelectionMode

/**
 * Gather the Pack
 * {1}{G}
 * Sorcery
 *
 * Reveal the top five cards of your library. You may put a creature card from among them into
 * your hand. Put the rest into your graveyard.
 * Spell mastery — If there are two or more instant and/or sorcery cards in your graveyard, put up
 * to two creature cards from among the revealed cards into your hand instead of one.
 *
 * [Patterns.Library.lookAtTopAndTakeMatching] with the cards revealed and the remainder sent to the
 * graveyard. Spell mastery only raises the cap of the one "up to N" choice, so it folds into the
 * selection as a [DynamicAmounts.conditional] (as Follow the Lumarets does), checked at resolution —
 * Gather the Pack is still on the stack then, so it never counts itself.
 */
val GatherThePack = card("Gather the Pack") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "Reveal the top five cards of your library. You may put a creature card from among them " +
        "into your hand. Put the rest into your graveyard.\n" +
        "Spell mastery — If there are two or more instant and/or sorcery cards in your graveyard, put up " +
        "to two creature cards from among the revealed cards into your hand instead of one."

    spell {
        effect = Patterns.Library.lookAtTopAndTakeMatching(
            count = DynamicAmounts.fixed(5),
            filter = GameObjectFilter.Creature,
            prompt = "You may put creature cards into your hand",
            selection = SelectionMode.ChooseUpTo(
                DynamicAmounts.conditional(
                    condition = Conditions.CardsInGraveyardMatchingAtLeast(2, GameObjectFilter.InstantOrSorcery),
                    ifTrue = 2,
                    ifFalse = 1
                )
            ),
            revealed = true,
            restDestination = CardDestination.ToZone(Zone.GRAVEYARD),
            restOrder = CardOrder.Preserve,
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "178"
        artist = "Igor Kieryluk"
        imageUri = "https://cards.scryfall.io/normal/front/9/a/9a4f5b00-ac50-485f-a38a-b9f409307d5c.jpg?1783938322"
        ruling("2015-06-22", "Check to see if there are two or more instant and/or sorcery cards in your graveyard as the spell resolves to determine whether the spell mastery ability applies. The spell itself won't count because it's still on the stack as you make this check.")
    }
}
