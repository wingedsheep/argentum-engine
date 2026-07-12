package com.wingedsheep.mtg.sets.definitions.vow.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Vampires' Vengeance
 * {2}{R}
 * Instant
 *
 * Vampires' Vengeance deals 2 damage to each non-Vampire creature. Create a Blood token.
 */
val VampiresVengeance = card("Vampires' Vengeance") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Instant"
    oracleText = "Vampires' Vengeance deals 2 damage to each non-Vampire creature. Create a Blood " +
        "token. (It's an artifact with \"{1}, {T}, Discard a card, Sacrifice this token: Draw a card.\")"

    spell {
        effect = Effects.Composite(
            Patterns.Group.dealDamageToAll(
                2,
                GroupFilter(GameObjectFilter.Creature.notSubtype(Subtype.VAMPIRE))
            ),
            Effects.CreateBlood(1)
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "339"
        artist = "Dave Kendall"
        flavorText = "She was ghastly, chalkily pale; the red seemed to have gone even from her lips " +
            "and gums, and the bones of her face stood out prominently; her breathing was painful " +
            "to see or hear."
        imageUri = "https://cards.scryfall.io/normal/front/9/f/9f4ba693-0323-415d-ad91-c083fbbab7f7.jpg?1782702951"
    }
}
