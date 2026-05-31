package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Scenario test for Krumar Initiate (TDM) — {1}{B} Human Cleric, 2/2.
 *
 * "{X}{B}, {T}, Pay X life: This creature endures X. Activate only as a sorcery."
 *
 * Exercises the new [com.wingedsheep.sdk.scripting.AbilityCost.PayXLife] cost — the X-linked life
 * payment must equal the X chosen for the `{X}{B}` mana cost — and that the endure amount reads
 * the same X. Both endure modes are checked (counters and Spirit token).
 */
class KrumarInitiateScenarioTest : ScenarioTestBase() {

    private val krumarAbilityId =
        cardRegistry.getCard("Krumar Initiate")!!.activatedAbilities.first().id

    init {
        context("Krumar Initiate endure-X activated ability") {

            test("endure X (counter mode) puts X +1/+1 counters on Krumar and pays X life") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Krumar Initiate", tapped = false, summoningSickness = false)
                    .withLandsOnBattlefield(1, "Swamp", 4) // pays {3}{B} → X up to 3
                    .withLifeTotal(1, 20)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val krumar = game.findPermanent("Krumar Initiate")!!

                // {X}{B} with X = 3 → spend {3}{B} (4 mana), pay 3 life, endure 3.
                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = krumar,
                        abilityId = krumarAbilityId,
                        xValue = 3
                    )
                )
                withClue("Activating endure-3 should succeed: ${result.error}") {
                    result.error shouldBe null
                }
                withClue("Pay X life consumed exactly X = 3 life") {
                    game.getLifeTotal(1) shouldBe 17
                }

                // Resolve the ability off the stack; endure pauses mid-resolution for the
                // choose-one (counters vs token). Pick counters (option 0).
                game.resolveStack()
                val decision = game.getPendingDecision()
                decision.shouldNotBeNull()
                decision.shouldBeInstanceOf<ChooseOptionDecision>()
                game.submitDecision(OptionChosenResponse(decision.id, 0))
                game.resolveStack()

                val counters = game.state.getEntity(krumar)?.get<CountersComponent>()
                withClue("Endure 3 in counter mode adds three +1/+1 counters") {
                    (counters?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0) shouldBe 3
                }
            }

            test("endure X (token mode) creates an X/X white Spirit token") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Krumar Initiate", tapped = false, summoningSickness = false)
                    .withLandsOnBattlefield(1, "Swamp", 3) // pays {2}{B} → X up to 2
                    .withLifeTotal(1, 20)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val krumar = game.findPermanent("Krumar Initiate")!!

                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = krumar,
                        abilityId = krumarAbilityId,
                        xValue = 2
                    )
                )
                withClue("Activating endure-2 should succeed: ${result.error}") {
                    result.error shouldBe null
                }
                withClue("Pay X life consumed exactly X = 2 life") {
                    game.getLifeTotal(1) shouldBe 18
                }

                // Resolve the ability; endure pauses for the choose-one. Choose token mode (option 1).
                game.resolveStack()
                val decision = game.getPendingDecision()
                decision.shouldNotBeNull()
                decision.shouldBeInstanceOf<ChooseOptionDecision>()
                game.submitDecision(OptionChosenResponse(decision.id, 1))
                game.resolveStack()

                val spirit = game.findPermanent("Spirit Token")
                withClue("Endure 2 in token mode creates a 2/2 white Spirit token") {
                    spirit.shouldNotBeNull()
                    val tokenCard = game.state.getEntity(spirit)!!.get<CardComponent>()!!
                    tokenCard.colors shouldBe setOf(Color.WHITE)
                    tokenCard.baseStats?.basePower shouldBe 2
                    tokenCard.baseStats?.baseToughness shouldBe 2
                }
            }
        }
    }
}
