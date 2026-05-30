package com.wingedsheep.mtg.sets.definitions.inv.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.CreateTokenEffect
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Saproling Symbiosis
 * {3}{G}
 * Sorcery
 * You may cast this spell as though it had flash if you pay {2} more to cast it.
 * Create a 1/1 green Saproling creature token for each creature you control.
 *
 * The flash-timing unlock reuses the shared FlashKicker plumbing (same as Ghitu Fire):
 * paying {2} more lets the sorcery be cast at instant speed. The token count reads the number
 * of creatures you control at resolution — tokens created by this spell are not counted because
 * the amount is evaluated once before the tokens enter (CR 608.2h).
 */
val SaprolingSymbiosis = card("Saproling Symbiosis") {
    manaCost = "{3}{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "You may cast this spell as though it had flash if you pay {2} more to cast it. " +
        "(You may cast it any time you could cast an instant.)\n" +
        "Create a 1/1 green Saproling creature token for each creature you control."

    keywordAbility(KeywordAbility.flashKicker("{2}"))

    spell {
        effect = CreateTokenEffect(
            count = DynamicAmount.Count(Player.You, Zone.BATTLEFIELD, GameObjectFilter.Creature),
            power = 1,
            toughness = 1,
            colors = setOf(Color.GREEN),
            creatureTypes = setOf("Saproling"),
            imageUri = "/images/tokens/inv-saproling.jpeg"
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "209"
        artist = "Ciruelo"
        imageUri = "https://cards.scryfall.io/normal/front/2/b/2bb63748-5c84-43a0-8f17-a2a17f658337.jpg?1562903901"
    }
}
