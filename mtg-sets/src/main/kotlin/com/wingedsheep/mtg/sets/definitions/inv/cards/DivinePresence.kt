package com.wingedsheep.mtg.sets.definitions.inv.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CapDamage
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.events.RecipientFilter

/**
 * Divine Presence
 * {2}{W}
 * Enchantment
 * If a source would deal 4 or more damage to a permanent or player, that source deals 3
 * damage to that permanent or player instead.
 *
 * Invasion engine gap #7: capping (clamp to a maximum) is neither prevention (subtract) nor
 * modification (add), so it uses the new [CapDamage] replacement. `maxAmount = 3` reproduces
 * "4 or more → 3" exactly — amounts of 3 or less are already below the cap and unchanged.
 */
val DivinePresence = card("Divine Presence") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Enchantment"
    oracleText = "If a source would deal 4 or more damage to a permanent or player, that source deals 3 damage to that permanent or player instead."

    replacementEffect(
        CapDamage(
            maxAmount = 3,
            appliesTo = EventPattern.DamageEvent(recipient = RecipientFilter.Any)
        )
    )

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "15"
        artist = "Ron Spears"
        imageUri = "https://cards.scryfall.io/normal/front/2/8/28cb898d-d6ce-410a-83bf-37962cca2735.jpg?1562903314"
    }
}
