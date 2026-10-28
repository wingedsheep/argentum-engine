package com.wingedsheep.mtg.sets.definitions.znr.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Chooser
import com.wingedsheep.sdk.scripting.effects.EmitLibrarySearchedEventEffect
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Cleansing Wildfire — Zendikar Rising #137
 * {1}{R} · Sorcery
 *
 * Destroy target land. Its controller may search their library for a basic land card, put it onto
 * the battlefield tapped, then shuffle.
 * Draw a card.
 *
 * Erode's compensation shape: the targeted land's controller (last-known once it has left the
 * battlefield) makes the "may" choice and the search. Per the ruling, an indestructible land that
 * survives still gives its controller the search and still draws you a card — so neither step is
 * gated on the destroy succeeding. An illegal target fizzles the whole spell (engine default).
 */
val CleansingWildfire = card("Cleansing Wildfire") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Destroy target land. Its controller may search their library for a basic land card, " +
        "put it onto the battlefield tapped, then shuffle.\nDraw a card."

    spell {
        val land = target(TargetFilter.Land)
        effect = Effects.Destroy(land) then Effects.May(
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
        ) then Effects.DrawCards(1)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "137"
        artist = "Mathias Kollros"
        flavorText = "Every rebirth looks like a death."
        imageUri = "https://cards.scryfall.io/normal/front/4/9/492d77e5-acc6-41b8-8930-f39d69234919.jpg?1783929361"
        ruling(
            "2020-09-25",
            "If the target land is an illegal target by the time Cleansing Wildfire tries to resolve, the " +
                "spell doesn't resolve. No player searches for a basic land card and you don't draw a card. " +
                "If the target is legal but not destroyed (most likely because it has indestructible), its " +
                "controller does search for a basic land card and you do draw a card."
        )
    }
}
