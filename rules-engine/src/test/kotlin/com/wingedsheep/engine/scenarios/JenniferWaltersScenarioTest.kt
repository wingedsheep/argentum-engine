package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Vanilla attackers with known, *distinct* power, so one combat-damage event produces three
 * She-Hulk triggers carrying three different numbers. Catalog creatures are not used here: the
 * whole point of the card is taking the biggest number, and that assertion must not rest on a
 * remembered P/T.
 */
private fun raider(cardName: String, attackPower: Int) = card(cardName) {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Human Warrior"
    power = attackPower
    toughness = 1
}

private val OnePowerRaider = raider("One-Power Raider", 1)
private val TwoPowerRaider = raider("Two-Power Raider", 2)
private val FivePowerRaider = raider("Five-Power Raider", 5)

/**
 * Scenario tests for Jennifer Walters // The Sensational She-Hulk (MSH #18).
 *
 * Front — {1}{W} Legendary Creature — Human Advisor Hero 2/3: "Your opponents can't cast spells
 * during your turn." · "{3}{G}{W}{W}: Transform Jennifer Walters. Activate only as a sorcery."
 *
 * Back — Legendary Creature — Gamma Hero 6/6, reach + trample, the same lock, plus:
 * "Whenever a creature you control is dealt damage, you may have The Sensational She-Hulk deal
 *  that much damage to any target. **Do this only once each turn.**"
 *
 * The back face is why this card needed `TriggeredAbility.effectOncePerTurn`. Per CR 603.2h the
 * ability "triggers only if its source's controller has not yet taken the indicated action that
 * turn": until She-Hulk has mirrored something, a multi-block puts one instance on the stack per
 * damaged creature and the controller declines down the line to the one they want; once she has,
 * the ability stops triggering and instances still on the stack do nothing as they resolve.
 * Modelling the rider as the *trigger* cap (`oncePerTurn`) would spend the turn's only fire on the
 * first trigger — even a declined one — and make the biggest damage number unreachable. The
 * primitive itself is covered by [EffectOncePerTurnTest]; this file covers the card.
 */
class JenniferWaltersScenarioTest : ScenarioTestBase() {

    init {
        cardRegistry.register(listOf(OnePowerRaider, TwoPowerRaider, FivePowerRaider))

        /**
         * Drive one batch of She-Hulk triggers to completion, targeting [targetId] throughout.
         *
         * A capped targeted trigger takes the ordinary targeted path, so a target is chosen for
         * every instance when it is put on the stack (CR 603.3d) and the "you may" is asked as each
         * instance *resolves* (Legolas, Counter of Kills ruling). [acceptNth] names the 1-based
         * yes/no question to accept — 0 declines every one. Returns how many yes/no questions were
         * raised; once an instance takes the action the rest resolve silently and never ask.
         */
        fun driveTriggers(game: TestGame, targetId: EntityId, acceptNth: Int): Int {
            var asked = 0
            var guard = 0
            while (guard++ < 40) {
                when (val decision = game.getPendingDecision()) {
                    is ChooseTargetsDecision -> game.selectTargets(listOf(targetId))
                    is YesNoDecision -> {
                        asked++
                        game.answerYesNo(asked == acceptNth)
                    }
                    null -> {
                        if (game.state.stack.isEmpty()) return asked
                        game.resolveStack()
                    }
                    else -> error("unexpected pending decision: $decision")
                }
            }
            error("She-Hulk decision loop did not settle")
        }

        /**
         * Player 2 attacks with the three raiders (power 1, 2 and 5); player 1 blocks each with a
         * separate creature, so all three blockers are dealt damage in one combat-damage event —
         * three simultaneous instances carrying three *different* numbers. The blockers all
         * survive, so the only triggers are the three mirrors.
         */
        fun multiBlockCombat(): TestGame {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "The Sensational She-Hulk")
                .withCardOnBattlefield(1, "Force of Nature")
                .withCardOnBattlefield(1, "Centaur Courser")
                .withCardOnBattlefield(1, "Phantom Warrior")
                .withCardOnBattlefield(2, "Five-Power Raider")
                .withCardOnBattlefield(2, "Two-Power Raider")
                .withCardOnBattlefield(2, "One-Power Raider")
                .withLifeTotal(1, 20)
                .withLifeTotal(2, 20)
                .withActivePlayer(2)
                .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                .build()

            game.declareAttackers(
                mapOf("Five-Power Raider" to 1, "Two-Power Raider" to 1, "One-Power Raider" to 1)
            ).error shouldBe null
            game.advanceToPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
            game.declareBlockers(
                mapOf(
                    "Force of Nature" to listOf("Five-Power Raider"),
                    "Centaur Courser" to listOf("Two-Power Raider"),
                    "Phantom Warrior" to listOf("One-Power Raider"),
                )
            ).error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.COMBAT_DAMAGE)
            return game
        }

        /** The damage amounts in play, and therefore the only legal single-mirror life losses. */
        val mirrorAmounts = setOf(1, 2, 5)

        context("The Sensational She-Hulk — the damage mirror") {

            test("declining an instance keeps offering the next, and only one mirror lands") {
                val game = multiBlockCombat()

                val asked = driveTriggers(game, game.player2Id, acceptNth = 3)

                withClue("CR 603.2h: while the action is untaken, every damaged creature triggers") {
                    asked shouldBe 3
                }
                withClue("exactly one instance mirrored — a single amount, never a sum") {
                    mirrorAmounts shouldContain (20 - game.getLifeTotal(2))
                }
            }

            test("once one instance mirrors, the rest do nothing as they resolve") {
                val game = multiBlockCombat()

                // Accept the first question. The other two instances are still on the stack, but
                // She-Hulk has taken the action, so they resolve silently — no "say yes twice and
                // get one mirror" trap (Nykthos Paragon: "other instances will do nothing as they
                // resolve").
                val asked = driveTriggers(game, game.player2Id, acceptNth = 1)

                withClue("no pointless second and third question") { asked shouldBe 1 }
                withClue("exactly one mirror landed") {
                    mirrorAmounts shouldContain (20 - game.getLifeTotal(2))
                }
            }

            test("each instance mirrors its own damage number, so the biggest is reachable") {
                // Order-independent by construction: take a different instance in each run and
                // check the three runs between them produce all three numbers. That pins
                // TRIGGER_DAMAGE_AMOUNT to the *instance* rather than to the batch, and it is what
                // makes "decline down to the 5" a real line of play. Which prompt is which is
                // covered separately by the dynamic-hint test below.
                val mirrored = (1..3).map { nth ->
                    val game = multiBlockCombat()
                    val asked = driveTriggers(game, game.player2Id, acceptNth = nth)
                    withClue("declining the first ${nth - 1} left the action untaken") {
                        asked shouldBe nth
                    }
                    20 - game.getLifeTotal(2)
                }

                withClue("the three instances carry three different numbers: $mirrored") {
                    mirrored.toSet() shouldBe mirrorAmounts
                }
            }

            test("each prompt names its own damage number, so the three are tellable apart") {
                // The printed sentence is "that much damage" on every instance, so without a
                // dynamic hint a multi-block asks the same question three times and the player
                // chooses blind — the choice the card is built around would be unusable at a
                // table even though the engine offers it. Decline everything and collect the hints.
                val game = multiBlockCombat()
                val hints = mutableListOf<String>()
                var guard = 0
                while (guard++ < 40) {
                    when (val decision = game.getPendingDecision()) {
                        is ChooseTargetsDecision -> game.selectTargets(listOf(game.player2Id))
                        is YesNoDecision -> {
                            decision.hint.shouldNotBeNull()
                            hints += decision.hint
                            game.answerYesNo(false)
                        }
                        null -> {
                            if (game.state.stack.isEmpty()) break
                            game.resolveStack()
                        }
                        else -> error("unexpected pending decision: $decision")
                    }
                }

                withClue("one hint per instance: $hints") { hints shouldHaveSize 3 }
                withClue("every hint names its instance's damage: $hints") {
                    hints.map { hint -> mirrorAmounts.filter { "$it damage" in hint } }
                        .flatten()
                        .toSet() shouldBe mirrorAmounts
                }
                withClue("the placeholder was substituted, not printed: $hints") {
                    hints.none { "{n}" in it } shouldBe true
                }
            }

            test("a declined trigger does not spend the turn — a bigger later hit still mirrors") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "The Sensational She-Hulk")
                    .withCardOnBattlefield(1, "Force of Nature")
                    .withCardOnBattlefield(1, "Centaur Courser")
                    .withCardOnBattlefield(2, "Goblin Guide")
                    .withCardInHand(1, "Lightning Bolt")
                    .withLandsOnBattlefield(1, "Mountain", 2)
                    .withLifeTotal(1, 20)
                    .withLifeTotal(2, 20)
                    .withActivePlayer(2)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                // Combat: the 2/1 Goblin Guide is blocked by Force of Nature, which is dealt 2.
                game.declareAttackers(mapOf("Goblin Guide" to 1)).error shouldBe null
                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.declareBlockers(
                    mapOf("Force of Nature" to listOf("Goblin Guide"))
                ).error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.COMBAT_DAMAGE)

                withClue("the 2-damage trigger is offered and declined") {
                    driveTriggers(game, game.player2Id, acceptNth = 0) shouldBe 1
                }
                withClue("nothing was mirrored") { game.getLifeTotal(2) shouldBe 20 }

                // Still the opponent's turn: bolt your own Centaur Courser for a bigger number.
                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)
                game.passPriority() // active player (Player2) passes; Player1 gets priority
                val courser = game.findPermanent("Centaur Courser")!!
                game.castSpell(1, "Lightning Bolt", targetId = courser).error shouldBe null
                game.resolveStack()

                withClue("the declined trigger left the action untaken, so this one is offered") {
                    driveTriggers(game, game.player2Id, acceptNth = 1) shouldBe 1
                }
                withClue("She-Hulk mirrors the *later, bigger* 3 damage") {
                    game.getLifeTotal(2) shouldBe 17
                }
            }
        }

        context("Jennifer Walters — the front face lock") {

            test("opponents can't cast spells during your turn") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Jennifer Walters")
                    .withCardInHand(2, "Lightning Bolt")
                    .withLandsOnBattlefield(2, "Mountain", 4)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.passPriority() // Player1 passes; Player2 gets priority on Player1's turn
                val blocked = game.castSpellTargetingPlayer(2, "Lightning Bolt", 1)

                withClue("PlayersCantCastSpells(EachOpponent, IsYourTurn) refuses the cast") {
                    (blocked.error != null) shouldBe true
                }
                withClue("and Player1 took no damage") { game.getLifeTotal(1) shouldBe 20 }
            }
        }
    }
}
