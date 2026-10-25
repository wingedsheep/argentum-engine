package com.wingedsheep.mtg.sets.definitions.tla.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Chooser
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.effects.EmitLibrarySearchedEventEffect

/**
 * Price of Freedom — {1}{R} Sorcery — Lesson
 *
 * Destroy target artifact or land an opponent controls. Its controller may search
 * their library for a basic land card, put it onto the battlefield tapped, then * shuffle.
 * Draw a card.
 *
 * Same Path-to-Exile-style compensation shape as [com.wingedsheep.mtg.sets.definitions.fin.cards.Sandworm]:
 * the destroy resolves first, then the destroyed permanent's controller — not the caster —
 * gets the optional basic-land search, so the [Effects.May] gate and the search pipeline are
 * delegated to [EffectTarget.TargetController] / [Player.ControllerOf]. "Its controller"
 * resolves from the targeted permanent at resolution; since it has just left the battlefield,
 * it falls back to its owner (last-known controller for a permanent that left play). Finally
 * the caster draws a card.
 */
val PriceOfFreedom = card("Price of Freedom") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery — Lesson"
    oracleText = "Destroy target artifact or land an opponent controls. Its controller may " +
        "search their library for a basic land card, put it onto the battlefield tapped, then shuffle.\n" +
        "Draw a card."

    spell {
        val permanent = target(TargetFilter(GameObjectFilter.ArtifactOrLand.opponentControls()))
        effect = Effects.Destroy(permanent) then
            Effects.May(
                effect = Effects.Pipeline {
                    val searchable = gather(
                        CardSource.FromZone(
                            zone = Zone.LIBRARY,
                            player = Player.ControllerOf("target"),
                            filter = GameObjectFilter.BasicLand,
                        ),
                        search = true,
                        searcher = Player.ControllerOf("target"),
                    )
                    val found = chooseUpTo(1, from = searchable, chooser = Chooser.ControllerOfTarget)
                    move(
                        found,
                        CardDestination.ToZone(
                            zone = Zone.BATTLEFIELD,
                            player = Player.ControllerOf("target"),
                            placement = ZonePlacement.Tapped,
                        )
                    )
                    run(Effects.ShuffleLibrary(target = EffectTarget.TargetController))
                    run(EmitLibrarySearchedEventEffect)
                },
                decisionMaker = EffectTarget.TargetController,
            ) then
            Effects.DrawCards(1)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "149"
        artist = "Kotakan"
        flavorText = "\"We're going to win a great victory against the Fire Nation today.\"\n—Jet"
        imageUri = "https://cards.scryfall.io/normal/front/9/f/9fbe94e9-a71d-4a31-9210-c599abe08e3f.jpg?1764121025"
    }
}
