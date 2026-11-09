package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.TransformEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Ulrich of the Krallenhorde // Ulrich, Uncontested Alpha (EMN #191).
 *
 *   Front (4/4) — "Whenever this creature enters or transforms into Ulrich of the Krallenhorde,
 *                  target creature gets +4/+4 until end of turn."
 *   Back  (6/6) — "Whenever this creature transforms into Ulrich, Uncontested Alpha, you may have it
 *                  fight target non-Werewolf creature you don't control."
 *
 * Covers the enters pump, the fight trigger's target filter (no Werewolves, nothing you control),
 * the on-resolution "you may", and the back-to-front transform re-firing the pump.
 */
class UlrichOfTheKrallenhordeScenarioTest : ScenarioTestBase() {

    init {
        cardRegistry.register(card("Test Moonmist") {
            manaCost = "{0}"
            typeLine = "Instant"
            spell {
                val creature = target(TargetFilter.Creature)
                effect = TransformEffect(creature)
            }
        })

        fun ScenarioTestBase.TestGame.transform(ulrich: EntityId, into: String) {
            castSpell(1, "Test Moonmist", targetId = ulrich).error shouldBe null
            if (getPendingDecision() is SelectManaSourcesDecision) submitManaSourcesAutoPay()
            resolveStack()
            state.getEntity(ulrich)!!.get<CardComponent>()!!.name shouldBe into
        }

        fun boardGame() = scenario()
            .withPlayers("Player1", "Player2")
            .withCardOnBattlefield(1, "Ulrich of the Krallenhorde", summoningSickness = false)
            .withCardOnBattlefield(1, "Hill Giant")
            .withCardOnBattlefield(2, "Grizzly Bears")
            .withCardOnBattlefield(2, "Smoldering Werewolf")
            .withCardInHand(1, "Test Moonmist")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)

        fun ScenarioTestBase.TestGame.damageOn(id: EntityId): Int =
            state.getEntity(id)?.get<DamageComponent>()?.amount ?: 0

        context("Ulrich of the Krallenhorde") {

            test("entering gives target creature +4/+4 until end of turn") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Ulrich of the Krallenhorde")
                    .withLandsOnBattlefield(1, "Mountain", 3)
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withCardOnBattlefield(1, "Hill Giant")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Ulrich of the Krallenhorde").error shouldBe null
                if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()
                game.resolveStack()

                val giant = game.findPermanent("Hill Giant")!!
                game.getPendingDecision().shouldBeInstanceOf<ChooseTargetsDecision>()
                game.selectTargets(listOf(giant)).error shouldBe null
                game.resolveStack()

                game.state.projectedState.getPower(giant) shouldBe 7
                game.state.projectedState.getToughness(giant) shouldBe 7
            }

            test("transforming offers only non-Werewolf creatures you don't control, and the fight resolves") {
                val game = boardGame().build()
                val ulrich = game.findPermanent("Ulrich of the Krallenhorde")!!
                game.transform(ulrich, "Ulrich, Uncontested Alpha")

                val bears = game.findPermanent("Grizzly Bears")!!
                val choice = game.getPendingDecision().shouldBeInstanceOf<ChooseTargetsDecision>()
                withClue("own Hill Giant and the opposing Werewolf are not legal") {
                    choice.legalTargets[0]!! shouldContainExactlyInAnyOrder listOf(bears)
                }
                game.selectTargets(listOf(bears)).error shouldBe null
                game.resolveStack()

                game.getPendingDecision().shouldBeInstanceOf<YesNoDecision>()
                game.answerYesNo(true).error shouldBe null
                game.resolveStack()

                game.isInGraveyard(2, "Grizzly Bears") shouldBe true
                game.damageOn(ulrich) shouldBe 2
            }

            test("declining the fight deals no damage") {
                val game = boardGame().build()
                val ulrich = game.findPermanent("Ulrich of the Krallenhorde")!!
                game.transform(ulrich, "Ulrich, Uncontested Alpha")

                val bears = game.findPermanent("Grizzly Bears")!!
                game.selectTargets(listOf(bears)).error shouldBe null
                game.resolveStack()
                game.getPendingDecision().shouldBeInstanceOf<YesNoDecision>()
                game.answerYesNo(false).error shouldBe null
                game.resolveStack()

                game.isOnBattlefield("Grizzly Bears") shouldBe true
                game.damageOn(bears) shouldBe 0
                game.damageOn(ulrich) shouldBe 0
            }

            test("transforming back into Ulrich of the Krallenhorde gives target creature +4/+4") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Ulrich of the Krallenhorde", summoningSickness = false)
                    .withCardOnBattlefield(1, "Hill Giant")
                    .withCardInHand(1, "Test Moonmist")
                    .withCardInHand(1, "Test Moonmist")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val ulrich = game.findPermanent("Ulrich of the Krallenhorde")!!

                // No creature the fight could target, so the back face's trigger never goes on the stack.
                game.transform(ulrich, "Ulrich, Uncontested Alpha")
                game.hasPendingDecision() shouldBe false

                game.transform(ulrich, "Ulrich of the Krallenhorde")
                val giant = game.findPermanent("Hill Giant")!!
                val choice = game.getPendingDecision().shouldBeInstanceOf<ChooseTargetsDecision>()
                choice.legalTargets[0]!! shouldContainExactlyInAnyOrder listOf(ulrich, giant)
                game.selectTargets(listOf(giant)).error shouldBe null
                game.resolveStack()

                game.state.projectedState.getPower(giant) shouldBe 7
                game.state.projectedState.getToughness(giant) shouldBe 7
            }
        }
        context("Ulrich's upkeep transform clauses") {

            /** Ulrich, Uncontested Alpha in player 1's end step, with [spells] cast so far this turn. */
            fun alphaAtEndOfTurn(spells: Map<Int, Int>): ScenarioTestBase.TestGame {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Ulrich, Uncontested Alpha")
                    .withActivePlayer(1)
                    .inPhase(Phase.ENDING, Step.END)
                    .build()
                game.state = game.state.copy(
                    playerSpellsCastThisTurn = spells.mapKeys { (n, _) -> if (n == 1) game.player1Id else game.player2Id }
                )
                return game
            }

            test("one spell from each player last turn doesn't transform Ulrich back") {
                val game = alphaAtEndOfTurn(mapOf(1 to 1, 2 to 1))
                val ulrich = game.findPermanent("Ulrich, Uncontested Alpha")!!
                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
                game.resolveStack()
                withClue("two spells in total, but no single player cast two or more") {
                    game.state.getEntity(ulrich)!!.get<CardComponent>()!!.name shouldBe "Ulrich, Uncontested Alpha"
                }
            }

            test("two spells by the opponent last turn transform Ulrich back") {
                val game = alphaAtEndOfTurn(mapOf(2 to 2))
                val ulrich = game.findPermanent("Ulrich, Uncontested Alpha")!!
                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
                game.resolveStack()
                // Transforming into the front face fires its +4/+4 trigger; Ulrich is the only creature.
                if (game.getPendingDecision() is ChooseTargetsDecision) {
                    game.selectTargets(listOf(ulrich)).error shouldBe null
                    game.resolveStack()
                }
                game.state.getEntity(ulrich)!!.get<CardComponent>()!!.name shouldBe "Ulrich of the Krallenhorde"
            }
        }
    }
}
