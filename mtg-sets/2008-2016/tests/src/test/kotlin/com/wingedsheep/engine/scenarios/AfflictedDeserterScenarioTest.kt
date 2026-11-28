package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.scripting.effects.TransformEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Afflicted Deserter // Werewolf Ransacker (DKA #81).
 *
 * Ransacker: "Whenever this creature transforms into Werewolf Ransacker, you may destroy target
 * artifact. If that artifact is put into a graveyard this way, this creature deals 3 damage to
 * that artifact's controller."
 *
 * Covers the destroy-and-damage path, declining the "may", and an indestructible artifact that
 * survives (so no damage is dealt).
 */
class AfflictedDeserterScenarioTest : ScenarioTestBase() {

    init {
        cardRegistry.register(card("Test Moonmist") {
            manaCost = "{0}"
            typeLine = "Instant"
            spell {
                val creature = target(TargetFilter.Creature)
                effect = TransformEffect(creature)
            }
        })

        fun gameWith(artifact: String) = scenario()
            .withPlayers("Player1", "Player2")
            .withCardOnBattlefield(1, "Afflicted Deserter", summoningSickness = false)
            .withCardOnBattlefield(2, artifact)
            .withCardInHand(1, "Test Moonmist")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()

        fun ScenarioTestBase.TestGame.transformAndTarget(artifact: String) {
            val deserter = findPermanent("Afflicted Deserter")!!
            castSpell(1, "Test Moonmist", targetId = deserter).error shouldBe null
            if (getPendingDecision() is SelectManaSourcesDecision) submitManaSourcesAutoPay()
            resolveStack()
            state.getEntity(deserter)!!.get<CardComponent>()!!.name shouldBe "Werewolf Ransacker"

            val target = findPermanent(artifact)!!
            val decision = getPendingDecision().shouldBeInstanceOf<ChooseTargetsDecision>()
            decision.legalTargets[0]!! shouldContainExactlyInAnyOrder listOf(target)
            selectTargets(listOf(target)).error shouldBe null
            resolveStack()
            getPendingDecision().shouldBeInstanceOf<YesNoDecision>()
        }

        context("Werewolf Ransacker transform trigger") {

            test("destroying the artifact deals 3 damage to its controller") {
                val game = gameWith("Ornithopter")
                game.transformAndTarget("Ornithopter")
                game.answerYesNo(true).error shouldBe null
                game.resolveStack()

                game.isInGraveyard(2, "Ornithopter") shouldBe true
                game.getLifeTotal(2) shouldBe 17
                game.getLifeTotal(1) shouldBe 20
            }

            test("declining the may leaves the artifact and deals no damage") {
                val game = gameWith("Ornithopter")
                game.transformAndTarget("Ornithopter")
                game.answerYesNo(false).error shouldBe null
                game.resolveStack()

                game.isOnBattlefield("Ornithopter") shouldBe true
                game.getLifeTotal(2) shouldBe 20
            }

            test("an indestructible artifact isn't put into a graveyard, so no damage is dealt") {
                val game = gameWith("Darksteel Ingot")
                game.transformAndTarget("Darksteel Ingot")
                game.answerYesNo(true).error shouldBe null
                game.resolveStack()

                withClue("Darksteel Ingot survives and its controller takes no damage") {
                    game.isOnBattlefield("Darksteel Ingot") shouldBe true
                    game.getLifeTotal(2) shouldBe 20
                }
            }
        }
    }
}
