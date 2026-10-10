package com.wingedsheep.mtg.sets.definitions.c19.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivationRestriction

/**
 * Idol of Oblivion
 * {2}
 * Artifact
 *
 * {T}: Draw a card. Activate only if you created a token this turn.
 * {8}, {T}, Sacrifice this artifact: Create a 10/10 colorless Eldrazi creature token.
 *
 * The draw's restriction is turn history (`Conditions.CreatedTokenThisTurn`): the token need not
 * still exist, so a Treasure sacrificed for mana still turns it on.
 */
val IdolOfOblivion = card("Idol of Oblivion") {
    manaCost = "{2}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "{T}: Draw a card. Activate only if you created a token this turn.\n" +
        "{8}, {T}, Sacrifice this artifact: Create a 10/10 colorless Eldrazi creature token."

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.DrawCards(1)
        restrictions = listOf(ActivationRestriction.OnlyIfCondition(Conditions.CreatedTokenThisTurn()))
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{8}"), Costs.Tap, Costs.SacrificeSelf)
        effect = Effects.CreateToken(
            power = 10,
            toughness = 10,
            creatureTypes = setOf("Eldrazi"),
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "55"
        artist = "Piotr Dura"
        flavorText = "\"Arise, great one, and cleanse the world of our enemies!\""
        imageUri = "https://cards.scryfall.io/normal/front/d/a/daed8456-011e-44e2-b180-6a8a75089257.jpg?1783932793"
    }
}
