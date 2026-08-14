package com.wingedsheep.mtg.sets.definitions.woe.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersTapped
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Horned Loch-Whale // Lagoon Breach
 * {4}{U}{U}
 * Creature — Whale
 * 6/6
 * Flash
 * Ward {2}
 * This creature enters tapped unless it's your turn.
 *
 * Adventure: Lagoon Breach — {1}{U}, Instant — Adventure
 * The owner of target attacking creature you don't control puts it on their choice of the top
 * or bottom of their library.
 *
 * "Enters tapped unless it's your turn" is [EntersTapped] with the positive
 * [Conditions.IsYourTurn] as the `unlessCondition` — flashing the Whale in on an opponent's turn
 * gets a tapped 6/6 that can't block, while hard-casting it on your own main phase gets an
 * untapped one (see Eddymurk Crab, which prints the same clause inverted).
 *
 * The Adventure is [Effects.PutOnTopOrBottomOfLibrary], which already models "their choice" —
 * the *owner* of the bounced creature picks the destination, not the spell's controller.
 *
 * (CR 715: Adventure cards. Casting the Adventure exiles the card on resolution and lets the
 * caster cast it as the creature spell while it remains in exile.)
 */
val HornedLochWhale = card("Horned Loch-Whale") {
    manaCost = "{4}{U}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Whale"
    oracleText = "Flash\nWard {2} (Whenever this creature becomes the target of a spell or ability " +
        "an opponent controls, counter it unless that player pays {2}.)\n" +
        "This creature enters tapped unless it's your turn."
    power = 6
    toughness = 6

    keywords(Keyword.FLASH)
    keywordAbility(KeywordAbility.ward("{2}"))

    replacementEffect(EntersTapped(unlessCondition = Conditions.IsYourTurn))

    adventure("Lagoon Breach") {
        manaCost = "{1}{U}"
        typeLine = "Instant — Adventure"
        oracleText = "The owner of target attacking creature you don't control puts it on their " +
            "choice of the top or bottom of their library. " +
            "(Then exile this card. You may cast the creature later from exile.)"

        spell {
            val attacker = target(
                "target attacking creature you don't control",
                TargetCreature(filter = TargetFilter.AttackingCreature.opponentControls()),
            )
            effect = Effects.PutOnTopOrBottomOfLibrary(attacker)
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "53"
        artist = "Simon Dominic"
        imageUri = "https://cards.scryfall.io/normal/front/9/6/96a05063-0556-42e4-8d4c-8e92be160ef5.jpg?1783915120"

        ruling(
            "2023-09-01",
            "The creature's owner chooses whether to put it on the top or bottom of their library. " +
                "If multiple cards are put into the library this way (such as when the spell targets " +
                "a melded permanent), that creature's owner puts all the cards on top or all the " +
                "cards on the bottom. They put them in whatever order they wish, and do not need to " +
                "reveal the order."
        )
    }
}
