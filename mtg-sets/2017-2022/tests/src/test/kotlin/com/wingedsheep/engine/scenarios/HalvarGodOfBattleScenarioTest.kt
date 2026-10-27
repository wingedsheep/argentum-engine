package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.TargetsResponse
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.khm.cards.HalvarGodOfBattle
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Halvar, God of Battle (front face).
 *
 * Enchanted or equipped creatures you control have double strike; at the beginning of each combat
 * Halvar may move an Aura or Equipment attached to a creature you control onto another creature you
 * control — here an Aura, which the shared attachment mover re-checks against its enchant ability.
 */
class HalvarGodOfBattleScenarioTest : ScenarioTestBase() {

    init {
        cardRegistry.register(HalvarGodOfBattle)

        fun board(): TestGame = scenario()
            .withPlayers("Player", "Opponent")
            .withCardOnBattlefield(1, "Halvar, God of Battle")
            .withCardOnBattlefield(1, "Grizzly Bears")
            .withCardOnBattlefield(1, "Hill Giant")
            .withCardAttachedTo(1, "Holy Strength", "Grizzly Bears")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()

        fun TestGame.reachCombatTrigger() {
            passUntilPhase(Phase.COMBAT, Step.BEGIN_COMBAT)
            val decision = getPendingDecision()
            decision.shouldBeInstanceOf<ChooseTargetsDecision>()
            submitDecision(
                TargetsResponse(
                    decision.id,
                    mapOf(0 to listOf(findPermanent("Holy Strength")!!), 1 to listOf(findPermanent("Hill Giant")!!)),
                )
            ).error shouldBe null
            resolveStack()
            getPendingDecision().shouldBeInstanceOf<YesNoDecision>()
        }

        context("Halvar, God of Battle") {
            test("only the enchanted creature has double strike") {
                val game = board()
                val projected = game.state.projectedState
                projected.hasKeyword(game.findPermanent("Grizzly Bears")!!, Keyword.DOUBLE_STRIKE) shouldBe true
                projected.hasKeyword(game.findPermanent("Hill Giant")!!, Keyword.DOUBLE_STRIKE) shouldBe false
                projected.hasKeyword(game.findPermanent("Halvar, God of Battle")!!, Keyword.DOUBLE_STRIKE) shouldBe false
            }

            test("the combat trigger moves the Aura, and double strike follows it") {
                val game = board()
                game.reachCombatTrigger()
                game.answerYesNo(true).error shouldBe null

                val giant = game.findPermanent("Hill Giant")!!
                val bears = game.findPermanent("Grizzly Bears")!!
                game.state.getEntity(game.findPermanent("Holy Strength")!!)!!
                    .get<AttachedToComponent>()?.targetId shouldBe giant
                withClue("Holy Strength's +1/+2 and Halvar's double strike now on Hill Giant") {
                    game.state.projectedState.getPower(giant) shouldBe 4
                    game.state.projectedState.hasKeyword(giant, Keyword.DOUBLE_STRIKE) shouldBe true
                    game.state.projectedState.hasKeyword(bears, Keyword.DOUBLE_STRIKE) shouldBe false
                }
            }

            test("declining the may leaves the Aura where it was") {
                val game = board()
                game.reachCombatTrigger()
                game.answerYesNo(false).error shouldBe null

                game.state.getEntity(game.findPermanent("Holy Strength")!!)!!
                    .get<AttachedToComponent>()?.targetId shouldBe game.findPermanent("Grizzly Bears")!!
            }
        }
    }
}
