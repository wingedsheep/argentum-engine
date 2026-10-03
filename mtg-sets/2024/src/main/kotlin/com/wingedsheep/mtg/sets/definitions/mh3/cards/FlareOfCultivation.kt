package com.wingedsheep.mtg.sets.definitions.mh3.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.SelfAlternativeCost
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Flare of Cultivation {1}{G}{G}
 * Sorcery
 *
 * You may sacrifice a nontoken green creature rather than pay this spell's mana cost.
 * Search your library for up to two basic land cards, reveal those cards, put one onto the
 * battlefield tapped and the other into your hand, then shuffle.
 *
 * The search is Cultivate's shape: pick up to two basics, then pick which found card enters
 * tapped — the remainder goes to hand.
 */
val FlareOfCultivation = card("Flare of Cultivation") {
    manaCost = "{1}{G}{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "You may sacrifice a nontoken green creature rather than pay this spell's mana cost.\n" +
        "Search your library for up to two basic land cards, reveal those cards, put one onto the " +
        "battlefield tapped and the other into your hand, then shuffle."

    selfAlternativeCost = SelfAlternativeCost(
        manaCost = ManaCost.parse("{0}"),
        additionalCosts = listOf(
            Costs.additional.SacrificePermanent(GameObjectFilter.Creature.withColor(Color.GREEN).nontoken())
        )
    )

    spell {
        effect = Effects.Pipeline {
            val searchable = gather(
                CardSource.FromZone(Zone.LIBRARY, Player.You, GameObjectFilter.BasicLand),
                search = true
            )
            val found = chooseUpTo(
                2,
                from = searchable,
                prompt = "Search your library for up to two basic land cards"
            )
            val (toBattlefield, toHandCards) = chooseExactlySplit(
                1,
                from = found,
                selectedLabel = "Onto the battlefield tapped",
                remainderLabel = "Into your hand",
                prompt = "Choose which basic land enters the battlefield tapped; the other goes to your hand."
            )
            move(
                toBattlefield,
                CardDestination.ToZone(Zone.BATTLEFIELD, placement = ZonePlacement.Tapped),
                revealed = true
            )
            toHand(toHandCards, revealed = true)
            run(Effects.ShuffleLibrary())
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "154"
        artist = "Billy Christian"
        flavorText = "\"You need to expand your horizons.\""
        imageUri = "https://cards.scryfall.io/normal/front/b/d/bda2fa87-81c0-41ac-8831-556ce392c058.jpg?1783911261"
    }
}
