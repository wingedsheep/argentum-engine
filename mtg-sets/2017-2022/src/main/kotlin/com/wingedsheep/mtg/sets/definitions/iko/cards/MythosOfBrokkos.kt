package com.wingedsheep.mtg.sets.definitions.iko.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.SearchDestination
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Mythos of Brokkos — Ikoria: Lair of Behemoths #168
 * {2}{G}{G} · Sorcery
 *
 * If {U}{B} was spent to cast this spell, search your library for a card, put that card into your
 * graveyard, then shuffle.
 * Return up to two permanent cards from your graveyard to your hand.
 *
 * The {U}{B} check reads the mana actually paid (a copy had none spent, so it skips the search).
 * The return step doesn't target and gathers the graveyard only after the search, so the card
 * just searched up may be one of the two returned (per the rulings).
 */
val MythosOfBrokkos = card("Mythos of Brokkos") {
    manaCost = "{2}{G}{G}"
    colorIdentity = "BGU"
    typeLine = "Sorcery"
    oracleText = "If {U}{B} was spent to cast this spell, search your library for a card, put that card " +
        "into your graveyard, then shuffle.\nReturn up to two permanent cards from your graveyard to your hand."

    spell {
        effect = Effects.Pipeline {
            run(
                Effects.If(
                    condition = Conditions.ManaSpentToCastIncludes(requiredBlue = 1, requiredBlack = 1),
                    then = Patterns.Library.searchLibrary(
                        filter = GameObjectFilter.Any,
                        count = 1,
                        destination = SearchDestination.GRAVEYARD
                    ),
                )
            )
            val permanentCards = gather(
                CardSource.FromZone(Zone.GRAVEYARD, Player.You, GameObjectFilter.Permanent)
            )
            val chosen = chooseUpTo(
                2,
                from = permanentCards,
                showAllCards = true,
                prompt = "Return up to two permanent cards from your graveyard to your hand",
                selectedLabel = "Return to hand",
                remainderLabel = "Leave in graveyard"
            )
            toHand(chosen)
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "168"
        artist = "Seb McKinnon"
        flavorText = "\"The most unexpected weapon that a monster can wield is kindness.\"\n—Tales of the Ozolith"
        imageUri = "https://cards.scryfall.io/normal/front/f/a/fa4fc5b1-6666-4c60-898f-f927212c7923.jpg?1783931031"
        ruling(
            "2020-04-17",
            "Because Mythos of Brokkos doesn't target the permanent cards in your graveyard, you may choose " +
                "the card you searched for."
        )
        ruling(
            "2020-04-17",
            "A permanent card is an artifact, creature, enchantment, land, or planeswalker card."
        )
        ruling(
            "2020-04-17",
            "If an effect copies the Mythos spell, no mana was spent to cast the copy, so the copy won't " +
                "receive the bonus."
        )
        ruling(
            "2020-04-17",
            "The ability checks what mana was actually spent to cast a spell. If an effect allows you to " +
                "spend mana \"as though it were mana\" of any color or type, that allows you to spend mana " +
                "you couldn't otherwise spend, but it doesn't change what mana you spent to cast the spell."
        )
    }
}
