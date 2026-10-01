package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Nahiri's Sacrifice (ONE #142, {1}{R}, Sorcery).
 *
 *   As an additional cost to cast this spell, sacrifice an artifact or creature with mana value X.
 *   Nahiri's Sacrifice deals X damage divided as you choose among any number of target creatures.
 *
 * X has no {X} in the mana cost to come from: the sacrifice pins it. The engine offers one cast per
 * mana value the caster could sacrifice, each carrying its X, its narrowed sacrifice picker, the
 * damage to divide and the target cap; the validator holds the sacrifice and the division to the
 * announced X (CR 601.2d — the division is announced as the spell is cast).
 */
class NahirisSacrificeScenarioTest : ScenarioTestBase() {

    private fun board() = scenario()
        .withPlayers("Player", "Opponent")
        .withCardInHand(1, "Nahiri's Sacrifice")
        .withCardOnBattlefield(1, "Ornithopter")      // artifact creature, MV 0
        .withCardOnBattlefield(1, "Grizzly Bears")    // MV 2
        .withCardOnBattlefield(1, "Hill Giant")       // MV 4
        .withCardOnBattlefield(2, "Llanowar Elves")   // 1/1
        .withCardOnBattlefield(2, "Savannah Lions")   // 2/1
        .withCardOnBattlefield(2, "Centaur Courser")  // 3/3
        .withLandsOnBattlefield(1, "Mountain", 2)
        .withActivePlayer(1)
        .withPriorityPlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        context("Nahiri's Sacrifice") {

            test("offers one cast per mana value you could sacrifice, each with X fixed") {
                val game = board()
                val offers = game.getLegalActions(1).filter {
                    (it.action as? CastSpell)?.cardId == game.findCardsInHand(1, "Nahiri's Sacrifice").single()
                }
                withClue("X = 0 (Ornithopter), 2 (Bears) and 4 (Hill Giant) — and no offer without an X") {
                    offers.map { (it.action as CastSpell).xValue } shouldContainExactlyInAnyOrder listOf(0, 2, 4)
                }
                val xTwo = offers.single { (it.action as CastSpell).xValue == 2 }
                withClue("the X = 2 offer sacrifices only the Bears, divides 2 among at most 2 targets") {
                    xTwo.additionalCostInfo!!.validSacrificeTargets shouldBe listOf(game.findPermanent("Grizzly Bears")!!)
                    xTwo.requiresDamageDistribution shouldBe true
                    xTwo.totalDamageToDistribute shouldBe 2
                    xTwo.targetCount shouldBe 2
                }
            }

            test("sacrificing a mana value 4 creature deals 4 damage divided as announced") {
                val game = board()
                val spell = game.findCardsInHand(1, "Nahiri's Sacrifice").single()
                val giant = game.findPermanent("Hill Giant")!!
                val elves = game.findPermanent("Llanowar Elves")!!
                val courser = game.findPermanent("Centaur Courser")!!

                game.execute(
                    CastSpell(
                        game.player1Id, spell,
                        targets = listOf(ChosenTarget.Permanent(elves), ChosenTarget.Permanent(courser)),
                        xValue = 4,
                        additionalCostPayment = AdditionalCostPayment(sacrificedPermanents = listOf(giant)),
                        damageDistribution = mapOf(elves to 1, courser to 3),
                    )
                ).error shouldBe null
                game.isInGraveyard(1, "Hill Giant") shouldBe true
                game.resolveStack()

                withClue("1 to the 1/1 and 3 to the 3/3 — both die; the Lions are untouched") {
                    game.hasPendingDecision() shouldBe false
                    game.isInGraveyard(2, "Llanowar Elves") shouldBe true
                    game.isInGraveyard(2, "Centaur Courser") shouldBe true
                    game.isOnBattlefield("Savannah Lions") shouldBe true
                }
            }

            test("the sacrifice must have the announced mana value") {
                val game = board()
                val spell = game.findCardsInHand(1, "Nahiri's Sacrifice").single()
                val bears = game.findPermanent("Grizzly Bears")!!
                val courser = game.findPermanent("Centaur Courser")!!

                withClue("a mana value 2 creature can't pay for X = 4") {
                    game.execute(
                        CastSpell(
                            game.player1Id, spell,
                            targets = listOf(ChosenTarget.Permanent(courser)),
                            xValue = 4,
                            additionalCostPayment = AdditionalCostPayment(sacrificedPermanents = listOf(bears)),
                        )
                    ).error shouldNotBe null
                }
                withClue("an unannounced X is 0, which the Bears don't match either") {
                    game.execute(
                        CastSpell(
                            game.player1Id, spell,
                            targets = listOf(ChosenTarget.Permanent(courser)),
                            additionalCostPayment = AdditionalCostPayment(sacrificedPermanents = listOf(bears)),
                        )
                    ).error shouldNotBe null
                }
                game.isOnBattlefield("Grizzly Bears") shouldBe true
            }

            test("the division must total X, and no more than X targets may be chosen") {
                val game = board()
                val spell = game.findCardsInHand(1, "Nahiri's Sacrifice").single()
                val bears = game.findPermanent("Grizzly Bears")!!
                val elves = game.findPermanent("Llanowar Elves")!!
                val lions = game.findPermanent("Savannah Lions")!!
                val courser = game.findPermanent("Centaur Courser")!!
                val payBears = AdditionalCostPayment(sacrificedPermanents = listOf(bears))

                withClue("3 damage divided is not X = 2") {
                    game.execute(
                        CastSpell(
                            game.player1Id, spell,
                            targets = listOf(ChosenTarget.Permanent(elves), ChosenTarget.Permanent(courser)),
                            xValue = 2,
                            additionalCostPayment = payBears,
                            damageDistribution = mapOf(elves to 1, courser to 2),
                        )
                    ).error shouldNotBe null
                }
                withClue("three targets can't each take 1 of 2 damage") {
                    game.execute(
                        CastSpell(
                            game.player1Id, spell,
                            targets = listOf(elves, lions, courser).map { ChosenTarget.Permanent(it) },
                            xValue = 2,
                            additionalCostPayment = payBears,
                            damageDistribution = mapOf(elves to 1, lions to 1, courser to 0),
                        )
                    ).error shouldNotBe null
                }

                game.execute(
                    CastSpell(
                        game.player1Id, spell,
                        targets = listOf(ChosenTarget.Permanent(elves), ChosenTarget.Permanent(lions)),
                        xValue = 2,
                        additionalCostPayment = payBears,
                        damageDistribution = mapOf(elves to 1, lions to 1),
                    )
                ).error shouldBe null
                game.resolveStack()
                game.isInGraveyard(2, "Llanowar Elves") shouldBe true
                game.isInGraveyard(2, "Savannah Lions") shouldBe true
            }

            test("a target removed in response loses its share; the other keeps exactly its own") {
                val game = board()
                val spell = game.findCardsInHand(1, "Nahiri's Sacrifice").single()
                val giant = game.findPermanent("Hill Giant")!!
                val elves = game.findPermanent("Llanowar Elves")!!
                val courser = game.findPermanent("Centaur Courser")!!

                game.execute(
                    CastSpell(
                        game.player1Id, spell,
                        targets = listOf(ChosenTarget.Permanent(elves), ChosenTarget.Permanent(courser)),
                        xValue = 4,
                        additionalCostPayment = AdditionalCostPayment(sacrificedPermanents = listOf(giant)),
                        damageDistribution = mapOf(elves to 2, courser to 2),
                    )
                ).error shouldBe null
                game.state = game.state.moveToZone(
                    elves,
                    com.wingedsheep.engine.state.ZoneKey(game.player2Id, com.wingedsheep.sdk.core.Zone.BATTLEFIELD),
                    com.wingedsheep.engine.state.ZoneKey(game.player2Id, com.wingedsheep.sdk.core.Zone.GRAVEYARD),
                )
                game.resolveStack()

                withClue("the Courser takes its 2, not the whole 4 — it survives as a 3/3") {
                    game.isOnBattlefield("Centaur Courser") shouldBe true
                }
            }
        }
    }
}
