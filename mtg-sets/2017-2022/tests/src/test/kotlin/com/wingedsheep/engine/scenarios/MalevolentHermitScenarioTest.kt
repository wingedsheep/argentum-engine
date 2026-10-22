package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.AlternativeCostType
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Malevolent Hermit // Benevolent Geist (MID) — the front sacrifices itself to counter a
 * noncreature spell unless {3} is paid; the disturbed back face makes its controller's
 * noncreature spells (and only those) uncounterable.
 */
class MalevolentHermitScenarioTest : ScenarioTestBase() {

    init {
        context("Malevolent Hermit") {

            test("sacrificing it counters a noncreature spell whose controller can't pay {3}") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Malevolent Hermit", summoningSickness = false)
                    .withLandsOnBattlefield(1, "Island", 1)
                    .withCardInHand(2, "Divination")
                    .withLandsOnBattlefield(2, "Island", 3)
                    .withCardInLibrary(1, "Island").withCardInLibrary(1, "Island")
                    .withCardInLibrary(2, "Island").withCardInLibrary(2, "Island")
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(2, "Divination").error shouldBe null
                game.passPriority().error shouldBe null

                val hermit = game.findPermanent("Malevolent Hermit")!!
                val divination = game.state.stack.single {
                    game.state.getEntity(it)?.get<CardComponent>()?.name == "Divination"
                }
                val abilityId = cardRegistry.getCard("Malevolent Hermit")!!.script.activatedAbilities[0].id
                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = hermit,
                        abilityId = abilityId,
                        targets = listOf(ChosenTarget.Spell(divination)),
                    )
                )
                withClue("error=${result.error}") { result.error shouldBe null }
                withClue("the Hermit is sacrificed as a cost") {
                    game.isInGraveyard(1, "Malevolent Hermit") shouldBe true
                }

                game.resolveStack()
                if (game.getPendingDecision() is YesNoDecision) game.answerYesNo(false).error shouldBe null
                game.resolveStack()

                game.isInGraveyard(2, "Divination") shouldBe true
                game.handSize(2) shouldBe 0
            }
        }

        context("Benevolent Geist") {

            fun disturbedGeist() = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInGraveyard(1, "Malevolent Hermit")
                .withLandsOnBattlefield(1, "Island", 3)
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withLandsOnBattlefield(1, "Forest", 2)
                .withCardInHand(1, "Lightning Bolt")
                .withCardInHand(1, "Grizzly Bears")
                .withCardInHand(2, "Counterspell")
                .withLandsOnBattlefield(2, "Island", 2)
                .withCardInLibrary(1, "Island").withCardInLibrary(1, "Island")
                .withCardInLibrary(2, "Island").withCardInLibrary(2, "Island")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
                .also { game ->
                    val hermit = game.findCardsInGraveyard(1, "Malevolent Hermit").single()
                    val cast = game.execute(
                        CastSpell(
                            game.player1Id, hermit,
                            useAlternativeCost = true,
                            alternativeCostType = AlternativeCostType.DISTURB,
                        )
                    )
                    withClue("disturb error=${cast.error}") { cast.error shouldBe null }
                    game.resolveStack()
                    game.findPermanent("Benevolent Geist") shouldNotBe null
                }

            test("its controller's noncreature spells can't be countered") {
                val game = disturbedGeist()

                game.castSpellTargetingPlayer(1, "Lightning Bolt", 2).error shouldBe null
                game.passPriority().error shouldBe null
                game.castSpellTargetingStackSpell(2, "Counterspell", "Lightning Bolt").error shouldBe null
                game.resolveStack()

                game.getLifeTotal(2) shouldBe 17
                game.isInGraveyard(2, "Counterspell") shouldBe true
            }

            test("its controller's creature spells can still be countered") {
                val game = disturbedGeist()

                game.castSpell(1, "Grizzly Bears").error shouldBe null
                game.passPriority().error shouldBe null
                game.castSpellTargetingStackSpell(2, "Counterspell", "Grizzly Bears").error shouldBe null
                game.resolveStack()

                game.isOnBattlefield("Grizzly Bears") shouldBe false
                game.isInGraveyard(1, "Grizzly Bears") shouldBe true
            }
        }
    }
}
