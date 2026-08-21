package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.DeclareAttackers
import com.wingedsheep.engine.core.DeclareBlockers
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Head of the Hunt (The Hobbit #75):
 *   {2}{B}{B} Creature — Wolf, 4/3
 *   Flash
 *   If a creature an opponent controls would die, exile it instead. When you do, create a
 *   2/2 green Wolf creature token.
 *
 * The interesting case is the *trade*: Head of the Hunt dying in the same combat-damage event as
 * the creatures it is meant to exile. CR 704.3 performs every applicable state-based action
 * simultaneously as a single event, and a replacement effect applies as that event is about to
 * happen (CR 614.1) — so the shield is still on the battlefield and still applies. The engine
 * moves the dying creatures one at a time, so before the pass-start snapshot went into
 * [com.wingedsheep.engine.mechanics.sba.SbaZoneMovementHelper] this came down to battlefield
 * iteration order: Head of the Hunt happened to move first and the creatures it had blocked went
 * to the graveyard untouched.
 */
class HeadOfTheHuntScenarioTest : ScenarioTestBase() {

    private val projector = StateProjector()

    init {
        context("Head of the Hunt") {

            test("a creature that trades with Head of the Hunt in combat is still exiled, and still mints a Wolf") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Head of the Hunt", summoningSickness = false)
                    .withCardOnBattlefield(2, "Centaur Courser", summoningSickness = false)
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                val hunt = game.findPermanent("Head of the Hunt")!!
                val courser = game.findPermanent("Centaur Courser")!!
                val opponentId = game.player2Id

                // 4/3 attacker into a 3/3 blocker: both are dealt lethal damage and are destroyed
                // by the same state-based-action check.
                game.declareAttackers(mapOf("Head of the Hunt" to 2)).error shouldBe null
                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.declareBlockers(mapOf("Centaur Courser" to listOf("Head of the Hunt")))
                    .error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.COMBAT_DAMAGE)
                game.resolveStack()
                if (game.state.pendingDecision != null) {
                    game.submitDefaultCombatDamage()
                    game.resolveStack()
                }

                withClue("Head of the Hunt itself dies normally — it shields opponents, not you") {
                    game.state.getGraveyard(game.player1Id) shouldContain hunt
                }
                withClue("The blocker it traded with is exiled, not put into the graveyard") {
                    game.state.getGraveyard(opponentId) shouldNotContain courser
                    game.state.getExile(opponentId) shouldContain courser
                }
                withClue("The reflexive 'when you do' mints a Wolf for Head of the Hunt's controller") {
                    game.findAllPermanents("Wolf Token").size shouldBe 1
                }
            }

            test("an opponent's creature killed while Head of the Hunt survives is exiled and mints a Wolf") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Head of the Hunt", summoningSickness = false)
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withCardInHand(1, "Lightning Bolt")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                val opponentId = game.player2Id

                game.castSpell(1, "Lightning Bolt", targetId = bears).error shouldBe null
                game.resolveStack()

                withClue("The bolted creature is exiled instead of dying") {
                    game.state.getGraveyard(opponentId) shouldNotContain bears
                    game.state.getExile(opponentId) shouldContain bears
                }
                withClue("The rider mints one 2/2 green Wolf") {
                    val wolves = game.findAllPermanents("Wolf Token")
                    wolves.size shouldBe 1
                    projector.getProjectedPower(game.state, wolves.single()) shouldBe 2
                    projector.getProjectedToughness(game.state, wolves.single()) shouldBe 2
                }
            }


            test("every creature dying alongside it is exiled — two blockers, two Wolves") {
                // The reported bug was three creatures dying at once to one shield. With a single
                // blocker a fix that repaired only the first redirect in a batch would still pass.
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Head of the Hunt", summoningSickness = false)
                    .withCardOnBattlefield(2, "Grizzly Bears", summoningSickness = false)
                    .withCardOnBattlefield(2, "Grizzly Bears", summoningSickness = false)
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                val hunt = game.findPermanent("Head of the Hunt")!!
                val bears = game.findAllPermanents("Grizzly Bears")
                bears.size shouldBe 2
                val opponentId = game.player2Id

                // 4 power split as lethal-in-order kills both 2/2s; 4 damage back kills the 4/3.
                // All three die to the same state-based-action check.
                game.declareAttackers(mapOf("Head of the Hunt" to 2)).error shouldBe null
                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.execute(
                    DeclareBlockers(opponentId, bears.associateWith { listOf(hunt) })
                ).error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.COMBAT_DAMAGE)
                game.resolveStack()
                if (game.state.pendingDecision != null) {
                    game.submitDefaultCombatDamage()
                    game.resolveStack()
                }

                withClue("Head of the Hunt traded with both blockers") {
                    game.state.getGraveyard(game.player1Id) shouldContain hunt
                }
                withClue("Both blockers are exiled, neither reaches the graveyard") {
                    bears.forEach { bear ->
                        game.state.getGraveyard(opponentId) shouldNotContain bear
                        game.state.getExile(opponentId) shouldContain bear
                    }
                }
                withClue("One Wolf per creature actually redirected") {
                    game.findAllPermanents("Wolf Token").size shouldBe 2
                }
            }

            test("an opponent's token is exiled too — the filter is deliberately not nontoken") {
                // HeadOfTheHunt.kt documents that the filter is *not* nontoken(): an opponent's
                // token still "would die", so it is exiled instead (and then ceases to exist as a
                // state-based action) and still pays off the Wolf. The reported game was tokens.
                //
                // The opponent makes the tokens and then kills one of their own, which keeps the
                // whole test inside the turn where they plainly hold priority — a token minted this
                // turn has summoning sickness and cannot attack, and Head of the Hunt's filter only
                // cares who *controls* the dying creature, not who killed it.
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Head of the Hunt", summoningSickness = false)
                    .withLandsOnBattlefield(2, "Mountain", 3)
                    .withCardInHand(2, "Dragon Fodder")
                    .withCardInHand(2, "Lightning Bolt")
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(2, "Dragon Fodder").error shouldBe null
                game.resolveStack()
                val goblins = game.findAllPermanents("Goblin Token")
                withClue("Dragon Fodder made two 1/1 Goblin tokens for the opponent") {
                    goblins.size shouldBe 2
                }

                game.castSpell(2, "Lightning Bolt", targetId = goblins.first()).error shouldBe null
                game.resolveStack()

                withClue("The bolted token was redirected to exile, minting a Wolf") {
                    game.findAllPermanents("Wolf Token").size shouldBe 1
                }
                withClue("The other token is untouched") {
                    game.findAllPermanents("Goblin Token").size shouldBe 1
                }
            }

            test("a shield dying to zero toughness still shields the creatures dying with it") {
                // ZeroToughnessCheck takes the same pass-start snapshot as LethalDamageCheck.
                // Languish (-4/-4 to all creatures) puts the 4/3 at 0/-1 and the 2/2 at -2/-2, so
                // both are put into their owners' graveyards by one CR 704.5f check — the shield
                // among them.
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Head of the Hunt", summoningSickness = false)
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Swamp", 4)
                    .withCardInHand(1, "Languish")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val hunt = game.findPermanent("Head of the Hunt")!!
                val bears = game.findPermanent("Grizzly Bears")!!
                val opponentId = game.player2Id

                game.castSpell(1, "Languish").error shouldBe null
                game.resolveStack()

                withClue("Head of the Hunt died to zero toughness alongside the creature it shields") {
                    game.state.getGraveyard(game.player1Id) shouldContain hunt
                }
                withClue("The opponent's creature is exiled, not put into the graveyard") {
                    game.state.getGraveyard(opponentId) shouldNotContain bears
                    game.state.getExile(opponentId) shouldContain bears
                }
                withClue("And still mints its Wolf") {
                    game.findAllPermanents("Wolf Token").size shouldBe 1
                }
            }

            test("your own dying creature is untouched — the shield only covers opponents") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Head of the Hunt", summoningSickness = false)
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withCardInHand(1, "Lightning Bolt")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!

                game.castSpell(1, "Lightning Bolt", targetId = bears).error shouldBe null
                game.resolveStack()

                withClue("Your own creature dies to the graveyard as normal") {
                    game.state.getGraveyard(game.player1Id) shouldContain bears
                }
                withClue("No Wolf is minted") {
                    game.findAllPermanents("Wolf Token").size shouldBe 0
                }
            }
        }
    }
}
