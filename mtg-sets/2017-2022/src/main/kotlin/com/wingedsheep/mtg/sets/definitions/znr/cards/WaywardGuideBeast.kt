package com.wingedsheep.mtg.sets.definitions.znr.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Wayward Guide-Beast — Zendikar Rising #176
 * {R} · Creature — Beast · 2/2
 *
 * Trample, haste
 * Whenever this creature deals combat damage to a player, return a land you control to its owner's hand.
 *
 * The land is chosen on resolution and isn't targeted (no hexproof/shroud interaction); it is
 * mandatory, so with no land on the battlefield the trigger simply does nothing.
 */
val WaywardGuideBeast = card("Wayward Guide-Beast") {
    manaCost = "{R}"
    colorIdentity = "R"
    typeLine = "Creature — Beast"
    oracleText = "Trample, haste\n" +
        "Whenever this creature deals combat damage to a player, return a land you control to its owner's hand."
    power = 2
    toughness = 2

    keywords(Keyword.TRAMPLE, Keyword.HASTE)

    triggeredAbility {
        trigger = Triggers.self.dealsCombatDamage(Recipient.AnyPlayer)
        effect = Effects.Pipeline {
            val lands = gather(CardSource.BattlefieldMatching(filter = GameObjectFilter.Land, player = Player.You))
            val returned = chooseExactly(
                1,
                from = lands,
                prompt = "Return a land you control to its owner's hand",
                useTargetingUI = true
            )
            toHand(returned)
        }
        description = "Whenever this creature deals combat damage to a player, return a land you control to its owner's hand."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "176"
        artist = "Filip Burburan"
        flavorText = "Goblin cavalry tactics consist of pointing a beast in a direction and hanging on for " +
            "as long as possible."
        imageUri = "https://cards.scryfall.io/normal/front/d/0/d00f8ab0-61cd-4721-b974-a2516da77d39.jpg"
    }
}
