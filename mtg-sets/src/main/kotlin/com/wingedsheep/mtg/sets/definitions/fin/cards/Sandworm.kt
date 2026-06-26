package com.wingedsheep.mtg.sets.definitions.fin.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.MayEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectFromCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectionMode
import com.wingedsheep.sdk.scripting.effects.ShuffleLibraryEffect
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Sandworm
 * {4}{R}
 * Creature — Worm
 * 5/4
 * Haste
 * When this creature enters, destroy target land. Its controller may search their library for a
 * basic land card, put it onto the battlefield tapped, then shuffle.
 *
 * A Stone-Rain-shaped land destruction stapled to an ETB, with the same Path-to-Exile-style
 * compensation as [com.wingedsheep.mtg.sets.definitions.sos.cards.Erode]: the destroy resolves
 * first, then the destroyed land's controller — not Sandworm's controller — gets the optional
 * search, so the [MayEffect] gate is delegated to [EffectTarget.TargetController] and the search
 * pipeline is scoped to [Player.ControllerOf] (library to gather from, battlefield to put the basic
 * onto tapped, and the library to shuffle). "Its controller" resolves from the targeted land at
 * resolution; since the land has just left the battlefield, it falls back to its owner (the standard
 * last-known controller for a permanent that left play).
 */
val Sandworm = card("Sandworm") {
    manaCost = "{4}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Worm"
    power = 5
    toughness = 4
    oracleText = "Haste\n" +
        "When this creature enters, destroy target land. Its controller may search their library " +
        "for a basic land card, put it onto the battlefield tapped, then shuffle."

    keywords(Keyword.HASTE)

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        val land = target("target land", Targets.Land)
        effect = Effects.Destroy(land) then MayEffect(
            effect = Effects.Composite(
                listOf(
                    GatherCardsEffect(
                        source = CardSource.FromZone(
                            zone = Zone.LIBRARY,
                            player = Player.ControllerOf("target"),
                            filter = GameObjectFilter.BasicLand,
                        ),
                        storeAs = "searchable",
                    ),
                    SelectFromCollectionEffect(
                        from = "searchable",
                        selection = SelectionMode.ChooseUpTo(DynamicAmount.Fixed(1)),
                        storeSelected = "found",
                    ),
                    MoveCollectionEffect(
                        from = "found",
                        destination = CardDestination.ToZone(
                            zone = Zone.BATTLEFIELD,
                            player = Player.ControllerOf("target"),
                            placement = ZonePlacement.Tapped,
                        ),
                    ),
                    ShuffleLibraryEffect(target = EffectTarget.TargetController),
                ),
            ),
            decisionMaker = EffectTarget.TargetController,
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "155"
        artist = "Awanqi (Angela Wang)"
        imageUri = "https://cards.scryfall.io/normal/front/a/6/a6e021da-2397-4ad3-a07b-65c701df531a.jpg?1748706340"
    }
}
