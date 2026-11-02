package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator

/**
 * First Contact
 * {1}{G}
 * Instant
 * Mill four cards. You may put a permanent card from among them into your hand. If this spell is the
 * first spell you've cast this game, you gain 2 life.
 *
 * "First spell" is judged as of the cast: the count is frozen onto the spell as it becomes cast, so a
 * spell cast in response doesn't take the life away. A copy wasn't cast (CR 707.10), so `WasCast`
 * keeps it from claiming to be first.
 */
val FirstContact = card("First Contact") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Instant"
    oracleText = "Mill four cards. You may put a permanent card from among them into your hand. If this " +
        "spell is the first spell you've cast this game, you gain 2 life. (To mill four cards, put the top " +
        "four cards of your library into your graveyard.)"

    spell {
        effect = Effects.Pipeline {
            val milled = mill(4)
            val selected = chooseUpTo(
                1,
                from = milled,
                filter = GameObjectFilter.Permanent,
                showAllCards = true,
                prompt = "You may put a permanent card into your hand",
                selectedLabel = "Put in hand",
                remainderLabel = "Leave in graveyard"
            )
            toHand(selected)
            run(Effects.If(
                condition = Conditions.All(
                    Conditions.WasCast,
                    Conditions.CompareAmounts(
                        DynamicAmounts.spellsCastThisGame(beforeThisSpell = true),
                        ComparisonOperator.EQ,
                        0,
                    ),
                ),
                then = Effects.GainLife(2)
            ))
        }
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "199"
        artist = "Kieran Yanner"
        imageUri = "https://cards.scryfall.io/normal/front/5/4/5407e8f4-0c8e-4e66-9d2f-f22277aa38f4.jpg?1785981580"
    }
}
