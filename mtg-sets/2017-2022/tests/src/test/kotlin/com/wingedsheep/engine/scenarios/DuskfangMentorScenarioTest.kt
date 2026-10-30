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
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Duskfang Mentor (IKO #86) — the enters trigger puts a lifelink counter on target non-Human
 * creature you control; the tap ability puts a +1/+1 counter on each creature you control with
 * lifelink, whether the keyword is printed or granted by a keyword counter.
 */
class DuskfangMentorScenarioTest : ScenarioTestBase() {

    private val abilityId = cardRegistry.getCard("Duskfang Mentor")!!.activatedAbilities.first().id

    private fun TestGame.counters(id: EntityId, type: CounterType): Int =
        state.getEntity(id)?.get<CountersComponent>()?.counters?.get(type) ?: 0

    init {
        test("enters trigger targets only non-Human creatures you control; the ability grows every lifelink creature") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Duskfang Mentor")
                .withCardOnBattlefield(1, "Duskfang Mentor")    // an older Mentor, ready to activate
                .withCardOnBattlefield(1, "Grizzly Bears")      // non-Human, no lifelink
                .withCardOnBattlefield(1, "Llanowar Elves")     // non-Human, no lifelink
                .withCardOnBattlefield(1, "Glory Seeker")       // Human — not a legal target
                .withCardOnBattlefield(1, "Child of Night")     // printed lifelink
                .withCardOnBattlefield(2, "Hill Giant")         // opponent's creature — not a legal target
                .withLandsOnBattlefield(1, "Swamp", 5)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val oldMentor = game.findPermanent("Duskfang Mentor")!!
            val bears = game.findPermanent("Grizzly Bears")!!
            val elves = game.findPermanent("Llanowar Elves")!!
            val seeker = game.findPermanent("Glory Seeker")!!
            val printed = game.findPermanent("Child of Night")!!
            val giant = game.findPermanent("Hill Giant")!!

            game.castSpell(1, "Duskfang Mentor").error shouldBe null
            game.resolveStack()

            val decision = game.getPendingDecision().shouldBeInstanceOf<ChooseTargetsDecision>()
            val legal = decision.legalTargets[0]!!
            withClue("non-Human creatures you control are legal targets") {
                legal shouldContainAll listOf(bears, elves, printed)
            }
            withClue("Humans (including the Mentors) and opponents' creatures are not") {
                legal shouldNotContain seeker
                legal shouldNotContain oldMentor
                legal shouldNotContain giant
            }
            game.selectTargets(listOf(bears))
            game.resolveStack()

            withClue("Grizzly Bears got a lifelink counter, which grants lifelink") {
                game.counters(bears, CounterType.LIFELINK) shouldBe 1
                game.state.projectedState.hasKeyword(bears, Keyword.LIFELINK) shouldBe true
            }

            game.execute(
                ActivateAbility(playerId = game.player1Id, sourceId = oldMentor, abilityId = abilityId)
            ).error shouldBe null
            game.resolveStack()

            withClue("creatures with lifelink (counter-granted or printed) each get a +1/+1 counter") {
                game.counters(bears, CounterType.PLUS_ONE_PLUS_ONE) shouldBe 1
                game.counters(printed, CounterType.PLUS_ONE_PLUS_ONE) shouldBe 1
            }
            withClue("creatures without lifelink get nothing") {
                game.counters(elves, CounterType.PLUS_ONE_PLUS_ONE) shouldBe 0
                game.counters(seeker, CounterType.PLUS_ONE_PLUS_ONE) shouldBe 0
                game.counters(oldMentor, CounterType.PLUS_ONE_PLUS_ONE) shouldBe 0
                game.counters(giant, CounterType.PLUS_ONE_PLUS_ONE) shouldBe 0
            }
        }
    }
}
