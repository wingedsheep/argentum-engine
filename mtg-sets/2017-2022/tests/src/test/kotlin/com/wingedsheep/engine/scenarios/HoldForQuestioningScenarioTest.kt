package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Hold for Questioning (J22 #11) — {3}{U} Enchantment — Aura.
 *
 * "Enchant creature or planeswalker
 *  When this Aura enters, tap enchanted permanent and investigate.
 *  Enchanted permanent doesn't untap during its controller's untap step and its activated
 *  abilities can't be activated."
 */
class HoldForQuestioningScenarioTest : ScenarioTestBase() {

    private fun TestGame.activationsOf(playerNumber: Int, source: EntityId) =
        getLegalActions(playerNumber).filter {
            val a = it.action
            a is ActivateAbility && a.sourceId == source
        }

    init {
        context("Hold for Questioning") {

            test("enters: taps the creature, investigates, and the creature stays tapped") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardInHand(1, "Hold for Questioning")
                    .withLandsOnBattlefield(1, "Island", 4)
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!

                game.castSpell(1, "Hold for Questioning", bears).error shouldBe null
                game.resolveStack()

                withClue("the Aura attached and its trigger tapped the creature") {
                    game.isOnBattlefield("Hold for Questioning") shouldBe true
                    game.state.getEntity(bears)!!.has<TappedComponent>() shouldBe true
                }
                withClue("the Aura's controller investigated") {
                    val clue = game.findPermanent("Clue")
                    (clue != null) shouldBe true
                    game.state.getEntity(clue!!)!!.get<ControllerComponent>()?.playerId shouldBe game.player1Id
                }

                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
                withClue("it's Bob's turn") { game.state.activePlayerId shouldBe game.player2Id }
                withClue("the creature didn't untap during its controller's untap step") {
                    game.state.getEntity(bears)!!.has<TappedComponent>() shouldBe true
                }
            }

            test("enchanted creature's activated abilities can't be activated") {
                val build = { enchanted: Boolean ->
                    var b = scenario()
                        .withPlayers("Alice", "Bob")
                        .withCardOnBattlefield(2, "Llanowar Elves", summoningSickness = false)
                    if (enchanted) b = b.withCardAttachedTo(1, "Hold for Questioning", "Llanowar Elves")
                    val game = b.withActivePlayer(2)
                        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                        .build()
                    game to game.findPermanent("Llanowar Elves")!!
                }

                val (control, controlElves) = build(false)
                withClue("control: unenchanted Elves offer their mana ability") {
                    control.activationsOf(2, controlElves).isNotEmpty() shouldBe true
                }

                val (game, elves) = build(true)
                withClue("the mana ability is locked") {
                    game.activationsOf(2, elves).isEmpty() shouldBe true
                }
            }

            test("enchanted planeswalker's loyalty abilities can't be activated") {
                val build = { enchanted: Boolean ->
                    var b = scenario()
                        .withPlayers("Alice", "Bob")
                        .withCardOnBattlefield(2, "Liliana of the Veil")
                    if (enchanted) b = b.withCardAttachedTo(1, "Hold for Questioning", "Liliana of the Veil")
                    val game = b.withActivePlayer(2)
                        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                        .build()
                    val liliana = game.findPermanent("Liliana of the Veil")!!
                    game.state = game.state.updateEntity(liliana) { c ->
                        c.with(CountersComponent().withAdded(CounterType.LOYALTY, 3))
                    }
                    game to liliana
                }

                val (control, controlLiliana) = build(false)
                withClue("control: an unenchanted Liliana offers loyalty abilities") {
                    control.activationsOf(2, controlLiliana).isNotEmpty() shouldBe true
                }

                val (game, liliana) = build(true)
                withClue("enchanted Liliana offers no loyalty abilities") {
                    game.activationsOf(2, liliana).isEmpty() shouldBe true
                }
            }
        }
    }
}
