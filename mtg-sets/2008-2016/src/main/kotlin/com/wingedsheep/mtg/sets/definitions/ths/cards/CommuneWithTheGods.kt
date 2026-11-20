package com.wingedsheep.mtg.sets.definitions.ths.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardOrder

/**
 * Commune with the Gods
 * {1}{G}
 * Sorcery
 *
 * Reveal the top five cards of your library. You may put a creature or enchantment card from
 * among them into your hand. Put the rest into your graveyard.
 *
 * Grisly Salvage's shape: revealed, up-to-one filtered take, rest to the graveyard in order.
 */
val CommuneWithTheGods = card("Commune with the Gods") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "Reveal the top five cards of your library. You may put a creature or enchantment card from among them into your hand. Put the rest into your graveyard."

    spell {
        effect = Patterns.Library.lookAtTopAndTakeMatching(
            count = DynamicAmounts.fixed(5),
            filter = GameObjectFilter.CreatureOrEnchantment,
            prompt = "You may put a creature or enchantment card from among them into your hand",
            revealed = true,
            restDestination = CardDestination.ToZone(Zone.GRAVEYARD),
            restOrder = CardOrder.Preserve,
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "155"
        artist = "Aleksi Briclot"
        flavorText = "For the first time in many years, Elspeth asked for help."
        imageUri = "https://cards.scryfall.io/normal/front/e/1/e101ae5f-c3ea-4788-b27f-d9452234eaef.jpg?1783939746"
    }
}
