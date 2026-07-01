package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.GrantActivatedAbility
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Honest Work — {U} Aura, enchant creature an opponent controls.
 *
 * Covers the new Layer-3 name override (SetName → "Humble Merchant", read via
 * ProjectedState.getName) composed with the existing becomes-a-1/1-X primitives
 * (TransformPermanent + SetBasePowerToughnessStatic + LoseAllAbilities +
 * GrantActivatedAbility), plus the ETB "tap + remove all counters" trigger.
 */
class HonestWorkScenarioTest : ScenarioTestBase() {

    private val stateProjector = StateProjector()

    init {
        context("Honest Work") {
            test("enchanted creature becomes a 1/1 Citizen named Humble Merchant with {T}: Add {C} and loses its abilities") {
                // Birds of Paradise: 0/1 Bird with Flying + "{T}: Add one mana of any color".
                // Attach Honest Work (controlled by Player1) directly so the ETB tap doesn't fire
                // and the granted {T} ability can be activated.
                val attached = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(2, "Birds of Paradise")
                    .withCardAttachedTo(1, "Honest Work", "Birds of Paradise")
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val host = attached.findPermanent("Birds of Paradise").shouldNotBeNull()
                val projected = stateProjector.project(attached.state)

                withClue("base power/toughness becomes 1/1") {
                    projected.getPower(host) shouldBe 1
                    projected.getToughness(host) shouldBe 1
                }
                withClue("name is overridden to Humble Merchant") {
                    projected.getName(host) shouldBe "Humble Merchant"
                }
                withClue("it is a Citizen and loses its other creature types") {
                    projected.hasSubtype(host, "Citizen") shouldBe true
                    projected.hasSubtype(host, "Bird") shouldBe false
                }
                withClue("it loses all abilities (Flying gone)") {
                    projected.hasKeyword(host, Keyword.FLYING) shouldBe false
                    projected.hasLostAllAbilities(host) shouldBe true
                }

                // Activate the granted "{T}: Add {C}" mana ability (Player2 controls the creature;
                // its own "{T}: Add any color" is suppressed by losing all abilities).
                val grantedAbilityId = cardRegistry.getCard("Honest Work")!!.script.staticAbilities
                    .filterIsInstance<GrantActivatedAbility>().first().ability.id
                val result = attached.execute(
                    ActivateAbility(
                        playerId = attached.player2Id,
                        sourceId = host,
                        abilityId = grantedAbilityId
                    )
                )
                withClue("activating the granted {T}: Add {C} mana ability should succeed: ${result.error}") {
                    result.error shouldBe null
                }
                withClue("activating {T}: Add {C} adds one colorless mana to Player2's pool") {
                    attached.state.getEntity(attached.player2Id)
                        ?.get<ManaPoolComponent>()?.colorless shouldBe 1
                }
            }

            test("ETB taps the enchanted creature and removes all counters from it") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withLandsOnBattlefield(1, "Island", 1)
                    .withCardInHand(1, "Honest Work")
                    .withCardOnBattlefield(2, "Birds of Paradise")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val birds = game.findPermanent("Birds of Paradise").shouldNotBeNull()

                // Put two +1/+1 counters on the creature before the Aura enters.
                game.state = game.state.updateEntity(birds) { container ->
                    container.with(CountersComponent(mapOf(CounterType.PLUS_ONE_PLUS_ONE to 2)))
                }
                withClue("counters are present before the Aura enters") {
                    game.state.getEntity(birds)?.get<CountersComponent>()
                        ?.getCount(CounterType.PLUS_ONE_PLUS_ONE) shouldBe 2
                }

                val castResult = game.castSpell(1, "Honest Work", birds)
                withClue("Honest Work should be cast successfully: ${castResult.error}") {
                    castResult.error shouldBe null
                }

                // Resolve the Aura spell and its ETB trigger.
                while (game.hasPendingDecision() || game.state.stack.isNotEmpty()) {
                    if (game.hasPendingDecision()) game.answerYesNo(true) else game.resolveStack()
                }

                // Assert the Tap half first so a failure localizes which half of the ETB broke.
                withClue("ETB taps the enchanted creature") {
                    game.state.getEntity(birds)!!.has<TappedComponent>() shouldBe true
                }
                withClue("ETB removes all counters from the enchanted creature") {
                    (game.state.getEntity(birds)?.get<CountersComponent>()
                        ?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0) shouldBe 0
                }
            }
        }
    }
}
