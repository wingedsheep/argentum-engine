package com.wingedsheep.mtg.sets.definitions.mkm.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect

/**
 * Soul Search — Murders at Karlov Manor #232
 * {W}{B} · Sorcery
 *
 * Target opponent reveals their hand. You choose a nonland card from it. Exile that card. If the
 * card's mana value is 1 or less, create a 1/1 white and black Spirit creature token with flying.
 *
 * A Thoughtseize whose "consolation prize" fires exactly when the pick was cheap — so the card is
 * never dead: hitting a one-drop nets a body, hitting a bomb nets the bomb.
 *
 * Reuses [Patterns.Hand.revealHandAndExileChosen] (the Cruelclaw's Heist shape). The rider reads
 * `storeExiledAs`, not the pre-move selection: the collection of cards that *actually reached
 * exile* is empty when the opponent's hand held no nonland card at all, which is exactly when the
 * card says nothing happens. Reading the pre-move key would mint a Spirit off a selection the move
 * never completed.
 *
 * Both printed rulings are about how mana value is computed on the exiled card rather than about
 * this spell's structure — {X} counts as 0 outside the stack (CR 202.3b) and a card with no mana
 * cost has mana value 0 — so both land in the "1 or less" branch. The engine's own mana-value read
 * already follows CR 202.3, so the filter is a plain [Filters.ManaValueAtMost]; the rulings are
 * recorded as metadata rather than special-cased.
 */
val SoulSearch = card("Soul Search") {
    manaCost = "{W}{B}"
    colorIdentity = "WB"
    typeLine = "Sorcery"
    oracleText = "Target opponent reveals their hand. You choose a nonland card from it. Exile " +
        "that card. If the card's mana value is 1 or less, create a 1/1 white and black Spirit " +
        "creature token with flying."

    spell {
        target("opponent", Targets.Opponent)
        effect = Effects.Composite(
            Patterns.Hand.revealHandAndExileChosen(storeExiledAs = "exiledCard"),
            ConditionalEffect(
                condition = Conditions.CollectionContainsMatch("exiledCard", Filters.ManaValueAtMost(1)),
                effect = Effects.CreateToken(
                    power = 1,
                    toughness = 1,
                    colors = setOf(Color.WHITE, Color.BLACK),
                    creatureTypes = setOf("Spirit"),
                    keywords = setOf(Keyword.FLYING),
                    name = "Spirit",
                    imageUri = "https://cards.scryfall.io/normal/front/f/4/f4588570-bde4-4c2f-8469-81a3e15fb57b.jpg?1783912607"
                )
            )
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "232"
        artist = "A. M. Sartor"
        flavorText = "Taking a secret to your grave still doesn't make it safe from the Orzhov."
        imageUri = "https://cards.scryfall.io/normal/front/0/f/0f852937-381d-4445-99d3-2ecb8af6bb6a.jpg?1783912838"

        ruling("2024-02-02", "If the exiled card has {X} in its mana cost, X is 0.")
        ruling("2024-02-02", "If the exiled card doesn't have a mana cost, its mana value is 0.")
    }
}
