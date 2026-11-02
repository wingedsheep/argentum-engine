package com.wingedsheep.mtg.sets.definitions.grn.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Glowspore Shaman
 * {B}{G}
 * Creature — Elf Shaman
 * 3/1
 * When this creature enters, mill three cards. You may put a land card from your graveyard on top
 * of your library.
 *
 * Mill first, so the freshly milled cards are legal picks, then a `chooseUpTo(1)` over the land
 * cards in your graveyard (not a target — a resolution-time choice; declining is choosing zero).
 */
val GlowsporeShaman = card("Glowspore Shaman") {
    manaCost = "{B}{G}"
    colorIdentity = "BG"
    typeLine = "Creature — Elf Shaman"
    power = 3
    toughness = 1
    oracleText = "When this creature enters, mill three cards. You may put a land card from your " +
        "graveyard on top of your library. (To mill a card, put the top card of your library into " +
        "your graveyard.)"

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.Pipeline {
            run(Patterns.Library.mill(3))
            val lands = gather(CardSource.FromZone(Zone.GRAVEYARD, Player.You, Filters.Land))
            val chosen = chooseUpTo(
                1,
                from = lands,
                showAllCards = true,
                prompt = "You may put a land card from your graveyard on top of your library",
                selectedLabel = "Put on top of library",
                remainderLabel = "Leave in graveyard"
            )
            toLibraryTop(chosen)
        }
        description = "When this creature enters, mill three cards. You may put a land card from " +
            "your graveyard on top of your library."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "173"
        artist = "Lars Grant-West"
        flavorText = "From death she grows living grandeur."
        imageUri = "https://cards.scryfall.io/normal/front/0/8/08fe260a-d204-4e75-b3e5-0cd9b4ca7084.jpg?1783934134"
        ruling("2018-10-05", "The land card you put on top of your library may be one that you just milled or one that was already there.")
    }
}
