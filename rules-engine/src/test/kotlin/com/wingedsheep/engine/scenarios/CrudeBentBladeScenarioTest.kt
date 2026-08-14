package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe

/**
 * Crude Bent Blade (HOB #63) — {2}{B} Artifact — Equipment.
 *
 * "When this Equipment enters, target opponent sacrifices a creature of their choice.
 *  Equipped creature gets +2/+1.
 *  Equip {2}"
 *
 * The edict half is the opponent's *choice*, so the decision must belong to them and must offer
 * only their own creatures. The equip half has to move the +2/+1 onto the equipped creature.
 */
class CrudeBentBladeScenarioTest : ScenarioTestBase() {

    init {
        context("Crude Bent Blade") {

            test("its ETB makes the targeted opponent sacrifice a creature of their choice") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Crude Bent Blade")
                    .withLandsOnBattlefield(1, "Swamp", 3)
                    .withCardOnBattlefield(1, "Centaur Courser")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Savannah Lions")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Crude Bent Blade").error shouldBe null
                if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()
                game.resolveStack()

                // The ETB targets an opponent; with one opponent it may be auto-chosen.
                if (game.getPendingDecision() is com.wingedsheep.engine.core.ChooseTargetsDecision) {
                    game.selectTargets(listOf(game.player2Id)).error shouldBe null
                    game.resolveStack()
                }

                val decision = game.getPendingDecision()
                withClue("the sacrifice is the opponent's own choice") {
                    (decision is SelectCardsDecision) shouldBe true
                    decision!!.playerId shouldBe game.player2Id
                }
                val options = (decision as SelectCardsDecision).options
                withClue("only their creatures are offered — not the caster's") {
                    options.map { game.state.getEntity(it)?.get<CardComponent>()?.name }
                        .shouldContainExactlyInAnyOrder("Grizzly Bears", "Savannah Lions")
                }

                val sacrificed = options.first()
                game.selectCards(listOf(sacrificed)).error shouldBe null
                game.resolveStack()

                withClue("exactly one of the opponent's creatures went to their graveyard") {
                    game.state.getBattlefield(game.player2Id).contains(sacrificed) shouldBe false
                    game.graveyardSize(2) shouldBe 1
                }
                withClue("the caster's own creature is untouched") {
                    game.isOnBattlefield("Centaur Courser") shouldBe true
                }
            }

            test("equipping moves +2/+1 onto the equipped creature") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Crude Bent Blade")
                    .withCardOnBattlefield(1, "Centaur Courser")
                    .withLandsOnBattlefield(1, "Swamp", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val blade = game.findPermanent("Crude Bent Blade")!!
                val courser = game.findPermanent("Centaur Courser")!!
                val equip = cardRegistry.requireCard("Crude Bent Blade")
                    .activatedAbilities.single { it.isEquipAbility }.id

                game.state.projectedState.getPower(courser) shouldBe 3

                game.execute(
                    ActivateAbility(
                        playerId = game.player1Id, sourceId = blade, abilityId = equip,
                        targets = listOf(ChosenTarget.Permanent(courser))
                    )
                ).error shouldBe null
                if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()
                game.resolveStack()

                withClue("the Equipment attached") {
                    game.state.getEntity(blade)?.get<AttachedToComponent>()?.targetId shouldBe courser
                }
                withClue("+2/+1 — 3/3 becomes 5/4") {
                    game.state.projectedState.getPower(courser) shouldBe 5
                    game.state.projectedState.getToughness(courser) shouldBe 4
                }
            }
        }
    }
}
