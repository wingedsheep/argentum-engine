package com.wingedsheep.mtg.sets.definitions.msh.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Madame Hydra
 * {2}{B}{R}
 * Legendary Creature — Human Villain
 * 2/3
 *
 * Whenever you cast a Villain spell, create a 2/1 black Villain creature token with menace.
 *
 * Implementation notes:
 * - [Triggers.YouCastSubtype] matches the spell on the stack by subtype, so it fires for any
 *   Villain spell (creature or otherwise) you cast — including the Villain tokens' own tribe
 *   payoffs later in the set. It does not fire for Madame Hydra herself (she isn't on the
 *   battlefield while her own spell is on the stack).
 */
val MadameHydra = card("Madame Hydra") {
    manaCost = "{2}{B}{R}"
    colorIdentity = "BR"
    typeLine = "Legendary Creature — Human Villain"
    oracleText = "Whenever you cast a Villain spell, create a 2/1 black Villain creature token " +
        "with menace. (It can't be blocked except by two or more creatures.)"
    power = 2
    toughness = 3

    triggeredAbility {
        trigger = Triggers.YouCastSubtype(Subtype.VILLAIN)
        effect = Effects.CreateToken(
            power = 2,
            toughness = 1,
            colors = setOf(Color.BLACK),
            creatureTypes = setOf(Subtype.VILLAIN.value),
            keywords = setOf(Keyword.MENACE),
            imageUri = "https://cards.scryfall.io/normal/front/4/a/4a51b6a0-9a54-4f01-b959-0a28c15d103f.jpg?1783902804"
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "221"
        artist = "Pauline Voss"
        flavorText = "\"This will be our night of glory! Hail HYDRA!\""
        imageUri = "https://cards.scryfall.io/normal/front/e/9/e94ccedb-1d27-4098-8823-d8d99b30387c.jpg?1783902899"
    }
}
