package com.wingedsheep.mtg.sets.definitions.sos.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Chooser
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.effects.EmitLibrarySearchedEventEffect

/**
 * Erode
 * {W}
 * Instant
 * Destroy target creature or planeswalker. Its controller may search their library for a basic land
 * card, put it onto the battlefield tapped, then shuffle.
 *
 * A Path-to-Exile-shaped compensation effect. The destroy resolves first, then the destroyed
 * permanent's controller — not Erode's controller — gets the optional search, so the [Effects.May]
 * gate is delegated to [EffectTarget.TargetController] and the whole search pipeline is scoped to
 * [Player.ControllerOf] (library to gather from, battlefield to put the land onto tapped, and the
 * library to shuffle). "Its controller" is resolved from the targeted permanent at resolution;
 * since the permanent has just left the battlefield, the controller falls back to its owner (the
 * standard last-known controller for a permanent that left play).
 */
val Erode = card("Erode") {
    manaCost = "{W}"
    colorIdentity = "W"
    typeLine = "Instant"
    oracleText = "Destroy target creature or planeswalker. Its controller may search their library " +
        "for a basic land card, put it onto the battlefield tapped, then shuffle."

    spell {
        val permanent = target(Targets.CreatureOrPlaneswalker)
        effect = Effects.Destroy(permanent) then Effects.May(
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
                // "Its controller may search their library" — the destroyed permanent's
                // controller picks the land, not Erode's controller. Without this the
                // caster makes the choice and sees the opponent's entire library order.
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
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "15"
        artist = "Florian Herold"
        flavorText = "\"Our lives are built upon our pasts. What happens, then, if we are cut off from them?\"\n" +
            "—Augusta, dean of order"
        imageUri = "https://cards.scryfall.io/normal/front/3/2/32e670da-7563-4f6a-a7db-4c126a440eb8.jpg"
    }
}
