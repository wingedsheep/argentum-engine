package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Tackle Artist {3}{R} 4/3 Orc Sorcerer — Trample.
 *
 * "Opus — Whenever you cast an instant or sorcery spell, put a +1/+1 counter on this creature. If
 * five or more mana was spent to cast that spell, put two +1/+1 counters on this creature instead."
 *
 * The 5+ tier *replaces* the base via `insteadIfFiveOrMore`: one +1/+1 counter normally, two when
 * five or more mana was spent (NOT three — the bonus is "instead", not additive). Exercises both
 * sides of the 5-mana boundary; counters are permanent so they persist.
 */
class TackleArtistScenarioTest : ScenarioTestBase() {

    private val projector = StateProjector()

    private fun plusCounters(game: TestGame, id: EntityId): Int =
        game.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    init {
        context("Tackle Artist — Opus +1/+1 counters") {

            test("a cheap instant/sorcery puts one +1/+1 counter (4/3 → 5/4)") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Tackle Artist") // 4/3
                    .withCardInHand(1, "Lightning Bolt") // {R}, 1 mana
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val artist = game.findPermanent("Tackle Artist")!!
                val bears = game.findPermanent("Grizzly Bears")!!

                game.castSpell(1, "Lightning Bolt", targetId = bears).error shouldBe null
                game.resolveStack()

                withClue("1 mana spent → one +1/+1 counter") {
                    plusCounters(game, artist) shouldBe 1
                    projector.getProjectedPower(game.state, artist) shouldBe 5
                    projector.getProjectedToughness(game.state, artist) shouldBe 4
                }
            }

            test("a 5+ mana spell puts TWO +1/+1 counters instead (4/3 → 6/5, not 7/6)") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Tackle Artist") // 4/3
                    .withCardInHand(1, "Blaze") // {X}{R}
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Mountain", 5)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val artist = game.findPermanent("Tackle Artist")!!
                val bears = game.findPermanent("Grizzly Bears")!!

                // Blaze X=4 → {4}{R} → 5 mana spent (boundary).
                game.castXSpell(1, "Blaze", xValue = 4, targetId = bears).error shouldBe null
                game.resolveStack()

                withClue("5 mana spent → two +1/+1 counters instead (not one, not three)") {
                    plusCounters(game, artist) shouldBe 2
                    projector.getProjectedPower(game.state, artist) shouldBe 6
                    projector.getProjectedToughness(game.state, artist) shouldBe 5
                }
            }
        }
    }
}
