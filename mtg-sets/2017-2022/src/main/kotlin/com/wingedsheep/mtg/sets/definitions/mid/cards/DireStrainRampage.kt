package com.wingedsheep.mtg.sets.definitions.mid.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.MoveType
import com.wingedsheep.sdk.scripting.effects.SearchDestination
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Dire-Strain Rampage
 * {1}{R}{G}
 * Sorcery
 * Destroy target artifact, enchantment, or land. If a land was destroyed this way, its controller
 * may search their library for up to two basic land cards, put them onto the battlefield tapped,
 * then shuffle. Otherwise, its controller may search their library for a basic land card, put it
 * onto the battlefield tapped, then shuffle.
 * Flashback {3}{R}{G}
 *
 * "Destroyed this way" is read off what the destroy step actually moved: an indestructible or
 * regenerated land isn't destroyed, so its controller gets the single-land "otherwise" branch.
 * "Its controller" is the target's last-known controller once it has left the battlefield.
 */
val DireStrainRampage = card("Dire-Strain Rampage") {
    manaCost = "{1}{R}{G}"
    colorIdentity = "RG"
    typeLine = "Sorcery"
    oracleText = "Destroy target artifact, enchantment, or land. If a land was destroyed this way, its " +
        "controller may search their library for up to two basic land cards, put them onto the battlefield " +
        "tapped, then shuffle. Otherwise, its controller may search their library for a basic land card, " +
        "put it onto the battlefield tapped, then shuffle.\n" +
        "Flashback {3}{R}{G} (You may cast this card from your graveyard for its flashback cost. Then exile it.)"

    spell {
        target(TargetFilter.ArtifactEnchantmentOrLand)
        fun controllerMaySearch(count: Int) = Effects.ForEachPlayer(
            Player.ControllerOf("target artifact, enchantment, or land"),
            listOf(
                Effects.May(
                    Patterns.Library.searchLibrary(
                        filter = GameObjectFilter.BasicLand,
                        count = count,
                        destination = SearchDestination.BATTLEFIELD,
                        entersTapped = true,
                        shuffleAfter = true
                    )
                )
            )
        )
        effect = Effects.Pipeline {
            val targeted = gather(CardSource.ChosenTargets)
            val destroyed = moveTracked(
                targeted,
                CardDestination.ToZone(Zone.GRAVEYARD),
                moveType = MoveType.Destroy
            )
            ifNotEmpty(destroyed, filter = GameObjectFilter.Land) {
                run(controllerMaySearch(2))
            } orElse {
                run(controllerMaySearch(1))
            }
        }
    }

    keywordAbility(KeywordAbility.flashback("{3}{R}{G}"))

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "219"
        artist = "Darek Zabrocki"
        imageUri = "https://cards.scryfall.io/normal/front/0/f/0f4a7c01-95ca-43bf-bc53-875cfb4bffa1.jpg?1783925563"
    }
}
