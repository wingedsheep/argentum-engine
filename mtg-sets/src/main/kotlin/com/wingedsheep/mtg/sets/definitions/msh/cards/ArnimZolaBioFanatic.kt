package com.wingedsheep.mtg.sets.definitions.msh.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivationRestriction

/**
 * Arnim Zola, Bio-Fanatic
 * {2}{B}
 * Legendary Artifact Creature — Scientist Villain
 * 2/3
 *
 * {3}, {T}: Create a tapped 2/1 black Villain creature token with menace.
 * Activate only if there are two or more creature cards in your graveyard.
 *
 * Implementation notes:
 * - "Activate only if …" is an activation restriction (checked when the ability is activated,
 *   CR 602.5), modeled with [ActivationRestriction.OnlyIfCondition] over
 *   [Conditions.CreatureCardsInGraveyardAtLeast] — *not* an intervening-if on resolution.
 * - The token is a plain (non-artifact) 2/1 black Villain with menace, created tapped.
 */
val ArnimZolaBioFanatic = card("Arnim Zola, Bio-Fanatic") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Legendary Artifact Creature — Scientist Villain"
    oracleText = "{3}, {T}: Create a tapped 2/1 black Villain creature token with menace. " +
        "Activate only if there are two or more creature cards in your graveyard. " +
        "(It can't be blocked except by two or more creatures.)"
    power = 2
    toughness = 3

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{3}"), Costs.Tap)
        effect = Effects.CreateToken(
            power = 2,
            toughness = 1,
            colors = setOf(Color.BLACK),
            creatureTypes = setOf(Subtype.VILLAIN.value),
            keywords = setOf(Keyword.MENACE),
            tapped = true,
            imageUri = "https://cards.scryfall.io/normal/front/4/a/4a51b6a0-9a54-4f01-b959-0a28c15d103f.jpg?1783902804"
        )
        restrictions = listOf(
            ActivationRestriction.OnlyIfCondition(Conditions.CreatureCardsInGraveyardAtLeast(2))
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "86"
        artist = "Immanuela Crovius"
        flavorText = "\"You are not the only one who found a way to live across the ages, Captain America.\""
        imageUri = "https://cards.scryfall.io/normal/front/0/7/07c70df6-b064-424a-852e-201b312a5b54.jpg?1783902948"
    }
}
