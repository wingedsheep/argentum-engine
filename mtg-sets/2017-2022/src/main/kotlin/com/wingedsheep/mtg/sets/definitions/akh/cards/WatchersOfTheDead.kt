package com.wingedsheep.mtg.sets.definitions.akh.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Chooser
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Watchers of the Dead
 * {2}
 * Artifact Creature — Cat
 * 2/2
 * Exile this creature: Each opponent chooses two cards in their graveyard and exiles the rest.
 *
 * [Effects.ForEachPlayer] over the opponents makes each iterated opponent the pipeline's
 * controller, so `Player.You` reads their graveyard and [Chooser.Controller] has *them* pick the
 * two cards they keep; everything else in that graveyard is exiled.
 */
val WatchersOfTheDead = card("Watchers of the Dead") {
    manaCost = "{2}"
    typeLine = "Artifact Creature — Cat"
    power = 2
    toughness = 2
    oracleText = "Exile this creature: Each opponent chooses two cards in their graveyard and exiles the rest."

    activatedAbility {
        cost = Costs.ExileSelf
        effect = Effects.ForEachPlayer(
            players = Player.EachOpponent,
            Effects.Pipeline {
                val graveyard = gather(CardSource.FromZone(Zone.GRAVEYARD, Player.You))
                val (_, rest) = chooseExactlySplit(
                    count = 2,
                    from = graveyard,
                    chooser = Chooser.Controller,
                    prompt = "Choose two cards in your graveyard to keep. Exile the rest.",
                    selectedLabel = "Keep",
                    remainderLabel = "Exile"
                )
                exile(rest)
            }
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "238"
        artist = "Joseph Meehan"
        flavorText = "They are tasked with protecting the entrance to the embalming chambers."
        imageUri = "https://cards.scryfall.io/normal/front/3/0/30efc831-3673-4bc1-8a25-150c0349253a.jpg?1783936447"
    }
}
