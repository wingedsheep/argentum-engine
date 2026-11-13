package com.wingedsheep.mtg.sets.definitions.bfz.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Transgress the Mind
 * {1}{B}
 * Sorcery
 * Devoid (This card has no color.)
 * Target player reveals their hand. You choose a card from it with mana value 3 or greater and
 * exile that card.
 *
 * Thought-Knot Seer's `Patterns.Hand.revealHandAndExileChosen` cast as a sorcery: any target
 * player (not just an opponent), with the choice narrowed to cards of mana value 3 or greater.
 * The whole hand is revealed even when nothing in it qualifies; then nothing is exiled.
 */
val TransgressTheMind = card("Transgress the Mind") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Devoid (This card has no color.)\n" +
        "Target player reveals their hand. You choose a card from it with mana value 3 or greater and exile that card."

    keywords(Keyword.DEVOID)

    spell {
        val player = target(Targets.Player)
        effect = Patterns.Hand.revealHandAndExileChosen(
            target = player,
            filter = GameObjectFilter.Any.manaValueAtLeast(3),
            prompt = "Choose a card with mana value 3 or greater to exile",
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "101"
        artist = "Cynthia Sheppard"
        flavorText = "\"Fear of the unknown can be a wonderfully innocent thing, relatively speaking.\"\n" +
            "—Anowon, the Ruin Sage"
        imageUri = "https://cards.scryfall.io/normal/front/f/5/f5742910-3d0f-4710-a5f5-f7a7908833dc.jpg?1783938204"
    }
}
