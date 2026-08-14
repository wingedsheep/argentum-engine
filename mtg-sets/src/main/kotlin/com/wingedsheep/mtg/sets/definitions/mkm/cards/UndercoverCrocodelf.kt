package com.wingedsheep.mtg.sets.definitions.mkm.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Undercover Crocodelf — Murders at Karlov Manor #239
 * {4}{G}{U} · Creature — Elf Crocodile Detective · 5/5
 *
 * Whenever this creature deals combat damage to a player, investigate.
 * Disguise {3}{G/U}{G/U}
 *
 * Six mana for a 5/5 with a damage trigger is a bad rate; {3} face down on turn three is a fine
 * one. The trade is that a face-down permanent has no abilities at all (CR 702.168a / 708.2), so
 * connecting while it's still a 2/2 investigates nothing — the Clue only starts flowing once the
 * {3}{G/U}{G/U} flip has happened.
 *
 * The trigger is SELF-bound and combat-only: burn, fight, or any other non-combat damage to a
 * player doesn't fire it.
 */
val UndercoverCrocodelf = card("Undercover Crocodelf") {
    manaCost = "{4}{G}{U}"
    colorIdentity = "GU"
    typeLine = "Creature — Elf Crocodile Detective"
    oracleText = "Whenever this creature deals combat damage to a player, investigate. (Create a " +
        "Clue token. It's an artifact with \"{2}, Sacrifice this token: Draw a card.\")\n" +
        "Disguise {3}{G/U}{G/U} (You may cast this card face down for {3} as a 2/2 creature with " +
        "ward {2}. Turn it face up any time for its disguise cost.)"
    power = 5
    toughness = 5
    disguise = "{3}{G/U}{G/U}"

    triggeredAbility {
        trigger = Triggers.DealsCombatDamageToPlayer
        effect = Effects.Investigate()
        description = "Whenever this creature deals combat damage to a player, investigate."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "239"
        artist = "Nicholas Gregory"
        imageUri = "https://cards.scryfall.io/normal/front/5/b/5bc669c8-6f39-4d52-82d3-a4005d41c8a5.jpg?1783912836"

        ruling(
            "2024-02-02",
            "Any time you have priority, you may turn the face-down creature face up by revealing " +
                "what its disguise cost is and paying that cost. This is a special action. It " +
                "doesn't use the stack and can't be responded to."
        )
        ruling(
            "2024-02-02",
            "Because the permanent is on the battlefield both before and after it's turned face " +
                "up, turning a permanent face up doesn't cause any enters-the-battlefield " +
                "abilities to trigger."
        )
        ruling(
            "2024-02-02",
            "If an effect refers to a Clue, it means any Clue artifact, not just a Clue artifact " +
                "token."
        )
    }
}
