package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Radagast of Rhosgobel — "The first creature spell you cast each turn costs {2} less to cast and
 * can be cast as though it had flash."
 *
 * The cost half rides `CostGating.NthOfTypePerTurn(1)`, which already existed. The timing half is
 * the new `GrantFlashToSpellType.nthOfTypePerTurn`, so these tests pin both halves *and* that they
 * agree on which spell is "the first" — the failure mode worth guarding is a discount that applies
 * without the flash, or a flash window that never closes.
 *
 * Grizzly Bears is {1}{G}: with the discount it costs {G} (only generic is reducible, CR 601.2f),
 * so "one Forest is enough" and "two Forests are not enough for the second one" are direct,
 * behavioural proofs of the gate rather than assertions about a displayed number.
 */
class RadagastOfRhosgobelScenarioTest : ScenarioTestBase() {

    init {
        context("Radagast of Rhosgobel — first creature spell each turn: {2} off and flash") {

            test("the turn's first creature spell costs {2} less") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Radagast of Rhosgobel", summoningSickness = false)
                    .withLandsOnBattlefield(1, "Forest", 1)
                    .withCardInHand(1, "Grizzly Bears")
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                withClue("{1}{G} reduced to {G} is payable with the single Forest") {
                    game.castSpell(1, "Grizzly Bears").error shouldBe null
                }
                game.resolveStack()
                game.isOnBattlefield("Grizzly Bears") shouldBe true
            }

            test("the second creature spell that turn pays full price") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Radagast of Rhosgobel", summoningSickness = false)
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withCardsInHand(1, "Grizzly Bears", 2)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Grizzly Bears").error shouldBe null
                game.resolveStack()

                withClue("the second Bears costs the full {1}{G}, and only one Forest is left") {
                    game.castSpell(1, "Grizzly Bears").error shouldNotBe null
                }
            }

            test("the turn's first creature spell can be cast at instant speed, and only that one") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Radagast of Rhosgobel", summoningSickness = false)
                    // Four Forests so affordability is never what stops the second cast — only the
                    // closed flash window is.
                    .withLandsOnBattlefield(1, "Forest", 4)
                    .withCardsInHand(1, "Grizzly Bears", 2)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .withPriorityPlayer(1)
                    // The end step is sorcery-speed-illegal, so any offered creature cast is flash.
                    .inPhase(Phase.ENDING, Step.END)
                    .build()

                fun bearsCastOffered() = game.getLegalActions(1).any { info ->
                    (info.action as? CastSpell)?.let { cast ->
                        game.state.getEntity(cast.cardId)?.get<CardComponent>()?.name == "Grizzly Bears"
                    } == true
                }

                withClue("no creature spell cast yet this turn, so the flash grant is live") {
                    bearsCastOffered() shouldBe true
                }

                game.castSpell(1, "Grizzly Bears").error shouldBe null
                game.resolveStack()

                withClue("the grant covered only the first creature spell this turn") {
                    bearsCastOffered() shouldBe false
                }
            }

            test("without Radagast the same board can't cast a creature in the end step") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withLandsOnBattlefield(1, "Forest", 4)
                    .withCardInHand(1, "Grizzly Bears")
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .withPriorityPlayer(1)
                    .inPhase(Phase.ENDING, Step.END)
                    .build()

                val offered = game.getLegalActions(1).any { info ->
                    (info.action as? CastSpell)?.let { cast ->
                        game.state.getEntity(cast.cardId)?.get<CardComponent>()?.name == "Grizzly Bears"
                    } == true
                }
                withClue("control: the flash in the previous test came from Radagast") {
                    offered shouldBe false
                }
            }
        }
    }
}
