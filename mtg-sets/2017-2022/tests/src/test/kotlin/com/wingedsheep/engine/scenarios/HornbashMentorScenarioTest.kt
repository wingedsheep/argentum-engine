package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Hornbash Mentor (IKO #159) — {2}{G} Creature — Human Warrior, 3/3.
 *
 *   "When this creature enters, put a trample counter on target non-Human creature you control.
 *    {2}{G}, {T}: Put a +1/+1 counter on each creature you control with trample."
 *
 * The ETB target excludes Humans (the Mentor itself included) and opponents' creatures; the trample
 * counter grants trample via projection, and the activated ability then counts that creature.
 */
class HornbashMentorScenarioTest : ScenarioTestBase() {

    private val pumpAbilityId =
        cardRegistry.getCard("Hornbash Mentor")!!.activatedAbilities.first().id

    private fun TestGame.counters(id: EntityId, type: CounterType): Int =
        state.getEntity(id)?.get<CountersComponent>()?.counters?.get(type) ?: 0

    init {
        test("the ETB trample counter goes only on a non-Human creature you control") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Hornbash Mentor")
                .withCardOnBattlefield(1, "Grizzly Bears")   // Bear — legal
                .withCardOnBattlefield(1, "Glory Seeker")    // Human Soldier — not legal
                .withCardOnBattlefield(2, "Centaur Courser") // opponent's — not legal
                .withLandsOnBattlefield(1, "Forest", 3)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bears = game.findPermanent("Grizzly Bears")!!

            game.castSpell(1, "Hornbash Mentor").error shouldBe null
            game.resolveStack()

            val targeting = game.getPendingDecision().shouldBeInstanceOf<ChooseTargetsDecision>()
            withClue("Only the non-Human creature you control is a legal target") {
                targeting.legalTargets[0]!! shouldContainExactlyInAnyOrder listOf(bears)
            }
            game.selectTargets(listOf(bears))
            game.resolveStack()

            withClue("Grizzly Bears gets a trample counter and has trample") {
                game.counters(bears, CounterType.TRAMPLE) shouldBe 1
                game.state.projectedState.hasKeyword(bears, Keyword.TRAMPLE) shouldBe true
            }
        }

        test("the activated ability puts a +1/+1 counter on each creature you control with trample") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Hornbash Mentor")
                .withCardOnBattlefield(1, "Colossal Dreadmaw")  // trample
                .withCardOnBattlefield(1, "Grizzly Bears")      // no trample
                .withCardOnBattlefield(2, "Colossal Dreadmaw")  // opponent's trampler
                .withLandsOnBattlefield(1, "Forest", 3)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val mentor = game.findPermanent("Hornbash Mentor")!!
            val dreadmaws = game.findAllPermanents("Colossal Dreadmaw")
            val myDreadmaw = dreadmaws.first { game.state.projectedState.getController(it) == game.player1Id }
            val theirDreadmaw = dreadmaws.first { it != myDreadmaw }
            val bears = game.findPermanent("Grizzly Bears")!!

            val activation = game.execute(
                ActivateAbility(playerId = game.player1Id, sourceId = mentor, abilityId = pumpAbilityId)
            )
            withClue("Activating should succeed: ${activation.error}") { activation.error shouldBe null }
            game.resolveStack()

            withClue("Only your creature with trample gets the counter") {
                game.counters(myDreadmaw, CounterType.PLUS_ONE_PLUS_ONE) shouldBe 1
                game.counters(bears, CounterType.PLUS_ONE_PLUS_ONE) shouldBe 0
                game.counters(mentor, CounterType.PLUS_ONE_PLUS_ONE) shouldBe 0
                game.counters(theirDreadmaw, CounterType.PLUS_ONE_PLUS_ONE) shouldBe 0
            }
        }
    }
}
