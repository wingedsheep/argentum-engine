package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.gtc.cards.ThespiansStage
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario test for Thespian's Stage (GTC #248) — Land.
 *
 * "{T}: Add {C}.
 *  {2}, {T}: This land becomes a copy of target land, except it has this ability."
 *
 * Focus: the battlefield-to-battlefield land copy with `affected = Self` and
 * `retainActivatingAbility` — the Stage takes the target's copiable values (name, subtypes), stays
 * tapped (ruling 2018-12-07: it doesn't untap on becoming a copy), and keeps its `{2}, {T}` ability
 * through the durable granted-ability record.
 */
class ThespiansStageScenarioTest : ScenarioTestBase() {

    init {
        val copyAbilityId = ThespiansStage.activatedAbilities[1].id

        context("Thespian's Stage") {

            test("becomes a copy of the opponent's Forest, stays tapped, and keeps this ability") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Thespian's Stage")
                    .withLandsOnBattlefield(1, "Island", 2)
                    .withCardOnBattlefield(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val stage = game.findPermanent("Thespian's Stage")!!
                val forest = game.findPermanent("Forest")!!

                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = stage,
                        abilityId = copyAbilityId,
                        targets = listOf(ChosenTarget.Permanent(forest))
                    )
                )
                withClue("activation should succeed: ${result.error}") { result.error shouldBe null }
                if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()
                game.resolveStack()

                val card = game.state.getEntity(stage)!!.get<CardComponent>()!!
                withClue("the Stage is now named Forest") { card.name shouldBe "Forest" }
                withClue("it has the copied land's subtype") {
                    game.state.projectedState.hasSubtype(stage, "Forest") shouldBe true
                }
                withClue("becoming a copy doesn't untap it") {
                    game.state.getEntity(stage)!!.has<TappedComponent>() shouldBe true
                }
                withClue("the {2}, {T} ability is retained through the copy") {
                    game.state.grantedActivatedAbilities.count {
                        it.entityId == stage && it.ability.id == copyAbilityId
                    } shouldBe 1
                }
                withClue("the target Forest is untouched") {
                    game.state.getEntity(forest)!!.get<CardComponent>()!!.name shouldBe "Forest"
                }
            }
        }
    }
}
