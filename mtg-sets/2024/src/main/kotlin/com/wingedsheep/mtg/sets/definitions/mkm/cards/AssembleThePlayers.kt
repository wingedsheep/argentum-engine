package com.wingedsheep.mtg.sets.definitions.mkm.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CastSpellTypesFromTopOfLibrary
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.LookAtTopOfLibrary

/**
 * Assemble the Players — Murders at Karlov Manor #3
 * {1}{W} · Enchantment · Rare
 *
 * You may look at the top card of your library any time.
 * Once each turn, you may cast a creature spell with power 2 or less from the top of your library.
 *
 * The Precognition Field shape with a power ceiling and a per-turn allowance. Visibility and play
 * permission are separate statics on purpose: [LookAtTopOfLibrary] is the *private* peek (only the
 * controller sees the card — this is not Future Sight's public reveal), and
 * [CastSpellTypesFromTopOfLibrary] is the casting grant. `maxCastsPerTurn = 1` belongs to the
 * granting permanent rather than to the player, which is exactly the printed ruling: two
 * Assemble the Players are two independent permissions, and one that leaves and returns in the
 * same turn comes back with a fresh one.
 *
 * The filter is the *spell's* power, so the disguise/morph ruling falls out for free — a face-down
 * spell is a 2/2 and qualifies whatever the card's printed power is.
 */
val AssembleThePlayers = card("Assemble the Players") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Enchantment"
    oracleText = "You may look at the top card of your library any time.\n" +
        "Once each turn, you may cast a creature spell with power 2 or less from the top of your " +
        "library."

    staticAbility {
        ability = LookAtTopOfLibrary()
    }

    staticAbility {
        ability = CastSpellTypesFromTopOfLibrary(
            filter = GameObjectFilter.Creature.powerAtMost(2),
            maxCastsPerTurn = 1
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "3"
        artist = "Evyn Fong"
        flavorText = "\"All will soon be made clear, but I can give you this: our killer is here " +
            "with us, in this very room.\"\n—Alquist Proft"
        imageUri = "https://cards.scryfall.io/normal/front/f/5/f5bcb21a-8559-4791-8cd0-482e7b8dcfd2.jpg?1783912930"

        ruling(
            "2024-02-02",
            "If Assemble the Players leaves the battlefield and returns to the battlefield in the " +
                "same turn, the casting permission granted by the old one is different than the " +
                "casting permission granted by the new one. Similarly, if you control multiple " +
                "Assemble the Players, each one has a different casting permission that may be " +
                "used once each turn."
        )
        ruling(
            "2024-02-02",
            "Because you never \"cast\" a land card, Assemble the Players doesn't allow you to " +
                "play a land creature (such as Dryad Arbor) from the top of your library."
        )
    }
}
