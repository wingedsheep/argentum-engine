package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CycleCard
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Cursed Totem (Mirage):
 * "Activated abilities of creatures can't be activated."
 *
 * Covers mana abilities of creatures (Llanowar Elves), non-mana activated abilities on
 * a creature-land (Daru Encampment becoming a creature would be blocked — but as a land
 * it is not), and ensures abilities of noncreature permanents (lands, the totem itself)
 * remain activatable.
 */
class CursedTotemScenarioTest : ScenarioTestBase() {

    init {
        context("Cursed Totem blocks creature activated abilities") {
            test("Llanowar Elves' mana ability is not in legal actions while Cursed Totem is on the battlefield") {
                val game = scenario()
                    .withPlayers("P1", "P2")
                    .withCardOnBattlefield(1, "Cursed Totem")
                    .withCardOnBattlefield(1, "Llanowar Elves", summoningSickness = false)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val elves = game.findPermanent("Llanowar Elves")!!
                val cardDef = cardRegistry.getCard("Llanowar Elves")!!
                val manaAbility = cardDef.script.activatedAbilities.first()

                val legal = game.getLegalActions(1)
                val elvesActivation = legal.find {
                    val a = it.action
                    a is ActivateAbility && a.sourceId == elves && a.abilityId == manaAbility.id
                }
                withClue("Llanowar Elves should have no activated abilities while Cursed Totem is in play") {
                    elvesActivation shouldBe null
                }
            }

            test("Direct activation of a creature's mana ability is rejected by the handler") {
                val game = scenario()
                    .withPlayers("P1", "P2")
                    .withCardOnBattlefield(1, "Cursed Totem")
                    .withCardOnBattlefield(1, "Llanowar Elves", summoningSickness = false)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val elves = game.findPermanent("Llanowar Elves")!!
                val cardDef = cardRegistry.getCard("Llanowar Elves")!!
                val manaAbility = cardDef.script.activatedAbilities.first()

                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = elves,
                        abilityId = manaAbility.id,
                    )
                )

                withClue("Engine should reject the activation while Cursed Totem is in play") {
                    (result.error != null) shouldBe true
                }
            }

            test("Llanowar Elves' mana ability returns to legal actions once Cursed Totem leaves") {
                val game = scenario()
                    .withPlayers("P1", "P2")
                    .withCardOnBattlefield(1, "Llanowar Elves", summoningSickness = false)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val elves = game.findPermanent("Llanowar Elves")!!
                val cardDef = cardRegistry.getCard("Llanowar Elves")!!
                val manaAbility = cardDef.script.activatedAbilities.first()

                val legal = game.getLegalActions(1)
                val elvesActivation = legal.find {
                    val a = it.action
                    a is ActivateAbility && a.sourceId == elves && a.abilityId == manaAbility.id
                }
                withClue("Without Cursed Totem, Llanowar Elves' mana ability should be available") {
                    (elvesActivation != null) shouldBe true
                }
            }
        }

        context("Cursed Totem does not block non-creature abilities") {
            test("Land mana abilities are unaffected") {
                val game = scenario()
                    .withPlayers("P1", "P2")
                    .withCardOnBattlefield(1, "Cursed Totem")
                    .withLandsOnBattlefield(1, "Forest", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val forest = game.findPermanent("Forest")!!

                val legal = game.getLegalActions(1)
                val forestActivation = legal.find {
                    val a = it.action
                    a is ActivateAbility && a.sourceId == forest
                }
                withClue("Forest's mana ability should still be available") {
                    (forestActivation != null) shouldBe true
                }
            }

            // "Activated abilities of creatures" means creature permanents — a creature card in
            // hand is not one, so it still cycles.
            test("A creature card in hand can still cycle") {
                val game = scenario()
                    .withPlayers("P1", "P2")
                    .withCardOnBattlefield(1, "Cursed Totem")
                    .withCardInHand(1, "Disciple of Law")
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withCardInLibrary(1, "Plains")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                (game.getLegalActions(1).any { it.action is CycleCard }) shouldBe true
                (game.cycleCard(1, "Disciple of Law").error == null) shouldBe true
            }
        }
    }
}
