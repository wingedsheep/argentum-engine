package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.LinkedExileComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for The Darkness Crystal (FIN #96):
 *   {2}{B}{B} Legendary Artifact
 *   - Black spells you cast cost {1} less to cast.  (existing ModifySpellCost primitive)
 *   - If a nontoken creature an opponent controls would die, instead exile it and you gain 2 life.
 *   - {4}{B}{B}, {T}: Put target creature card exiled with The Darkness Crystal onto the
 *     battlefield tapped under your control with two additional +1/+1 counters on it.
 *
 * Exercises the new engine capabilities: RedirectZoneChangeWithEffect(linkToSource) + the GainLife
 * replacement rider, and the ExiledWithSource target predicate driving the linked-exile reanimation
 * (entering tapped, under your control, with two +1/+1 counters).
 */
class TheDarknessCrystalScenarioTest : ScenarioTestBase() {

    init {
        context("The Darkness Crystal") {

            test("an opponent's dying creature is exiled+linked, you gain 2 life, then it can be reanimated tapped with two +1/+1 counters") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "The Darkness Crystal", summoningSickness = false)
                    .withCardOnBattlefield(2, "Grizzly Bears") // opponent's 2/2
                    .withLandsOnBattlefield(1, "Mountain", 1)   // pays {R} for the Bolt
                    .withLandsOnBattlefield(1, "Swamp", 6)      // pays {4}{B}{B} for the reanimation
                    .withCardInHand(1, "Lightning Bolt")
                    .withLifeTotal(1, 20)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val crystal = game.findPermanent("The Darkness Crystal")!!
                val bears = game.findPermanent("Grizzly Bears")!!
                val opponentId = game.player2Id

                // Kill the opponent's 2/2 with Lightning Bolt (3 damage).
                game.castSpell(1, "Lightning Bolt", targetId = bears).error shouldBe null
                game.resolveStack()

                withClue("The dying creature is redirected out of the graveyard into exile") {
                    game.state.getGraveyard(opponentId) shouldNotContain bears
                    game.state.getExile(opponentId) shouldContain bears
                }
                withClue("It is linked to The Darkness Crystal so ability 3 can retrieve it") {
                    (game.state.getEntity(crystal)?.get<LinkedExileComponent>()?.exiledIds ?: emptyList()) shouldContain bears
                }
                withClue("You gain 2 life from the replacement rider (20 -> 22)") {
                    game.getLifeTotal(1) shouldBe 22
                }

                // Reanimate the exiled creature with the {4}{B}{B}, {T} ability.
                val abilityId = cardRegistry.getCard("The Darkness Crystal")!!.script.activatedAbilities[0].id
                val activate = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = crystal,
                        abilityId = abilityId,
                        targets = listOf(ChosenTarget.Card(bears, opponentId, Zone.EXILE)),
                    )
                )
                withClue("Activating the reanimation ability should succeed: ${activate.error}") {
                    activate.error shouldBe null
                }
                if (game.getPendingDecision() is SelectManaSourcesDecision) {
                    game.submitManaSourcesAutoPay()
                }
                game.resolveStack()

                withClue("The creature returns to the battlefield under your control") {
                    game.findPermanent("Grizzly Bears") shouldBe bears
                    game.state.getEntity(bears)?.get<ControllerComponent>()?.playerId shouldBe game.player1Id
                }
                withClue("It enters tapped") {
                    (game.state.getEntity(bears)?.has<TappedComponent>() ?: false) shouldBe true
                }
                withClue("It enters with two additional +1/+1 counters") {
                    game.state.getEntity(bears)?.get<CountersComponent>()
                        ?.getCount(CounterType.PLUS_ONE_PLUS_ONE) shouldBe 2
                }
            }

            test("your own creature dying is unaffected (only an opponent's nontoken creature is exiled)") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "The Darkness Crystal", summoningSickness = false)
                    .withCardOnBattlefield(1, "Grizzly Bears") // your own 2/2
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withCardInHand(1, "Lightning Bolt")
                    .withLifeTotal(1, 20)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val crystal = game.findPermanent("The Darkness Crystal")!!
                val mine = game.findPermanent("Grizzly Bears")!!

                game.castSpell(1, "Lightning Bolt", targetId = mine).error shouldBe null
                game.resolveStack()

                withClue("Your own creature dies to the graveyard normally, is not exiled or linked") {
                    game.state.getGraveyard(game.player1Id) shouldContain mine
                    (game.state.getEntity(crystal)?.get<LinkedExileComponent>()?.exiledIds ?: emptyList()) shouldNotContain mine
                }
                withClue("No life is gained for your own creature dying") {
                    game.getLifeTotal(1) shouldBe 20
                }
            }
        }
    }
}
