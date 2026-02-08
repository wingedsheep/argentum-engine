package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.DynamicAmount
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.LookAtTopAndReorderEffect
import com.wingedsheep.sdk.scripting.Player

/**
 * Information Dealer
 * {1}{U}
 * Creature — Human Wizard
 * 1/1
 * {T}: Look at the top X cards of your library, where X is the number of
 * Wizards you control, then put them back in any order.
 */
val InformationDealer = card("Information Dealer") {
    manaCost = "{1}{U}"
    typeLine = "Creature — Human Wizard"
    power = 1
    toughness = 1

    activatedAbility {
        cost = AbilityCost.Tap
        effect = LookAtTopAndReorderEffect(
            count = DynamicAmount.CountBattlefield(
                Player.You,
                GameObjectFilter.Creature.withSubtype("Wizard")
            )
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "88"
        artist = "Jerry Tiritilli"
        flavorText = "\"One wizard is a suspect. Two wizards are a conspiracy.\" —Elvish refugee"
        imageUri = "https://cards.scryfall.io/normal/front/a/4/a45ac59c-654d-44de-b266-532d44b34137.jpg?1562933756"
    }
}
