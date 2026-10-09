package com.wingedsheep.mtg.sets.definitions.clb.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Cadira, Caller of the Small
 * {1}{G}{W}
 * Legendary Creature — Orc Ranger
 * 3/3
 * Trample
 * Whenever Cadira deals combat damage to a player, for each token you control, create a 1/1
 * white Rabbit creature token.
 *
 * The token count is read on resolution, over every token you control (not just creatures).
 */
val CadiraCallerOfTheSmall = card("Cadira, Caller of the Small") {
    manaCost = "{1}{G}{W}"
    colorIdentity = "GW"
    typeLine = "Legendary Creature — Orc Ranger"
    power = 3
    toughness = 3
    oracleText = "Trample\nWhenever Cadira deals combat damage to a player, for each token you control, " +
        "create a 1/1 white Rabbit creature token."

    keywords(Keyword.TRAMPLE)

    triggeredAbility {
        trigger = Triggers.self.dealsCombatDamage(Recipient.AnyPlayer)
        effect = Effects.CreateToken(
            count = DynamicAmounts.battlefield(Player.You, GameObjectFilter.Token).count(),
            power = 1,
            toughness = 1,
            colors = setOf(Color.WHITE),
            creatureTypes = setOf("Rabbit"),
            imageUri = "https://cards.scryfall.io/normal/front/2/2/22ca55e0-d269-4178-bc90-920a12066e4f.jpg?1783922327"
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "269"
        artist = "Alexandr Leskinen"
        flavorText = "She roams the border between wilderness and civilization, protecting those who need it most."
        imageUri = "https://cards.scryfall.io/normal/front/7/5/75994e0b-b0c7-4b0d-8f48-4be303429bd6.jpg?1783922696"
    }
}
