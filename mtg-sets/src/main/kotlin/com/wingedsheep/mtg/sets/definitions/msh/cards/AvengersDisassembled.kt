package com.wingedsheep.mtg.sets.definitions.msh.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Chooser
import com.wingedsheep.sdk.scripting.effects.DealDamageEffect
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.MayEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectFromCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectionMode
import com.wingedsheep.sdk.scripting.effects.ShuffleLibraryEffect
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Avengers Disassembled
 * {1}{R}{R}
 * Sorcery
 *
 * Choose one or both —
 * • Avengers Disassembled deals 3 damage to each creature.
 * • Destroy target land. Its controller may search their library for a basic land card, put it
 *   onto the battlefield tapped, then shuffle.
 *
 * Second mode mirrors [com.wingedsheep.mtg.sets.definitions.sos.cards.Erode]'s compensation
 * shape: the search/shuffle is scoped to the destroyed land's controller (not this spell's
 * controller) via [Chooser.ControllerOfTarget] / [Player.ControllerOf] / [EffectTarget.TargetController].
 */
val AvengersDisassembled = card("Avengers Disassembled") {
    manaCost = "{1}{R}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Choose one or both —\n" +
        "• Avengers Disassembled deals 3 damage to each creature.\n" +
        "• Destroy target land. Its controller may search their library for a basic land card, " +
        "put it onto the battlefield tapped, then shuffle."

    spell {
        modal(chooseCount = 2, minChooseCount = 1) {
            mode("Avengers Disassembled deals 3 damage to each creature") {
                effect = Effects.ForEachInGroup(
                    filter = GroupFilter.AllCreatures,
                    effect = DealDamageEffect(3, EffectTarget.Self),
                )
            }
            mode(
                "Destroy target land. Its controller may search their library for a basic land " +
                    "card, put it onto the battlefield tapped, then shuffle."
            ) {
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
                                chooser = Chooser.ControllerOfTarget,
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
                    // The Gather/Select/Move/Shuffle pipeline's auto-composed description reads
                    // poorly as a yes/no prompt (each sub-effect's .description concatenated
                    // verbatim) — override with the oracle-text phrasing, shown from the
                    // decision-maker's own perspective (they see "you", not "its controller").
                    descriptionOverride = "You may search your library for a basic land card, " +
                        "put it onto the battlefield tapped, then shuffle.",
                )
            }
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "124"
        artist = "David Szabo"
        imageUri = "https://cards.scryfall.io/normal/front/7/2/72d3e750-870b-497d-80d7-e3df097db554.jpg?1783902933"
    }
}
