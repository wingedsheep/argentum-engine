package com.wingedsheep.ai.engine

import com.wingedsheep.ai.engine.knowledge.IntentCatalog
import com.wingedsheep.ai.puzzles.advanceToPriority
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.EngineServices
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.doubles.shouldBeGreaterThan
import io.kotest.matchers.doubles.shouldBeLessThan
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * A repeatable activation that grows its source **through the source's own trigger** — Inspired
 * Tethermage's "{6}: Empower Jace 2" puts loyalty counters on a Jace, and its "whenever you put one
 * or more loyalty counters on a planeswalker, put a +1/+1 counter on this creature" answers — is
 * growth [com.wingedsheep.ai.engine.evaluation.GrowthPotential] prices, not only an activation
 * whose own effect says "+1/+1 counter".
 *
 * The production game (2026-11-02, 7988f9c0 turn 19, reported by a player as the AI's "weird
 * hatred of the 4/4 Beast tokens with trample"): the AI's Infinite Coursework — tap enchanted
 * creature, it loses all abilities and doesn't untap — went on a 4/4 trample Beast token while an
 * Inspired Tethermage 5/4 stood beside it on eight lands. The Tethermage grew a counter a turn
 * from there and was a 7/6 two turns later; the Coursework on it would have stopped the growth too.
 */
class SelfFedGrowthAiTest : ScenarioTestBase() {

    private val growth by lazy { IntentCatalog.of(cardRegistry).growth!! }

    private fun TestGame.resolveOnce(cardName: String, controller: Int) {
        val effect = cardRegistry.getCard(cardName)!!.script.spellEffect!!
        val controllerId = if (controller == 1) player1Id else player2Id
        state = EngineServices(cardRegistry).effectExecutorRegistry
            .execute(state, effect, EffectContext(sourceId = null, controllerId = controllerId))
            .state
    }

    private fun TestGame.addPlusOneCounters(cardName: String, amount: Int) {
        val id = findPermanent(cardName)!!
        state = state.updateEntity(id) { container ->
            val counters = container.get<CountersComponent>() ?: CountersComponent()
            container.with(counters.withAdded(CounterType.PLUS_ONE_PLUS_ONE, amount))
        }
    }

    /** The 7988f9c0 turn-19 position, simplified to the three creatures the Coursework could take. */
    private fun board(opponentLands: Int, jaceOut: Boolean = false): TestGame {
        val game = scenario().withPlayers()
            .withTurnNumber(19)
            .withActivePlayer(1)
            .withLifeTotal(1, 13)
            .withLifeTotal(2, 18)
            .withLandsOnBattlefield(1, "Island", 7)
            .withCardInHand(1, "Infinite Coursework")
            .withLandsOnBattlefield(2, "Forest", opponentLands)
            .withCardOnBattlefield(2, "Inspired Tethermage", tapped = true)
            .withCardOnBattlefield(2, "Yoshimaru, Scrappy Stray")
            .withCardInLibrary(1, "Island")
            .withCardInLibrary(2, "Forest")
            .build()
            .advanceToPriority(1, Step.PRECOMBAT_MAIN)
        game.addPlusOneCounters("Inspired Tethermage", 2)
        game.resolveOnce("Bestial Incursion", controller = 2)
        if (jaceOut) game.empowerOnce()
        return game
    }

    /** Resolve the Tethermage's own Empower once, so a Jace token is already out (7988f9c0 turn 21). */
    private fun TestGame.empowerOnce() {
        val tethermage = findPermanent("Inspired Tethermage")!!
        val effect = cardRegistry.getCard("Inspired Tethermage")!!.script.activatedAbilities.single().effect
        state = EngineServices(cardRegistry).effectExecutorRegistry
            .execute(state, effect, EffectContext(sourceId = tethermage, controllerId = player2Id))
            .state
    }

    private fun TestGame.courseworkTarget(profile: AiProfile = AiProfile.LIVE): String {
        val action = AIPlayer.create(cardRegistry, player1Id, profile).chooseAction(state)
        val cast = action as? CastSpell ?: return "no cast: $action"
        val id = (cast.targets.single() as ChosenTarget.Permanent).entityId
        return state.getEntity(id)?.get<CardComponent>()?.name ?: "?"
    }

    private fun TestGame.tethermageGrowth(): Double = growth.value(
        state, state.projectedState, findPermanent("Inspired Tethermage")!!, "Inspired Tethermage",
    )

    init {
        test("the Tethermage's {6} is read as growth through its own loyalty-counter trigger") {
            val fed = board(opponentLands = 8).tethermageGrowth()
            val starved = board(opponentLands = 3).tethermageGrowth()
            withClue("eight lands: $fed; three lands: $starved") {
                // A counter on each of the next three turns at 0.75 / 0.5 / 0.25, 1.4 a counter,
                // plus the Jace token the first Empower makes.
                fed shouldBeGreaterThan 2.0
                starved shouldBeLessThan 0.5
            }
        }

        test("with a Jace already out, the {6} still grows the Tethermage") {
            val fed = board(opponentLands = 8, jaceOut = true).tethermageGrowth()
            withClue("eight lands, Jace out: $fed") {
                // Three counters at 0.75 / 0.5 / 0.25, 1.4 a counter.
                fed shouldBeGreaterThan 1.5
            }
        }

        test("with a Jace out, Infinite Coursework still takes the growing Tethermage over the Beast") {
            val game = board(opponentLands = 8, jaceOut = true)
            val without = game.courseworkTarget(AiProfile.LIVE_WITHOUT_GROWTH)
            val with = game.courseworkTarget()
            withClue("with growth read: $with; without: $without") {
                // The production misplay, still there without the reading.
                without shouldBe "Beast Token"
                with shouldBe "Inspired Tethermage"
            }
        }

        test("Infinite Coursework takes the growing Tethermage, not the 4/4 trample Beast") {
            val game = board(opponentLands = 8)
            val without = game.courseworkTarget(AiProfile.LIVE_WITHOUT_GROWTH)
            val with = game.courseworkTarget()
            withClue("with growth read: $with; without: $without") {
                without shouldNotBe "Inspired Tethermage"
                with shouldBe "Inspired Tethermage"
            }
        }
    }
}
