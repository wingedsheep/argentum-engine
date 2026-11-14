package com.wingedsheep.mtg.sets.definitions.ori.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyDamageAmount
import com.wingedsheep.sdk.scripting.events.Recipient

/**
 * Embermaw Hellion
 * {3}{R}{R}
 * Creature — Hellion
 * 4/5
 *
 * Trample
 * If another red source you control would deal damage to a permanent or player, it deals that
 * much damage plus 1 to that permanent or player instead.
 *
 * Jaya, Venerated Firemage's line: a plain additive [ModifyDamageAmount], so the extra point is
 * dealt by the original source (ruling), scoped to "another red source you control" through
 * `notSourceItself()` (the damage-source filter is evaluated with the Hellion as the host) and to
 * any recipient — you and your own permanents included. Any red source counts, spells and abilities
 * as well as creatures. The Hellion's own damage isn't boosted by itself, but a second Hellion
 * boosts it (cumulative ruling).
 */
val EmbermawHellion = card("Embermaw Hellion") {
    manaCost = "{3}{R}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Hellion"
    power = 4
    toughness = 5
    oracleText = "Trample (This creature can deal excess combat damage to the player or planeswalker it's attacking.)\n" +
        "If another red source you control would deal damage to a permanent or player, it deals that " +
        "much damage plus 1 to that permanent or player instead."

    keywords(Keyword.TRAMPLE)

    replacementEffect(
        ModifyDamageAmount(
            modifier = 1,
            appliesTo = EventPattern.DamageEvent(
                source = GameObjectFilter.Any.withColor(Color.RED).youControl().notSourceItself(),
                recipient = Recipient.Any,
            ),
        )
    )

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "141"
        artist = "James Paick"
        imageUri = "https://cards.scryfall.io/normal/front/6/6/664f60a8-e80a-4fe0-b76c-2d76c728329e.jpg?1783938332"
        ruling("2015-06-22", "Embermaw Hellion's last ability doesn't cause Embermaw Hellion to deal damage; it affects the amount of damage dealt by the original red source.")
        ruling("2015-06-22", "If the player or permanent being dealt damage is also affected by a damage prevention effect, that player or the controller of that permanent can apply that effect and Embermaw Hellion's effect in any order. If all of the damage is prevented, Embermaw Hellion's effect can't apply to it.")
        ruling("2015-06-22", "Multiple Embermaw Hellions are cumulative. If you control two of them, you'll add 2 to the damage dealt by another red source you control. In this case, you would also add 1 to the damage dealt by either Embermaw Hellion.")
    }
}
