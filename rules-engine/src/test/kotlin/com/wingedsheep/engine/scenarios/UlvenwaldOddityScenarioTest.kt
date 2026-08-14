package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.vow.cards.UlvenwaldOddity
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Ulvenwald Oddity // Ulvenwald Behemoth (VOW #225).
 *
 *   Front — Ulvenwald Oddity (4/4) — Trample, haste. {5}{G}{G}: Transform this creature.
 *   Back  — Ulvenwald Behemoth (8/8) — Trample, haste. Other creatures you control get +1/+1 and
 *           have trample and haste.
 *
 * Exercises the {5}{G}{G} activated transform and the back face's anthem over *other* creatures you
 * control (a +1/+1 buff plus granted trample and haste, and that it does not pump the Behemoth
 * itself).
 */
class UlvenwaldOddityScenarioTest : ScenarioTestBase() {

    private val transformAbilityId = UlvenwaldOddity
        .activatedAbilities.first { !it.isManaAbility }.id

    init {
        context("Ulvenwald Oddity") {

            test("{5}{G}{G} transforms Oddity into Ulvenwald Behemoth") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Ulvenwald Oddity", summoningSickness = false)
                    .withLandsOnBattlefield(1, "Forest", 7)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val oddity = game.findPermanent("Ulvenwald Oddity")!!
                game.execute(
                    ActivateAbility(playerId = game.player1Id, sourceId = oddity, abilityId = transformAbilityId)
                ).error shouldBe null
                if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()
                game.resolveStack()

                withClue("Oddity flipped to its 8/8 back face, same entity") {
                    game.state.getEntity(oddity)!!.get<CardComponent>()!!.name shouldBe "Ulvenwald Behemoth"
                    game.state.projectedState.getPower(oddity) shouldBe 8
                    game.state.projectedState.getToughness(oddity) shouldBe 8
                }
            }

            test("the Behemoth's anthem pumps other creatures and grants them trample and haste") {
                // Start with the back face already in play plus a vanilla ally, so the anthem is
                // observable without paying the transform cost.
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Ulvenwald Behemoth", summoningSickness = false)
                    .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val behemoth = game.findPermanent("Ulvenwald Behemoth")!!
                val bears = game.findPermanent("Grizzly Bears")!!

                withClue("the other creature gets +1/+1 (2/2 -> 3/3)") {
                    game.state.projectedState.getPower(bears) shouldBe 3
                    game.state.projectedState.getToughness(bears) shouldBe 3
                }
                withClue("the other creature gains trample and haste") {
                    game.state.projectedState.hasKeyword(bears, Keyword.TRAMPLE) shouldBe true
                    game.state.projectedState.hasKeyword(bears, Keyword.HASTE) shouldBe true
                }
                withClue("the Behemoth does not pump itself (stays 8/8)") {
                    game.state.projectedState.getPower(behemoth) shouldBe 8
                    game.state.projectedState.getToughness(behemoth) shouldBe 8
                }
            }
        }
    }
}
