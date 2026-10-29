package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.DeclareAttackers
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.player.BlockerCountLimitThisCombatComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.c17.cards.MirriWeatherlightDuelist
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Mirri, Weatherlight Duelist (C17 #43) — {1}{G}{W} Legendary Creature — Cat Warrior 3/2.
 *
 *   First strike
 *   Whenever Mirri attacks, each opponent can't block with more than one creature this combat.
 *   As long as Mirri is tapped, no more than one creature can attack you each combat.
 */
class MirriWeatherlightDuelistScenarioTest : ScenarioTestBase() {

    /** Player 1 attacks with Bears and Lions into player 2, who controls Mirri and Ajani. */
    private fun intoMirri(mirriTapped: Boolean): TestGame = scenario().withPlayers("Attacker", "Mirri")
        .withCardOnBattlefield(1, "Grizzly Bears")
        .withCardOnBattlefield(1, "Savannah Lions")
        .withCardOnBattlefield(2, "Mirri, Weatherlight Duelist", tapped = mirriTapped)
        .withCardOnBattlefield(2, "Ajani Resolute")
        .withCardInLibrary(1, "Plains")
        .withCardInLibrary(2, "Plains")
        .withActivePlayer(1)
        .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
        .build()

    private fun TestGame.attack(vararg pairs: Pair<String, EntityId>) =
        execute(DeclareAttackers(player1Id, pairs.associate { (name, defender) -> findPermanent(name)!! to defender }))

    /** Mirri's controller attacks with Mirri; the opponent has three potential blockers. */
    private fun mirriAttacks(): TestGame {
        val game = scenario().withPlayers("Mirri", "Defender")
            .withCardOnBattlefield(1, "Mirri, Weatherlight Duelist")
            .withCardOnBattlefield(1, "Grizzly Bears")
            .withCardOnBattlefield(2, "Savannah Lions")
            .withCardOnBattlefield(2, "Hill Giant")
            .withCardOnBattlefield(2, "Elvish Mystic")
            .withCardInLibrary(1, "Plains")
            .withCardInLibrary(2, "Plains")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()
        game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
        game.declareAttackers(mapOf("Mirri, Weatherlight Duelist" to 2, "Grizzly Bears" to 2)).error shouldBe null
        game.resolveStack()
        return game
    }

    init {
        context("as long as Mirri is tapped, no more than one creature can attack you") {
            test("two creatures can't attack Mirri's tapped controller") {
                val game = intoMirri(mirriTapped = true)
                game.attack("Grizzly Bears" to game.player2Id, "Savannah Lions" to game.player2Id).error shouldNotBe null
            }

            test("one creature may attack Mirri's tapped controller") {
                val game = intoMirri(mirriTapped = true)
                game.attack("Grizzly Bears" to game.player2Id).error shouldBe null
            }

            test("untapped Mirri imposes no cap") {
                val game = intoMirri(mirriTapped = false)
                game.attack("Grizzly Bears" to game.player2Id, "Savannah Lions" to game.player2Id).error shouldBe null
            }

            test("attacks on that player's planeswalker aren't capped") {
                val game = intoMirri(mirriTapped = true)
                val ajani = game.findPermanent("Ajani Resolute")!!
                withClue("both at the planeswalker") {
                    game.attack("Grizzly Bears" to ajani, "Savannah Lions" to ajani).error shouldBe null
                }
            }

            test("in a three-player game the cap protects only Mirri's controller") {
                val driver = GameTestDriver()
                driver.registerCards(TestCards.all + MirriWeatherlightDuelist)
                val (p1, p2, p3) = driver.initMultiplayer(List(3) { Deck.of("Mountain" to 40) })
                driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
                val bears = List(2) { driver.putCreatureOnBattlefield(p1, "Grizzly Bears") }
                bears.forEach(driver::removeSummoningSickness)
                driver.tapPermanent(driver.putCreatureOnBattlefield(p2, "Mirri, Weatherlight Duelist"))
                driver.passPriorityUntil(Step.DECLARE_ATTACKERS)

                withClue("two creatures can't attack Mirri's controller") {
                    driver.declareAttackers(p1, bears.associateWith { p2 }).error shouldNotBe null
                }
                withClue("the other opponent isn't protected") {
                    driver.declareAttackers(p1, bears.associateWith { p3 }).error shouldBe null
                }
            }

            test("one at the player and one at the planeswalker is fine") {
                val game = intoMirri(mirriTapped = true)
                val ajani = game.findPermanent("Ajani Resolute")!!
                game.attack("Grizzly Bears" to game.player2Id, "Savannah Lions" to ajani).error shouldBe null
            }
        }

        context("whenever Mirri attacks, each opponent can't block with more than one creature") {
            test("the opponent can't block with two creatures") {
                val game = mirriAttacks()
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.declareBlockers(
                    mapOf(
                        "Savannah Lions" to listOf("Grizzly Bears"),
                        "Hill Giant" to listOf("Mirri, Weatherlight Duelist"),
                    )
                ).error shouldNotBe null
            }

            test("two creatures double-blocking one attacker still count as two") {
                val game = mirriAttacks()
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.declareBlockers(
                    mapOf(
                        "Savannah Lions" to listOf("Grizzly Bears"),
                        "Hill Giant" to listOf("Grizzly Bears"),
                    )
                ).error shouldNotBe null
            }

            test("blocking with one creature is allowed") {
                val game = mirriAttacks()
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.declareBlockers(mapOf("Hill Giant" to listOf("Mirri, Weatherlight Duelist"))).error shouldBe null
            }

            test("Mirri's controller isn't capped, only opponents") {
                val game = mirriAttacks()
                game.state.getEntity(game.player1Id)!!.has<BlockerCountLimitThisCombatComponent>() shouldBe false
                game.state.getEntity(game.player2Id)!!.get<BlockerCountLimitThisCombatComponent>()?.maxBlockers shouldBe 1
            }

            test("the cap holds even if Mirri leaves the battlefield before blocks") {
                val game = mirriAttacks()
                val mirri = game.findPermanent("Mirri, Weatherlight Duelist")!!
                game.state = game.state.moveToZone(
                    mirri, ZoneKey(game.player1Id, Zone.BATTLEFIELD), ZoneKey(game.player1Id, Zone.GRAVEYARD)
                )
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.declareBlockers(
                    mapOf(
                        "Savannah Lions" to listOf("Grizzly Bears"),
                        "Hill Giant" to listOf("Grizzly Bears"),
                    )
                ).error shouldNotBe null
            }

            test("the cap ends with the combat") {
                val game = mirriAttacks()
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.declareNoBlockers().error shouldBe null
                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)
                game.state.getEntity(game.player2Id)!!.has<BlockerCountLimitThisCombatComponent>() shouldBe false
            }
        }
    }
}
