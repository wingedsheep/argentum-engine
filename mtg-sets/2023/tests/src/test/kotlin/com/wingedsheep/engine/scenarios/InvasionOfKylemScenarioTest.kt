package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.DeclareAttackers
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.SummoningSicknessComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Invasion of Kylem // Valor's Reach Tag Team.
 *
 * Front: when it enters, up to two target creatures each get +2/+0 and gain vigilance and haste
 * until end of turn. Back (a sorcery, cast when the Siege is defeated): create two 3/2 red and
 * white Warriors with "Whenever this token and at least one other creature token attack, put a
 * +1/+1 counter on this token."
 */
class InvasionOfKylemScenarioTest : ScenarioTestBase() {

    private fun TestGame.warriors(): List<EntityId> =
        state.getBattlefield().filter {
            state.getEntity(it)?.get<CardComponent>()?.typeLine?.subtypes?.any { s -> s.value == "Warrior" } == true
        }

    private fun TestGame.plusCounters(id: EntityId): Int =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    /** The tokens were made this turn: lift their summoning sickness, then attack the opponent. */
    private fun TestGame.attackWith(attackers: List<EntityId>) {
        attackers.forEach { id -> state = state.updateEntity(id) { it.without<SummoningSicknessComponent>() } }
        advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
        execute(DeclareAttackers(player1Id, attackers.associateWith { player2Id })).error shouldBe null
    }

    /** Bolt the Siege (defense 5) twice — the second removes the last counter — and cast the back face. */
    private fun TestGame.defeatAndCastBack() {
        checkStateBasedActions()
        repeat(2) {
            castSpell(1, "Lightning Bolt", findPermanent("Invasion of Kylem")).error shouldBe null
            resolveStack()
        }
        answerYesNo(true).error shouldBe null
        resolveStack()
    }

    private fun defeatedGame(extra: ScenarioBuilder.() -> ScenarioBuilder = { this }) = scenario()
        .withPlayers("Player", "Opponent")
        .withCardOnBattlefield(1, "Invasion of Kylem")
        .withCardInHand(1, "Lightning Bolt")
        .withCardInHand(1, "Lightning Bolt")
        .withLandsOnBattlefield(1, "Mountain", 2)
        .withCardInLibrary(1, "Island")
        .withCardInLibrary(2, "Island")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .extra()
        .build()

    init {
        context("front face — Invasion of Kylem") {
            test("up to two target creatures get +2/+0, vigilance and haste") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Invasion of Kylem")
                    .withLandsOnBattlefield(1, "Mountain", 2)
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(1, "Hill Giant")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Invasion of Kylem").error shouldBe null
                game.resolveStack()
                val bears = game.findPermanent("Grizzly Bears")!!
                val giant = game.findPermanent("Hill Giant")!!
                game.selectTargets(listOf(bears, giant)).error shouldBe null
                game.resolveStack()

                val projected = game.state.projectedState
                projected.getPower(bears) shouldBe 4
                projected.getPower(giant) shouldBe 5
                projected.getToughness(bears) shouldBe 2
                projected.hasKeyword(bears, Keyword.VIGILANCE) shouldBe true
                projected.hasKeyword(giant, Keyword.HASTE) shouldBe true
            }
        }

        context("back face — Valor's Reach Tag Team") {
            test("defeating the Siege casts the sorcery: two 3/2 Warriors, Siege to the graveyard") {
                val game = defeatedGame()
                game.defeatAndCastBack()

                val warriors = game.warriors()
                warriors.size shouldBe 2
                warriors.forEach {
                    game.state.projectedState.getPower(it) shouldBe 3
                    game.state.projectedState.getToughness(it) shouldBe 2
                }
                withClue("the sorcery resolved and the card went to the graveyard front face up") {
                    game.isInGraveyard(1, "Invasion of Kylem") shouldBe true
                    game.isOnBattlefield("Invasion of Kylem") shouldBe false
                }
            }

            test("two Warrior tokens attacking together each get a +1/+1 counter") {
                val game = defeatedGame()
                game.defeatAndCastBack()
                val (a, b) = game.warriors()

                game.attackWith(listOf(a, b))
                game.resolveStack()

                game.plusCounters(a) shouldBe 1
                game.plusCounters(b) shouldBe 1
            }

            test("a Warrior attacking beside a nontoken creature gets no counter") {
                val game = defeatedGame {
                    withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                }
                game.defeatAndCastBack()
                val (a, _) = game.warriors()
                val bears = game.findPermanent("Grizzly Bears")!!

                game.attackWith(listOf(a, bears))
                game.resolveStack()

                withClue("the other attacker must be a creature *token*") {
                    game.plusCounters(a) shouldBe 0
                }
            }
        }
    }
}
