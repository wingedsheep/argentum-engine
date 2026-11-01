package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Scenario tests for Gideon's Sacrifice (WAR #14).
 *
 *   Choose a creature or planeswalker you control. All damage that would be dealt this turn to you
 *   and permanents you control is dealt to the chosen permanent instead (if it's still on the
 *   battlefield).
 *
 * Pins the rulings: damage to you and to your other permanents moves to the chosen one, combat
 * damage too (including combat damage to a planeswalker you control); damage to the chosen permanent
 * stays there; opponents and their permanents aren't covered; a chosen permanent that has left
 * redirects nothing; and with two Sacrifices you choose which chosen permanent is dealt the damage.
 */
class GideonsSacrificeScenarioTest : ScenarioTestBase() {

    init {
        fun TestGame.damageOn(id: EntityId) = state.getEntity(id)?.get<DamageComponent>()?.amount ?: 0
        fun TestGame.loyaltyOf(id: EntityId) =
            state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.LOYALTY) ?: 0

        fun builder(sacrifices: Int = 1) = scenario()
            .withPlayers("Gideon", "Opponent")
            .apply { repeat(sacrifices) { withCardInHand(1, "Gideon's Sacrifice") } }
            .withLandsOnBattlefield(1, "Plains", sacrifices)
            .withCardOnBattlefield(1, "Grizzly Bears")
            .withCardOnBattlefield(1, "Hill Giant")
            .withLandsOnBattlefield(2, "Mountain", 3)
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .apply { repeat(4) { withCardInLibrary(1, "Plains"); withCardInLibrary(2, "Mountain") } }

        fun TestGame.castAndChoose(chosen: EntityId) {
            castSpell(1, "Gideon's Sacrifice").error shouldBe null
            resolveStack()
            withClue("the permanent is chosen on resolution") {
                getPendingDecision().shouldBeInstanceOf<ChooseTargetsDecision>()
            }
            selectTargets(listOf(chosen)).error shouldBe null
            resolveStack()
        }

        test("damage that would be dealt to you is dealt to the chosen creature instead") {
            val game = builder().withCardInHand(2, "Shock").build()
            val giant = game.findPermanent("Hill Giant")!!
            game.castAndChoose(giant)

            game.passPriority()
            game.castSpellTargetingPlayer(2, "Shock", 1).error shouldBe null
            game.resolveStack()

            game.getLifeTotal(1) shouldBe 20
            game.damageOn(giant) shouldBe 2
        }

        test("damage to another permanent you control moves; damage to the chosen one stays") {
            val game = builder().withCardInHand(2, "Shock").withCardInHand(2, "Shock").build()
            val bears = game.findPermanent("Grizzly Bears")!!
            val giant = game.findPermanent("Hill Giant")!!
            game.castAndChoose(giant)

            game.passPriority()
            game.castSpell(2, "Shock", bears).error shouldBe null
            game.resolveStack()
            withClue("the Bears' Shock went to the Giant") {
                game.damageOn(bears) shouldBe 0
                game.damageOn(giant) shouldBe 2
            }

            game.passPriority()
            game.castSpell(2, "Shock", giant).error shouldBe null
            game.resolveStack()
            withClue("more damage than its toughness can be redirected onto it — the Giant dies") {
                game.findPermanent("Hill Giant") shouldBe null
                game.damageOn(bears) shouldBe 0
            }
        }

        test("the opponent and their permanents are not covered") {
            val game = builder()
                .withCardInHand(2, "Shock")
                .withCardInHand(2, "Shock")
                .withCardOnBattlefield(2, "Giant Spider")
                .build()
            val giant = game.findPermanent("Hill Giant")!!
            val spider = game.findPermanent("Giant Spider")!!
            game.castAndChoose(giant)

            game.passPriority()
            game.castSpellTargetingPlayer(2, "Shock", 2).error shouldBe null
            game.resolveStack()
            game.passPriority()
            game.castSpell(2, "Shock", spider).error shouldBe null
            game.resolveStack()

            game.getLifeTotal(2) shouldBe 18
            game.damageOn(spider) shouldBe 2
            game.damageOn(giant) shouldBe 0
        }

        test("a chosen creature that has left the battlefield redirects nothing") {
            val game = builder().withCardInHand(2, "Lightning Bolt").withCardInHand(2, "Shock").build()
            val giant = game.findPermanent("Hill Giant")!!
            game.castAndChoose(giant)

            game.passPriority()
            game.castSpell(2, "Lightning Bolt", giant).error shouldBe null
            game.resolveStack()
            withClue("Bolt aimed at the chosen creature stays on it") { game.findPermanent("Hill Giant") shouldBe null }

            game.passPriority()
            game.castSpellTargetingPlayer(2, "Shock", 1).error shouldBe null
            game.resolveStack()
            game.getLifeTotal(1) shouldBe 18
        }

        test("a chosen planeswalker loses loyalty for the damage dealt to your creatures") {
            val game = builder().withCardOnBattlefield(1, "Ajani, the Greathearted").withCardInHand(2, "Shock").build()
            val ajani = game.findPermanent("Ajani, the Greathearted")!!
            val bears = game.findPermanent("Grizzly Bears")!!
            val loyalty = game.loyaltyOf(ajani)
            game.castAndChoose(ajani)

            game.passPriority()
            game.castSpell(2, "Shock", bears).error shouldBe null
            game.resolveStack()

            game.damageOn(bears) shouldBe 0
            game.loyaltyOf(ajani) shouldBe loyalty - 2
        }

        test("combat damage to a planeswalker you control is dealt to the chosen creature instead") {
            val game = scenario()
                .withPlayers("Gideon", "Opponent")
                .withCardInHand(1, "Gideon's Sacrifice")
                .withLandsOnBattlefield(1, "Plains", 1)
                .withCardOnBattlefield(1, "Hill Giant")
                .withCardOnBattlefield(1, "Ajani, the Greathearted")
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withActivePlayer(2)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .apply { repeat(4) { withCardInLibrary(1, "Plains"); withCardInLibrary(2, "Mountain") } }
                .build()
            val giant = game.findPermanent("Hill Giant")!!
            val ajani = game.findPermanent("Ajani, the Greathearted")!!
            val loyalty = game.loyaltyOf(ajani)

            game.passPriority()
            game.castAndChoose(giant)

            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackersWithPermanentTargets(
                permanentAttackers = mapOf("Grizzly Bears" to "Ajani, the Greathearted")
            ).error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
            game.declareNoBlockers().error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.END_COMBAT)

            withClue("Ajani kept his loyalty") { game.loyaltyOf(ajani) shouldBe loyalty }
            withClue("the Giant was dealt the Bears' 2") { game.damageOn(giant) shouldBe 2 }
        }

        context("two Sacrifices that chose different permanents") {
            fun twoShields(): Triple<TestGame, EntityId, EntityId> {
                val game = builder(sacrifices = 2).withCardInHand(2, "Shock").build()
                val bears = game.findPermanent("Grizzly Bears")!!
                val giant = game.findPermanent("Hill Giant")!!
                game.castAndChoose(bears)
                game.castAndChoose(giant)
                game.passPriority()
                game.castSpellTargetingPlayer(2, "Shock", 1).error shouldBe null
                game.resolveStack()
                return Triple(game, bears, giant)
            }

            test("you choose which of them is dealt the damage — and only that one") {
                for (pickFirst in listOf(true, false)) {
                    val (game, bears, giant) = twoShields()
                    val decision = game.getPendingDecision().shouldBeInstanceOf<YesNoDecision>()
                    decision.playerId shouldBe game.player1Id
                    val firstOffered = decision.yesText
                    game.answerYesNo(pickFirst).error shouldBe null
                    game.resolveStack()

                    withClue("no further question, and the damage didn't bounce back and forth") {
                        game.hasPendingDecision() shouldBe false
                    }
                    game.getLifeTotal(1) shouldBe 20
                    val (dealt, spared) = if ((firstOffered == "Grizzly Bears") == pickFirst) bears to giant else giant to bears
                    withClue("picked ${if (pickFirst) firstOffered else decision.noText}") {
                        if (dealt == bears) {
                            withClue("2 damage on the 2/2 Bears killed them") { game.findPermanent("Grizzly Bears") shouldBe null }
                        } else {
                            game.damageOn(giant) shouldBe 2
                        }
                        game.damageOn(spared) shouldBe 0
                    }
                }
            }

            test("combat damage asks once per damage step — a double striker's second hit is a new choice") {
                val game = scenario()
                    .withPlayers("Gideon", "Opponent")
                    .withCardInHand(1, "Gideon's Sacrifice")
                    .withCardInHand(1, "Gideon's Sacrifice")
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(1, "Hill Giant")
                    .withCardOnBattlefield(2, "Boros Swiftblade")
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .apply { repeat(4) { withCardInLibrary(1, "Plains"); withCardInLibrary(2, "Mountain") } }
                    .build()
                val bears = game.findPermanent("Grizzly Bears")!!
                val giant = game.findPermanent("Hill Giant")!!
                game.passPriority()
                game.castAndChoose(bears)
                game.passPriority()
                game.castAndChoose(giant)

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Boros Swiftblade" to 1)).error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.declareNoBlockers().error shouldBe null

                // Pass priority until each damage step stops to ask; never through passUntilPhase,
                // which would answer the question itself.
                val answers = listOf(true, false)
                for (answer in answers) {
                    repeat(6) { if (!game.hasPendingDecision()) game.passPriority() }
                    val decision = game.getPendingDecision().shouldBeInstanceOf<YesNoDecision>()
                    decision.playerId shouldBe game.player1Id
                    game.answerYesNo(answer).error shouldBe null
                }
                game.hasPendingDecision() shouldBe false

                withClue("the player took none of it") { game.getLifeTotal(1) shouldBe 20 }
                withClue("asked once per strike and answered differently, each chosen permanent took 1") {
                    game.damageOn(bears) shouldBe 1
                    game.damageOn(giant) shouldBe 1
                }
            }
        }

        test("with no creature or planeswalker to choose, nothing happens") {
            val game = scenario()
                .withPlayers("Gideon", "Opponent")
                .withCardInHand(1, "Gideon's Sacrifice")
                .withLandsOnBattlefield(1, "Plains", 1)
                .withCardInHand(2, "Shock")
                .withLandsOnBattlefield(2, "Mountain", 1)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .apply { repeat(4) { withCardInLibrary(1, "Plains"); withCardInLibrary(2, "Mountain") } }
                .build()
            game.castSpell(1, "Gideon's Sacrifice").error shouldBe null
            game.resolveStack()
            game.hasPendingDecision() shouldBe false

            game.passPriority()
            game.castSpellTargetingPlayer(2, "Shock", 1).error shouldBe null
            game.resolveStack()
            game.getLifeTotal(1) shouldBe 18
            game.findPermanent("Plains").shouldNotBeNull()
        }
    }
}
