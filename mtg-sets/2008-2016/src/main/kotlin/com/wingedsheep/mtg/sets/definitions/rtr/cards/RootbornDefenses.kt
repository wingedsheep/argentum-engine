package com.wingedsheep.mtg.sets.definitions.rtr.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Rootborn Defenses
 * {2}{W}
 * Instant
 *
 * Populate. Creatures you control gain indestructible until end of turn. (To populate, create a
 * token that's a copy of a creature token you control.)
 *
 * Populate is [Effects.CreateTokenCopyOfChosenPermanent] over creature tokens (as on Coursers'
 * Accord). The indestructible grant is [Patterns.Group.grantKeywordToAll] over creatures you
 * control, sequenced after the populate: a group effect gathers its creatures when it resolves,
 * so the token populate just made gains indestructible too.
 *
 * Canonical printing: Return to Ravnica, the card's earliest real printing.
 */
val RootbornDefenses = card("Rootborn Defenses") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Instant"
    oracleText = "Populate. Creatures you control gain indestructible until end of turn. (To populate, " +
        "create a token that's a copy of a creature token you control.)"

    spell {
        effect = Effects.CreateTokenCopyOfChosenPermanent(GameObjectFilter.Creature.token()) then
            Patterns.Group.grantKeywordToAll(
                Keyword.INDESTRUCTIBLE,
                Filters.Group.creaturesYouControl,
            )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "19"
        artist = "Mark Zug"
        imageUri = "https://cards.scryfall.io/normal/front/d/e/deccfa48-b8df-4dcc-ba1b-920f8352def7.jpg?1783940374"
    }
}
