package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Flicker of Fate (THB #16, reprinted J22 #56) — {1}{W} Instant.
 *
 *   Exile target creature or enchantment, then return it to the battlefield under its owner's control.
 *
 * Rulings exercised: the returned permanent is a new object (Auras on a blinked creature go to the
 * graveyard), and a blinked Aura's controller chooses what it enchants as it returns.
 */
class FlickerOfFateScenarioTest : ScenarioTestBase() {

    init {
        context("Flicker of Fate") {

            test("blinks an opponent's creature back under its owner's control") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Flicker of Fate")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                val cast = game.castSpell(1, "Flicker of Fate", bears)
                withClue("Cast should succeed: ${cast.error}") { cast.error shouldBe null }
                game.resolveStack()

                val returned = game.findPermanent("Grizzly Bears")
                withClue("Grizzly Bears is back on the battlefield") { returned shouldNotBe null }
                withClue("it returns under its owner's (Player2's) control") {
                    game.state.getEntity(returned!!)!!.get<ControllerComponent>()?.playerId shouldBe game.player2Id
                }
                withClue("it is not in exile") { game.isInExile(2, "Grizzly Bears") shouldBe false }
            }

            test("blinking an enchanted creature sends its Aura to the graveyard") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Flicker of Fate")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardAttachedTo(1, "Holy Strength", "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                game.castSpell(1, "Flicker of Fate", bears).error shouldBe null
                game.resolveStack()

                withClue("Grizzly Bears returned") { game.findPermanent("Grizzly Bears") shouldNotBe null }
                withClue("Holy Strength fell off and went to the graveyard") {
                    game.isInGraveyard(1, "Holy Strength") shouldBe true
                }
            }

            test("blinking an Aura lets its controller choose a new host as it returns") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Flicker of Fate")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(1, "Hill Giant")
                    .withCardAttachedTo(1, "Holy Strength", "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val aura = game.findPermanent("Holy Strength")!!
                val giant = game.findPermanent("Hill Giant")!!
                game.castSpell(1, "Flicker of Fate", aura).error shouldBe null
                game.resolveStack()

                withClue("the returning Aura asks what to enchant") { game.hasPendingDecision() shouldBe true }
                game.selectTargets(listOf(giant)).error shouldBe null

                val returned = game.findPermanent("Holy Strength")
                withClue("Holy Strength is back on the battlefield") { returned shouldNotBe null }
                withClue("it is now attached to Hill Giant") {
                    game.state.getEntity(returned!!)!!.get<AttachedToComponent>()?.targetId shouldBe giant
                }
            }

            test("a blinked token ceases to exist and does not return") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Flicker of Fate")
                    .withCardOnBattlefield(1, "Grizzly Bears", isToken = true)
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val token = game.findPermanent("Grizzly Bears")!!
                game.castSpell(1, "Flicker of Fate", token).error shouldBe null
                game.resolveStack()

                withClue("the token does not come back to the battlefield") {
                    game.findPermanent("Grizzly Bears") shouldBe null
                }
            }

            test("cannot target a land") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Flicker of Fate")
                    .withCardOnBattlefield(2, "Forest")
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val forest = game.findPermanent("Forest")!!
                withClue("a land is neither a creature nor an enchantment") {
                    game.castSpell(1, "Flicker of Fate", forest).error shouldNotBe null
                }
            }
        }
    }
}
