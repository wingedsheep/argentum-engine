package com.wingedsheep.mtg.sets.definitions.msh.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect

/**
 * HYDRA Troopers
 * {2}{B}
 * Creature — Human Soldier Villain
 * 3/2
 *
 * When this creature enters, create a tapped 2/1 black Villain creature token with menace
 * if there are two or more creature cards in your graveyard. Otherwise, mill two cards.
 *
 * Implementation notes:
 * - The "if … otherwise …" clause is *not* an intervening-if — it's a branch evaluated as the
 *   trigger resolves, so it is a [ConditionalEffect] over
 *   [Conditions.CreatureCardsInGraveyardAtLeast], not `triggerCondition`. The trigger always
 *   goes on the stack and always does one of the two things.
 */
val HydraTroopers = card("HYDRA Troopers") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Human Soldier Villain"
    oracleText = "When this creature enters, create a tapped 2/1 black Villain creature token " +
        "with menace if there are two or more creature cards in your graveyard. Otherwise, " +
        "mill two cards. (Put the top two cards of your library into your graveyard.)"
    power = 3
    toughness = 2

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = ConditionalEffect(
            condition = Conditions.CreatureCardsInGraveyardAtLeast(2),
            effect = Effects.CreateToken(
                power = 2,
                toughness = 1,
                colors = setOf(Color.BLACK),
                creatureTypes = setOf(Subtype.VILLAIN.value),
                keywords = setOf(Keyword.MENACE),
                tapped = true,
                imageUri = "https://cards.scryfall.io/normal/front/4/a/4a51b6a0-9a54-4f01-b959-0a28c15d103f.jpg?1783902804"
            ),
            elseEffect = Patterns.Library.mill(2)
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "101"
        artist = "Zoltan Boros"
        flavorText = "Evil never lacks foot soldiers to do its will."
        imageUri = "https://cards.scryfall.io/normal/front/4/0/40c202f1-6e0d-42f4-a41e-e0be3362d585.jpg?1783902942"
    }
}
