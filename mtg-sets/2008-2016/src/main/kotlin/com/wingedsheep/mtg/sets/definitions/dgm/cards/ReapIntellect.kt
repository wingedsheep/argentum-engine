package com.wingedsheep.mtg.sets.definitions.dgm.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Reap Intellect — Dragon's Maze #95
 * {X}{2}{U}{B} · Sorcery
 *
 * Target opponent reveals their hand. You choose up to X nonland cards from it and exile them.
 * For each card exiled this way, search that player's graveyard, hand, and library for any number
 * of cards with the same name as that card and exile them. Then that player shuffles.
 *
 * Pick the Brain's reveal-and-exile with an "up to X" pick, then one search per exiled card —
 * the printed "for each card exiled this way" — via a collection loop whose body matches names
 * against the iterated card. Only cards that actually reached exile drive a search; with none
 * exiled there is no search at all (2013-04-15 ruling), but the player still shuffles.
 */
val ReapIntellect = card("Reap Intellect") {
    manaCost = "{X}{2}{U}{B}"
    colorIdentity = "UB"
    typeLine = "Sorcery"
    oracleText = "Target opponent reveals their hand. You choose up to X nonland cards from it and " +
        "exile them. For each card exiled this way, search that player's graveyard, hand, and library " +
        "for any number of cards with the same name as that card and exile them. Then that player shuffles."

    spell {
        val opponent = target(Targets.Opponent)
        effect = Effects.Pipeline {
            run(Effects.RevealHand(opponent))
            val hand = gather(CardSource.FromZone(Zone.HAND, opponent.asPlayer))
            val chosen = chooseUpTo(
                DynamicAmounts.xValue(),
                from = hand,
                filter = GameObjectFilter.Nonland,
                prompt = "Choose up to X nonland cards to exile",
                selectedLabel = "Exile",
                showAllCards = true,
                alwaysPrompt = true
            )
            exile(chosen, owner = opponent.asPlayer)
            val exiledThisWay = filter(chosen, GameObjectFilter.Any.currentlyIn(Zone.EXILE))
            run(Effects.ForEachInCollection(exiledThisWay, Effects.Pipeline {
                val matches = gather(
                    CardSource.FromMultipleZones(
                        zones = listOf(Zone.GRAVEYARD, Zone.HAND, Zone.LIBRARY),
                        player = opponent.asPlayer,
                        filter = GameObjectFilter.Any.sharingNameWith(EffectTarget.IterationEntity)
                    ),
                    search = true
                )
                val toExile = chooseAnyNumber(
                    from = matches,
                    prompt = "Choose any number of cards with the same name to exile",
                    selectedLabel = "Exile"
                )
                exile(toExile, owner = opponent.asPlayer)
            }))
            run(Effects.ShuffleLibrary(target = opponent))
        }
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "95"
        artist = "Steven Belledin"
        imageUri = "https://cards.scryfall.io/normal/front/c/6/c6297df2-c67a-4054-9617-5c6202c76de8.jpg?1783940024"
        ruling(
            "2013-04-15",
            "Once Reap Intellect begins to resolve, no player can cast spells or activate abilities " +
                "until it has completely resolved. For example, the opponent can't cast any cards that " +
                "would be exiled after their hand is revealed."
        )
        ruling(
            "2013-04-15",
            "You can choose to leave cards with those names in the zone they are in. You don't have to exile them."
        )
        ruling(
            "2013-04-15",
            "If you don't exile any cards from the player's hand, you don't search that player's library."
        )
    }
}
