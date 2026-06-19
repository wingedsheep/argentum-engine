package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Berta, Wise Extrapolator (Secrets of Strixhaven #175).
 *
 * Berta, Wise Extrapolator ({2}{G}{U}, 1/4, Legendary Frog Druid):
 *   Increment (Whenever you cast a spell, if the amount of mana you spent is greater than this
 *     creature's power or toughness, put a +1/+1 counter on this creature.)
 *   Whenever one or more +1/+1 counters are put on Berta, add one mana of any color.
 *   {X}, {T}: Create a 0/0 green and blue Fractal creature token and put X +1/+1 counters on it.
 *
 * Exercises the Increment keyword combined with the CountersPlaced self-trigger that adds mana,
 * plus the {X},{T} Fractal-token activated ability that distributes X +1/+1 counters.
 */
class BertaWiseExtrapolatorScenarioTest : ScenarioTestBase() {

    private val fractalAbilityId =
        cardRegistry.getCard("Berta, Wise Extrapolator")!!.activatedAbilities.first().id

    private fun plusOneCounters(game: TestGame, id: EntityId): Int =
        game.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    init {
        context("Berta, Wise Extrapolator — Increment feeds the counters-placed mana trigger") {

            test("casting a spell with mana spent > P/T increments Berta and triggers mana") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Berta, Wise Extrapolator")
                    .withCardInHand(1, "Stoke the Flames") // {2}{R}{R} = 4 mana spent > toughness 4? no, > power 1 yes
                    .withLandsOnBattlefield(1, "Mountain", 4)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val berta = game.findPermanent("Berta, Wise Extrapolator")!!

                // Cast Stoke the Flames paying 4 mana (> Berta's power 1) targeting the opponent.
                val cast = game.castSpellTargetingPlayer(1, "Stoke the Flames", 2)
                withClue("Casting Stoke the Flames should succeed: ${cast.error}") {
                    cast.error shouldBe null
                }
                // Increment trigger: 4 mana > power 1 -> +1/+1 counter. That counter placement
                // in turn triggers Berta's "add one mana of any color".
                game.resolveStack()

                withClue("Increment should have placed one +1/+1 counter (4 mana > power 1)") {
                    plusOneCounters(game, berta) shouldBe 1
                }
            }
        }

        context("Berta, Wise Extrapolator — {X},{T} Fractal token") {

            test("activating with X=2 makes a 0/0 Fractal with two +1/+1 counters") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Berta, Wise Extrapolator")
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val berta = game.findPermanent("Berta, Wise Extrapolator")!!

                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = berta,
                        abilityId = fractalAbilityId,
                        xValue = 2,
                    )
                )
                withClue("Activating Berta's {X},{T} ability should succeed: ${result.error}") {
                    result.error shouldBe null
                }
                game.resolveStack()

                val fractal = game.findPermanent("Fractal Token")
                withClue("A Fractal token should have been created") {
                    (fractal != null) shouldBe true
                }
                withClue("The Fractal token has X=2 +1/+1 counters (0/0 base)") {
                    plusOneCounters(game, fractal!!) shouldBe 2
                }
            }
        }
    }
}
