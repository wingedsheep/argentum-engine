package com.wingedsheep.mtg.sets.definitions.tdm.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Chooser
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.predicates.CardPredicate
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.effects.EmitLibrarySearchedEventEffect

/**
 * Magmatic Hellkite — Tarkir: Dragonstorm #111
 * {2}{R}{R} · Creature — Dragon · Rare
 * 4/5
 *
 * Flying
 * When this creature enters, destroy target nonbasic land an opponent controls. Its
 * controller searches their library for a basic land card, puts it onto the battlefield
 * tapped with a stun counter on it, then shuffles.
 *
 * The "compensating ramp" is performed by the destroyed land's controller, not by the
 * Hellkite's controller. Modeled as an atomic Gather -> Select -> Move pipeline scoped to
 * that opponent via [Player.ControllerOf] / [Chooser.TargetPlayer] / [EffectTarget.TargetController]:
 *  - Destroy the targeted land first.
 *  - Gather basic lands from *the land controller's* library (after destruction the
 *    relational player reference falls back to the land's owner, which for a land equals
 *    its controller, so the correct opponent still searches).
 *  - That player selects up to one (a search may always fail to find — CR 701.23b).
 *  - The chosen basic enters *their* battlefield tapped with a stun counter via
 *    [MoveCollectionEffect.addCounterType].
 *  - Then that player shuffles.
 */
val MagmaticHellkite = card("Magmatic Hellkite") {
    manaCost = "{2}{R}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Dragon"
    power = 4
    toughness = 5
    oracleText = "Flying\n" +
        "When this creature enters, destroy target nonbasic land an opponent controls. Its " +
        "controller searches their library for a basic land card, puts it onto the battlefield " +
        "tapped with a stun counter on it, then shuffles."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.enters()
        val land = target(
            TargetFilter(
                    GameObjectFilter(
                        cardPredicates = listOf(
                            CardPredicate.IsLand,
                            CardPredicate.Not(CardPredicate.IsBasicLand),
                        )
                    )
                ).opponentControls(),
        )

        // The land controller (target[0]'s controller) ramps a basic, tapped, stunned.
        val landController = Player.ControllerOf("nonbasic land an opponent controls")

        effect = Effects.Destroy(land) then
            Effects.Pipeline {
                val rampLands = gather(
                    CardSource.FromZone(Zone.LIBRARY, landController, GameObjectFilter.BasicLand),
                    search = true,
                    searcher = landController
                )
                val rampChosen = chooseUpTo(1, from = rampLands, chooser = Chooser.ControllerOfTarget)
                move(
                    rampChosen,
                    CardDestination.ToZone(
                        Zone.BATTLEFIELD,
                        landController,
                        ZonePlacement.Tapped
                    ),
                    addCounterType = CounterType.STUN
                )
                run(Effects.ShuffleLibrary(target = EffectTarget.TargetController))
                run(EmitLibrarySearchedEventEffect)
            }
        description = "destroy target nonbasic land an opponent controls. Its controller searches " +
            "their library for a basic land card, puts it onto the battlefield tapped with a stun " +
            "counter on it, then shuffles."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "111"
        artist = "Tyler Walpole"
        imageUri = "https://cards.scryfall.io/normal/front/b/3/b3b3aec8-d931-4c7f-86b5-1e7dfb717b59.jpg?1743204407"
    }
}
