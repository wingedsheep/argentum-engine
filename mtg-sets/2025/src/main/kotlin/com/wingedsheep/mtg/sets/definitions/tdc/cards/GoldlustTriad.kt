package com.wingedsheep.mtg.sets.definitions.tdc.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.events.Recipient

/**
 * Goldlust Triad — Tarkir: Dragonstorm Commander #34
 * {4}{R} · Creature — Dragon · 4/3
 *
 * Flying
 * Myriad
 * Whenever this creature deals combat damage to a player, create a Treasure token.
 *
 * Myriad is engine-live ([com.wingedsheep.sdk.scripting.Myriad]); the copies carry the Treasure
 * trigger too, so each copy that connects makes its own Treasure.
 */
val GoldlustTriad = card("Goldlust Triad") {
    manaCost = "{4}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Dragon"
    power = 4
    toughness = 3
    oracleText = "Flying\nMyriad (Whenever this creature attacks, for each opponent other than defending " +
        "player, you may create a token copy that's tapped and attacking that player or a planeswalker " +
        "they control. Exile the tokens at end of combat.)\n" +
        "Whenever this creature deals combat damage to a player, create a Treasure token."

    keywords(Keyword.FLYING, Keyword.MYRIAD)

    triggeredAbility {
        trigger = Triggers.self.dealsCombatDamage(Recipient.AnyPlayer)
        effect = Effects.CreateTreasure()
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "34"
        artist = "Arif Wijaya"
        imageUri = "https://cards.scryfall.io/normal/front/2/8/2876d57a-a17e-4892-9992-321a32562e13.jpg?1783907174"
        ruling("2025-04-04", "If the defending player is your only opponent, no tokens are put onto the battlefield.")
        ruling("2025-04-04", "You choose whether each token is attacking the player or a planeswalker they control as the token is created. If it's attacking a planeswalker, you choose which one. You can't have any of the tokens attack a battle.")
        ruling("2025-04-04", "Although the tokens enter attacking, they were never declared as attackers. Abilities that trigger whenever a creature attacks won't trigger, including the myriad ability of the tokens. If there are any costs to have a creature attack, those costs won't apply to the tokens.")
    }
}
