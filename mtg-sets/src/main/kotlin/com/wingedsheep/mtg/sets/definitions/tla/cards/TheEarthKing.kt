package com.wingedsheep.mtg.sets.definitions.tla.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.SearchDestination
import com.wingedsheep.sdk.scripting.references.Player

/**
 * The Earth King
 * {3}{G}
 * Legendary Creature — Human Noble Ally
 * 2/2
 *
 * When The Earth King enters, create a 4/4 green Bear creature token.
 * Whenever one or more creatures you control with power 4 or greater attack, search
 * your library for up to that many basic land cards, put them onto the battlefield
 * tapped, then shuffle.
 */
val TheEarthKing = card("The Earth King") {
    manaCost = "{3}{G}"
    colorIdentity = "G"
    typeLine = "Legendary Creature — Human Noble Ally"
    power = 2
    toughness = 2
    oracleText = "When The Earth King enters, create a 4/4 green Bear creature token.\n" +
        "Whenever one or more creatures you control with power 4 or greater attack, search your library for up to that many basic land cards, put them onto the battlefield tapped, then shuffle."

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Effects.CreateToken(
            power = 4,
            toughness = 4,
            colors = setOf(Color.GREEN),
            creatureTypes = setOf("Bear"),
            count = 1
        )
    }

    // "that many" = the number of attacking creatures you control with power 4 or greater,
    // counted as the ability resolves. "up to" => ChooseUpTo selection.
    triggeredAbility {
        trigger = Triggers.YouAttackWithFilter(
            GameObjectFilter.Creature.youControl().powerAtLeast(4)
        )
        effect = Patterns.Library.searchLibrary(
            filter = GameObjectFilter.BasicLand,
            count = DynamicAmounts.battlefield(
                Player.You,
                GameObjectFilter.Creature.attacking().powerAtLeast(4)
            ).count(),
            destination = SearchDestination.BATTLEFIELD,
            entersTapped = true,
            shuffleAfter = true
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "172"
        artist = "Ryota Murayama"
        flavorText = "\"Bosco seems to like him.\""
        imageUri = "https://cards.scryfall.io/normal/front/a/8/a8d5dca6-381a-4361-b265-27de8d04335c.jpg?1764121171"
    }
}
