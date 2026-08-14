package com.wingedsheep.mtg.sets.definitions.mkm.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Chalk Outline — Murders at Karlov Manor #157
 * {3}{G} · Enchantment · Uncommon
 *
 * Whenever one or more creature cards leave your graveyard, create a 2/2 white and blue Detective
 * creature token, then investigate.
 *
 * "One or more … leave" is CR 603.2c batch wording, so [Triggers.CardsLeaveYourGraveyard] is the
 * right shape rather than a per-card trigger: a mass reanimation, a flashback cast, and a
 * graveyard-exiling sweep each fire this exactly once no matter how many creature cards moved or
 * where they went (the printed ruling says so in as many words). The filter is
 * [GameObjectFilter.Creature] — matched against the *card*, so a creature card cast from the
 * graveyard counts on its way to the stack.
 *
 * The payoff is a plain [Effects.Composite]: the Detective token first, then the Clue. Order is
 * printed order and observable — a "whenever you create a token" watcher sees the Detective before
 * the Clue. Both take their art from the MKM `tokenArt` layer, so no `imageUri` is baked in here.
 */
val ChalkOutline = card("Chalk Outline") {
    manaCost = "{3}{G}"
    colorIdentity = "G"
    typeLine = "Enchantment"
    oracleText = "Whenever one or more creature cards leave your graveyard, create a 2/2 white and " +
        "blue Detective creature token, then investigate. (Create a Clue token. It's an artifact " +
        "with \"{2}, Sacrifice this token: Draw a card.\")"

    triggeredAbility {
        trigger = Triggers.CardsLeaveYourGraveyard(GameObjectFilter.Creature)
        effect = Effects.Composite(
            Effects.CreateToken(
                power = 2,
                toughness = 2,
                colors = setOf(Color.WHITE, Color.BLUE),
                creatureTypes = setOf("Detective")
            ),
            Effects.Investigate()
        )
        description = "Whenever one or more creature cards leave your graveyard, create a 2/2 " +
            "white and blue Detective creature token, then investigate."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "157"
        artist = "Julia Griffin"
        flavorText = "\"In your search for a suspect, never forget that the most important person " +
            "is the victim.\"\n—Ezrim, Agency chief"
        imageUri = "https://cards.scryfall.io/normal/front/b/3/b3ff56c1-4153-4e15-9ac6-06d93fa2ae50.jpg?1783912868"

        ruling(
            "2024-02-02",
            "If multiple creature cards leave your graveyard at the same time, Chalk Outline's " +
                "ability will trigger only once."
        )
    }
}
