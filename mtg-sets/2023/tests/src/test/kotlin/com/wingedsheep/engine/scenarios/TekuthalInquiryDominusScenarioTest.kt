package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.one.cards.TekuthalInquiryDominus
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import com.wingedsheep.sdk.scripting.DistributedCounterRemoval
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Tekuthal, Inquiry Dominus (ONE #71) — {2}{U}{U} 3/5 Legendary Creature — Phyrexian Horror.
 *
 *   Flying
 *   If you would proliferate, proliferate twice instead.
 *   {1}{U/P}{U/P}, Remove three counters from among other artifacts, creatures, and planeswalkers
 *   you control: Put an indestructible counter on Tekuthal.
 *
 * The proliferate doubling is a `RepeatKeywordAction` replacement; the cost's "other" is a
 * `notSourceItself()` filter, which only works because the ability's source reaches the cost's
 * filter context.
 */
class TekuthalInquiryDominusScenarioTest : ScenarioTestBase() {

    private val abilityId = TekuthalInquiryDominus.activatedAbilities.first().id

    private val spreading = card("Test Spreading") {
        manaCost = "{1}"
        typeLine = "Sorcery"
        oracleText = "Proliferate."
        spell { effect = Effects.Proliferate() }
    }

    private fun seed(game: TestGame, id: EntityId, type: CounterType, amount: Int) {
        game.state = game.state.updateEntity(id) { c ->
            c.with((c.get<CountersComponent>() ?: CountersComponent()).withAdded(type, amount))
        }
    }

    private fun counters(game: TestGame, id: EntityId, type: CounterType): Int =
        game.state.getEntity(id)?.get<CountersComponent>()?.getCount(type) ?: 0

    private fun activate(game: TestGame, tekuthal: EntityId, from: EntityId, type: CounterType) =
        game.execute(
            ActivateAbility(
                playerId = game.player1Id,
                sourceId = tekuthal,
                abilityId = abilityId,
                costPayment = AdditionalCostPayment(
                    distributedCounterRemovals = listOf(DistributedCounterRemoval(from, type.printed, 3))
                )
            )
        )

    init {
        cardRegistry.register(spreading)

        test("your proliferate happens twice") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Tekuthal, Inquiry Dominus")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardInHand(1, "Test Spreading")
                .withLandsOnBattlefield(1, "Island", 1)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val bears = game.findPermanent("Grizzly Bears")!!
            seed(game, bears, CounterType.PLUS_ONE_PLUS_ONE, 1)

            game.castSpell(1, "Test Spreading").error shouldBe null
            var prompts = 0
            var guard = 0
            while ((game.state.stack.isNotEmpty() || game.hasPendingDecision()) && guard++ < 20) {
                if (game.hasPendingDecision()) { game.selectCards(listOf(bears)); prompts++ } else game.resolveStack()
            }

            prompts shouldBe 2
            counters(game, bears, CounterType.PLUS_ONE_PLUS_ONE) shouldBe 3
        }

        test("removing three counters from another creature puts an indestructible counter on Tekuthal") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Tekuthal, Inquiry Dominus")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withLandsOnBattlefield(1, "Island", 3)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val tekuthal = game.findPermanent("Tekuthal, Inquiry Dominus")!!
            val bears = game.findPermanent("Grizzly Bears")!!
            seed(game, bears, CounterType.PLUS_ONE_PLUS_ONE, 3)

            val act = activate(game, tekuthal, bears, CounterType.PLUS_ONE_PLUS_ONE)
            withClue("activation: ${act.error}") { act.error shouldBe null }
            counters(game, bears, CounterType.PLUS_ONE_PLUS_ONE) shouldBe 0
            game.resolveStack()

            counters(game, tekuthal, CounterType.INDESTRUCTIBLE) shouldBe 1
        }

        test("counters on Tekuthal itself can't pay the cost") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Tekuthal, Inquiry Dominus")
                .withLandsOnBattlefield(1, "Island", 3)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val tekuthal = game.findPermanent("Tekuthal, Inquiry Dominus")!!
            seed(game, tekuthal, CounterType.PLUS_ONE_PLUS_ONE, 3)

            activate(game, tekuthal, tekuthal, CounterType.PLUS_ONE_PLUS_ONE).error shouldNotBe null
            counters(game, tekuthal, CounterType.PLUS_ONE_PLUS_ONE) shouldBe 3
            counters(game, tekuthal, CounterType.INDESTRUCTIBLE) shouldBe 0
        }
    }
}
