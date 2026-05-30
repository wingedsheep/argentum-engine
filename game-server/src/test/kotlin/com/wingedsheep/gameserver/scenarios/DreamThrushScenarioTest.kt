package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.mechanics.mana.ManaSolver
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Scenario tests for Dream Thrush.
 *
 * Card reference:
 * - Dream Thrush ({1}{U}): Creature — Bird, 1/1, Flying
 *   "{T}: Target land becomes the basic land type of your choice until end of turn."
 *
 * Exercises the new [com.wingedsheep.sdk.scripting.effects.SetLandTypeEffect] / SetBasicLandTypes
 * floating modification — "becomes" REPLACES the land's existing land subtypes (Rule 305.7),
 * unlike the additive AddSubtype.
 */
class DreamThrushScenarioTest : ScenarioTestBase() {

    private val stateProjector = StateProjector()
    private val manaSolver = ManaSolver(cardRegistry)

    /** Activate Dream Thrush's tap ability targeting [landId] and choose [landType]. */
    private fun ScenarioTestBase.TestGame.changeLandType(landId: com.wingedsheep.sdk.model.EntityId, landType: String) {
        val thrushId = findPermanent("Dream Thrush")!!
        val ability = cardRegistry.getCard("Dream Thrush")!!.script.activatedAbilities.first()

        val result = execute(
            ActivateAbility(
                playerId = player1Id,
                sourceId = thrushId,
                abilityId = ability.id,
                targets = listOf(ChosenTarget.Permanent(landId))
            )
        )
        withClue("Activation should succeed: ${result.error}") {
            result.error shouldBe null
        }

        resolveStack()

        val decision = getPendingDecision()
        decision.shouldNotBeNull()
        decision.shouldBeInstanceOf<ChooseOptionDecision>()
        val index = decision.options.indexOf(landType)
        withClue("'$landType' should be among options ${decision.options}") {
            (index >= 0) shouldBe true
        }
        submitDecision(OptionChosenResponse(decision.id, index))
        resolveStack()
    }

    init {
        context("Dream Thrush changes a target land's type") {

            test("opponent's Forest becomes an Island and produces blue mana") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Dream Thrush")
                    .withLandsOnBattlefield(2, "Forest", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val forest = game.findPermanent("Forest")!!
                game.changeLandType(forest, "Island")

                val projected = stateProjector.project(game.state)
                withClue("Land should now be an Island") {
                    projected.hasSubtype(forest, "Island") shouldBe true
                }
                withClue("Land should no longer be a Forest (becomes = replace, Rule 305.7)") {
                    projected.hasSubtype(forest, "Forest") shouldBe false
                }
                withClue("Land should still be a Land") {
                    projected.hasType(forest, "LAND") shouldBe true
                }
                withClue("Opponent should be able to pay {U} from the now-Island land") {
                    manaSolver.canPay(game.state, game.player2Id, ManaCost.parse("{U}")) shouldBe true
                }
                withClue("Opponent should not be able to pay {G} (no longer a Forest)") {
                    manaSolver.canPay(game.state, game.player2Id, ManaCost.parse("{G}")) shouldBe false
                }
            }

            test("the type change wears off at end of turn") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Dream Thrush")
                    .withLandsOnBattlefield(2, "Mountain", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val mountain = game.findPermanent("Mountain")!!
                game.changeLandType(mountain, "Island")

                withClue("Land should be an Island during the turn") {
                    stateProjector.project(game.state).hasSubtype(mountain, "Island") shouldBe true
                }

                // Advance through the cleanup step into the next turn.
                game.passUntilPhase(Phase.ENDING, Step.END)
                game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)

                val projected = stateProjector.project(game.state)
                withClue("Land should be a Mountain again after end of turn") {
                    projected.hasSubtype(mountain, "Mountain") shouldBe true
                }
                withClue("Land should no longer be an Island") {
                    projected.hasSubtype(mountain, "Island") shouldBe false
                }
            }

            test("the land surfaces a type-change badge while it is the chosen type") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Dream Thrush")
                    .withLandsOnBattlefield(2, "Forest", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val forest = game.findPermanent("Forest")!!

                fun typeChangeBadges() = game.getClientState(1).cards.values
                    .first { it.id == forest }
                    .activeEffects
                    .filter { it.icon == "type-change" }

                withClue("Before the change the land has no type-change badge") {
                    typeChangeBadges() shouldBe emptyList()
                }

                game.changeLandType(forest, "Island")

                withClue("After becoming an Island the land surfaces exactly one type-change badge") {
                    val badges = typeChangeBadges()
                    badges.size shouldBe 1
                    badges[0].name shouldBe "Island"
                }
            }
        }
    }
}
