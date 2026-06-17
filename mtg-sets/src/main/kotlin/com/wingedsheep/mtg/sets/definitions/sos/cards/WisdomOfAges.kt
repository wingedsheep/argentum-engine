package com.wingedsheep.mtg.sets.definitions.sos.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource

/**
 * Wisdom of Ages
 * {4}{U}{U}{U}
 * Sorcery
 * Return all instant and sorcery cards from your graveyard to your hand. You have no maximum
 * hand size for the rest of the game.
 * Exile Wisdom of Ages.
 *
 * Resolution order (CR 608.2):
 *  1. Gather every instant/sorcery card in your graveyard and move them all to your hand
 *     (no selection — "all"). Wisdom of Ages itself is still on the stack at this point, so it
 *     does not return itself.
 *  2. Confer the permanent, player-scoped "no maximum hand size for the rest of the game"
 *     property via [Effects.RemoveMaximumHandSize]. This survives step 3 exiling the card,
 *     which is why it lives on the player rather than on a permanent's static ability.
 *  3. Exile Wisdom of Ages — `selfExile()` routes the spell to exile instead of its owner's
 *     graveyard as it finishes resolving (CR 608.2 / `CardScript.selfExileOnResolve`).
 */
val WisdomOfAges = card("Wisdom of Ages") {
    manaCost = "{4}{U}{U}{U}"
    colorIdentity = "U"
    typeLine = "Sorcery"
    oracleText = "Return all instant and sorcery cards from your graveyard to your hand. You have " +
        "no maximum hand size for the rest of the game.\n" +
        "Exile Wisdom of Ages."

    spell {
        selfExile()
        effect = Effects.Composite(
            Effects.Pipeline {
                val cards = gather(
                    CardSource.FromZone(Zone.GRAVEYARD, filter = GameObjectFilter.InstantOrSorcery)
                )
                toHand(cards)
            },
            Effects.RemoveMaximumHandSize(),
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "71"
        artist = "Jabari Weathers"
        flavorText = "\"If you take the time to listen, the archaics will tell you that this world " +
            "has been torn asunder and remade many times before.\"\n—Jadzi, Oracle of Arcavios"
        imageUri = "https://cards.scryfall.io/normal/front/b/2/b227ef04-33e4-44e8-a357-0ea3dfe5d49b.jpg?1775937405"
    }
}
