package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.leg.cards.GiantSlug
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe

/**
 * Giant Slug — "{5}: At the beginning of your next upkeep, choose a basic land type. This creature
 * gains landwalk of the chosen type until the end of that turn."
 *
 * The interesting parts are the timing — nothing happens now, nor on the opponent's upkeep — and
 * that the land type is chosen only as the delayed trigger resolves, with the landwalk lasting
 * through that whole turn and no further.
 */
class GiantSlugScenarioTest : FunSpec({

    val abilityId = GiantSlug.activatedAbilities.first().id
    val landwalks = listOf(
        Keyword.PLAINSWALK, Keyword.ISLANDWALK, Keyword.SWAMPWALK, Keyword.MOUNTAINWALK, Keyword.FORESTWALK,
    )

    fun GameTestDriver.landwalksOf(slug: EntityId): List<Keyword> =
        landwalks.filter { state.projectedState.hasKeyword(slug, it) }

    test("the chosen landwalk arrives at your next upkeep and lasts until the end of that turn") {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + listOf(GiantSlug))
        d.initMirrorMatch(deck = Deck.of("Swamp" to 40), startingLife = 20)
        val me = d.activePlayer!!
        val opp = d.getOpponent(me)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val slug = d.putCreatureOnBattlefield(me, "Giant Slug")
        d.giveColorlessMana(me, 5)
        d.submit(ActivateAbility(playerId = me, sourceId = slug, abilityId = abilityId))
            .outcome shouldBe Outcome.Done
        d.bothPass()

        withClue("nothing is granted while the delayed trigger waits") {
            d.landwalksOf(slug) shouldContainExactly emptyList()
        }

        d.passPriorityUntil(Step.UPKEEP, opp)
        withClue("the opponent's upkeep is not 'your next upkeep'") {
            d.stackSize shouldBe 0
            d.pendingDecision shouldBe null
            d.landwalksOf(slug) shouldContainExactly emptyList()
        }

        d.passPriorityUntil(Step.UPKEEP, me)
        var options: List<String>? = null
        var guard = 0
        while ((d.stackSize > 0 || d.pendingDecision != null) && guard++ < 20) {
            val decision = d.pendingDecision
            when {
                decision is ChooseOptionDecision -> {
                    decision.playerId shouldBe me
                    options = decision.options
                    d.submitDecision(me, OptionChosenResponse(decision.id, decision.options.indexOf("Forest")))
                        .error shouldBe null
                }
                decision != null -> d.autoResolveDecision()
                else -> d.bothPass()
            }
        }

        withClue("the type is chosen from the five basic land types as the trigger resolves") {
            options shouldBe listOf("Plains", "Island", "Swamp", "Mountain", "Forest")
        }
        d.landwalksOf(slug) shouldContainExactly listOf(Keyword.FORESTWALK)

        d.passPriorityUntil(Step.POSTCOMBAT_MAIN)
        withClue("still forestwalking later that turn") {
            d.landwalksOf(slug) shouldContainExactly listOf(Keyword.FORESTWALK)
        }

        d.passPriorityUntil(Step.UPKEEP, opp)
        withClue("the landwalk ended with that turn, and the trigger fired only once") {
            d.landwalksOf(slug) shouldContainExactly emptyList()
            d.stackSize shouldBe 0
        }
    }
})
