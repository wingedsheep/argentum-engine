package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.mechanics.speed.SpeedService
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Speed
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Far Fortune, End Boss (DFT #203) — {2}{B}{R} Legendary Creature — Human Mercenary, 4/5.
 *
 * "Start your engines!
 *  Whenever you attack, Far Fortune deals 1 damage to each opponent.
 *  Max speed — If a source you control would deal damage to an opponent or a permanent an opponent
 *  controls, it deals that much damage plus 1 instead."
 *
 * The rider is the first max-speed-gated *replacement* effect on the damage side, so the things
 * worth pinning are the gate and the recipient filter:
 *
 *  - at max speed a source you control deals +1 to an opponent and to their permanents;
 *  - below max speed nothing changes (Start your engines! only puts you at 1, so the gate is the
 *    only thing standing between the two cases);
 *  - the filter is one-directional — damage to *you* or to *your own* permanents is untouched, which
 *    is what stops the rider from taxing its own controller;
 *  - the attack trigger stacks with the rider: 1 damage becomes 2 at max speed, since Far Fortune
 *    is itself a source you control.
 */
class FarFortuneEndBossScenarioTest : ScenarioTestBase() {

    init {
        context("Far Fortune's max-speed damage rider") {

            test("a source you control deals +1 to an opponent at max speed") {
                val game = farFortuneGame(maxSpeed = true)
                game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)

                game.castSpellTargetingPlayer(1, "Lightning Bolt", 2).error shouldBe null
                game.resolveStack()

                withClue("Bolt's 3 becomes 4") { game.getLifeTotal(2) shouldBe 16 }
            }

            test("the same Bolt deals its printed 3 below max speed") {
                val game = farFortuneGame(maxSpeed = false)
                game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)

                game.castSpellTargetingPlayer(1, "Lightning Bolt", 2).error shouldBe null
                game.resolveStack()

                withClue("Start your engines! only sets speed 1 — the gate must hold") {
                    game.getLifeTotal(2) shouldBe 17
                }
            }

            test("damage to a permanent an opponent controls is bumped too") {
                // A 4/4 survives Bolt's printed 3 and dies to 4, which is what makes the +1 on a
                // permanent observable at all.
                val game = farFortuneGame(maxSpeed = true, opponentCreature = "Charging Rhino")
                game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)

                val rhino = game.findPermanent("Charging Rhino")!!
                game.castSpell(1, "Lightning Bolt", rhino).error shouldBe null
                game.resolveStack()

                withClue("3 + 1 = 4 is lethal to a 4/4") { game.isOnBattlefield("Charging Rhino") shouldBe false }
            }

            test("the same 4/4 survives below max speed") {
                val game = farFortuneGame(maxSpeed = false, opponentCreature = "Charging Rhino")
                game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)

                val rhino = game.findPermanent("Charging Rhino")!!
                game.castSpell(1, "Lightning Bolt", rhino).error shouldBe null
                game.resolveStack()

                withClue("3 damage on a 4/4 is not lethal") { game.isOnBattlefield("Charging Rhino") shouldBe true }
            }

            test("damage to yourself is not bumped — the rider only taxes opponents") {
                val game = farFortuneGame(maxSpeed = true)
                game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)

                game.castSpellTargetingPlayer(1, "Lightning Bolt", 1).error shouldBe null
                game.resolveStack()

                withClue("RecipientFilter.OpponentOrPermanentTheyControl excludes you") {
                    game.getLifeTotal(1) shouldBe 17
                }
            }

            test("the attack trigger's 1 damage becomes 2 at max speed") {
                val game = farFortuneGame(maxSpeed = true)
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)

                game.declareAttackers(mapOf("Far Fortune, End Boss" to 2)).error shouldBe null
                game.resolveStack()

                withClue("Far Fortune is itself a source you control, so 1 → 2") {
                    game.getLifeTotal(2) shouldBe 18
                }
            }

            test("the attack trigger deals its printed 1 below max speed") {
                val game = farFortuneGame(maxSpeed = false)
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)

                game.declareAttackers(mapOf("Far Fortune, End Boss" to 2)).error shouldBe null
                game.resolveStack()

                game.getLifeTotal(2) shouldBe 19
            }
        }
    }

    /**
     * Far Fortune already on the battlefield with a Bolt in hand and Mountains to cast it. Speed is
     * stamped directly rather than raced up through opponent life loss — Start your engines! has
     * already put the controller at 1 by then via its state-based action.
     */
    private fun farFortuneGame(
        maxSpeed: Boolean,
        opponentCreature: String? = null
    ): TestGame {
        val builder = scenario()
            .withPlayers("Player1", "Player2")
            .withCardOnBattlefield(1, "Far Fortune, End Boss", summoningSickness = false)
            .withCardInHand(1, "Lightning Bolt")
            .withLandsOnBattlefield(1, "Mountain", 3)
        if (opponentCreature != null) {
            builder.withCardOnBattlefield(2, opponentCreature)
        }
        builder.withCardInLibrary(1, "Grizzly Bears")
        builder.withCardInLibrary(2, "Grizzly Bears")
        val game = builder
            .withActivePlayer(1)
            .inPhase(Phase.BEGINNING, Step.UPKEEP)
            .build()
        if (maxSpeed) {
            game.state = SpeedService.set(game.state, game.player1Id, Speed.MAX, "test").first
        }
        return game
    }
}
