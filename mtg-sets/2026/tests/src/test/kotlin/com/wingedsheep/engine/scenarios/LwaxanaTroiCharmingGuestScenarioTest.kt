package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Lwaxana Troi, Charming Guest: "{2}, {T}: Choose another target creature you control. For as long
 * as Lwaxana Troi remains tapped, if a triggered ability of that creature triggers, that ability
 * triggers an additional time." The grant's engine rules (untap / leave ends it, one-way) are pinned
 * by `GrantedSourceTriggerDoublerTest`; this covers the printed card.
 */
class LwaxanaTroiCharmingGuestScenarioTest : ScenarioTestBase() {

    private val lwaxana = "Lwaxana Troi, Charming Guest"

    private fun TestGame.activate(target: EntityId) = execute(
        ActivateAbility(
            player1Id, findPermanent(lwaxana)!!,
            cardRegistry.getCard(lwaxana)!!.script.activatedAbilities.single().id,
            targets = listOf(ChosenTarget.Permanent(target)),
        )
    )

    private fun TestGame.plusOneCounters(name: String): Int =
        state.getEntity(findPermanent(name)!!)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    private fun board() = scenario()
        .withPlayers("Player1", "Player2")
        .withCardOnBattlefield(1, lwaxana)
        .withCardOnBattlefield(1, "Ajani's Pridemate")
        .withCardInHand(1, "Angel's Mercy")
        .withLandsOnBattlefield(1, "Mountain", 2)
        .withLandsOnBattlefield(1, "Plains", 4)
        .withCardInLibrary(1, "Plains")
        .withCardInLibrary(2, "Plains")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)

    init {
        test("the chosen creature's triggered ability triggers an additional time while she stays tapped") {
            val game = board().build()

            game.activate(game.findPermanent("Ajani's Pridemate")!!).error shouldBe null
            game.resolveStack()

            game.castSpell(1, "Angel's Mercy").error shouldBe null
            repeat(5) { if (game.state.stack.isNotEmpty()) game.resolveStack() }

            game.getLifeTotal(1) shouldBe 27
            withClue("Pridemate's 'whenever you gain life' fires twice") {
                game.plusOneCounters("Ajani's Pridemate") shouldBe 2
            }
        }

        test("she can't choose herself") {
            val game = board().build()
            game.activate(game.findPermanent(lwaxana)!!).error shouldNotBe null
        }
    }
}
