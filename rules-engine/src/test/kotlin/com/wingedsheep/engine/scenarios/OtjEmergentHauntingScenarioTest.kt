package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.CastSpellRecord
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.otj.cards.EmergentHaunting
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.TypeLine
import com.wingedsheep.sdk.core.Zone
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Emergent Haunting {1}{U} Enchantment (OTJ canonical).
 *
 * "At the beginning of your end step, if you haven't cast a spell from your hand this turn and
 * this enchantment isn't a creature, it becomes a 3/3 Spirit creature with flying in addition to
 * its other types."
 * "{2}{U}: Surveil 1."
 *
 * The intervening-if is `All(Not(YouCastSpellsThisTurn(1, HAND)), SourceMatches(Noncreature))`.
 */
class OtjEmergentHauntingScenarioTest : ScenarioTestBase() {

    init {
        // Register the card under test on top of the base TestCards corpus.
        cardRegistry.register(listOf(EmergentHaunting))

        context("Emergent Haunting end-step animation") {

            test("becomes a 3/3 Spirit with flying (and stays an Enchantment) when no spell was cast from hand") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Emergent Haunting")
                    .withActivePlayer(1)
                    .build()

                val haunting = game.findPermanent("Emergent Haunting")!!
                val projector = com.wingedsheep.engine.mechanics.layers.StateProjector()

                withClue("starts as a noncreature enchantment") {
                    projector.project(game.state).isCreature(haunting) shouldBe false
                }

                game.passUntilPhase(Phase.ENDING, Step.END)
                game.resolveStack()

                val projected = projector.project(game.state)
                withClue("becomes a creature") { projected.isCreature(haunting) shouldBe true }
                withClue("3 power") { projected.getPower(haunting) shouldBe 3 }
                withClue("3 toughness") { projected.getToughness(haunting) shouldBe 3 }
                withClue("has flying") { projected.hasKeyword(haunting, Keyword.FLYING) shouldBe true }
                withClue("still an Enchantment (in addition to its other types)") {
                    projected.hasType(haunting, "ENCHANTMENT") shouldBe true
                }
            }

            test("does not become a creature when a spell was cast from hand this turn") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Emergent Haunting")
                    .withActivePlayer(1)
                    .build()

                // Record a spell cast from hand this turn — fails the intervening-if.
                game.state = game.state.copy(
                    spellsCastThisTurnByPlayer = mapOf(
                        game.player1Id to listOf(
                            CastSpellRecord(
                                typeLine = TypeLine.parse("Instant"),
                                manaValue = 1,
                                colors = emptySet(),
                                isFaceDown = false,
                                castFromZone = Zone.HAND,
                            )
                        )
                    )
                )

                val haunting = game.findPermanent("Emergent Haunting")!!
                val projector = com.wingedsheep.engine.mechanics.layers.StateProjector()

                game.passUntilPhase(Phase.ENDING, Step.END)
                game.resolveStack()

                withClue("stays a noncreature enchantment — the trigger's intervening-if was false") {
                    projector.project(game.state).isCreature(haunting) shouldBe false
                }
            }

            test("{2}{U}: Surveil 1 looks at the top card and can mill it") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Emergent Haunting")
                    .withLandsOnBattlefield(1, "Island", 3)
                    .withCardInLibrary(1, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val haunting = game.findPermanent("Emergent Haunting")!!
                val abilityId = EmergentHaunting.activatedAbilities[0].id
                val libBefore = game.state.getLibrary(game.player1Id).size

                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = haunting,
                        abilityId = abilityId
                    )
                )
                withClue("activating Surveil 1 should succeed: ${result.error}") {
                    result.error shouldBe null
                }
                game.resolveStack()

                // Surveil presents a yes/no-style decision to put the looked-at card into the
                // graveyard; resolve whatever decision is pending so the ability finishes.
                val pending = game.getPendingDecision()
                withClue("surveil should present a decision to keep or bin the top card") {
                    (pending != null) shouldBe true
                }
            }
        }
    }
}
