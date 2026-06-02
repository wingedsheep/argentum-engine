package com.wingedsheep.mtg.sets.definitions.inv.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.PreventDamage
import com.wingedsheep.sdk.scripting.events.RecipientFilter
import com.wingedsheep.sdk.scripting.events.SourceFilter

/**
 * Well-Laid Plans
 * {2}{U}
 * Enchantment
 * Prevent all damage that would be dealt to a creature by another creature if they share a color.
 *
 * Invasion engine gap #7: "by another creature ... if they share a color" composes the
 * existing `Creature` filter with the new relational [CardPredicate.SharesColorWithRecipient]
 * predicate (via [GameObjectFilter.sharingColorWithRecipient]). The predicate compares the
 * damage source's colors against the recipient's and excludes self-damage, so the prevention
 * only triggers between two distinct, color-sharing creatures.
 */
val WellLaidPlans = card("Well-Laid Plans") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Enchantment"
    oracleText = "Prevent all damage that would be dealt to a creature by another creature if they share a color."

    replacementEffect(
        PreventDamage(
            appliesTo = EventPattern.DamageEvent(
                recipient = RecipientFilter.AnyCreature,
                source = SourceFilter.Matching(
                    GameObjectFilter.Creature.sharingColorWithRecipient()
                )
            )
        )
    )

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "88"
        artist = "Kev Walker"
        imageUri = "https://cards.scryfall.io/normal/front/1/c/1c55eb8f-925a-42c1-9e48-d7f99cab3b01.jpg?1562900616"
    }
}
