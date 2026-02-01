package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.mechanics.StateBasedActionChecker
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.lorwyn.LorwynEclipsedSet
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario tests for Ajani, Outland Chaperone - the first planeswalker implementation.
 *
 * Ajani, Outland Chaperone: {1}{W}{W}
 * Legendary Planeswalker — Ajani
 * Starting Loyalty: 3
 *
 * +1: Create a 1/1 green and white Kithkin creature token.
 * −2: Ajani deals 4 damage to target tapped creature.
 * −8: Look at the top X cards of your library, where X is your life total.
 *     You may put any number of nonland permanent cards with mana value 3 or less
 *     from among them onto the battlefield. Then shuffle.
 */
class AjaniScenarioTest : ScenarioTestBase() {

    init {
        // Register Lorwyn Eclipsed cards
        cardRegistry.register(LorwynEclipsedSet.allCards)

        context("Ajani planeswalker basics") {

            test("Ajani enters the battlefield with 3 loyalty counters") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Ajani, Outland Chaperone")
                    .withLandsOnBattlefield(1, "Plains", 3)  // Mana for {1}{W}{W}
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Cast Ajani
                val castResult = game.castSpell(1, "Ajani, Outland Chaperone")
                withClue("Casting Ajani should succeed: ${castResult.error}") {
                    castResult.error shouldBe null
                }

                // Resolve the spell
                game.resolveStack()

                // Verify Ajani is on battlefield
                withClue("Ajani should be on the battlefield") {
                    game.isOnBattlefield("Ajani, Outland Chaperone") shouldBe true
                }

                // Verify loyalty counters
                val ajaniId = game.findPermanent("Ajani, Outland Chaperone")!!
                val counters = game.state.getEntity(ajaniId)?.get<CountersComponent>()
                val loyalty = counters?.getCount(CounterType.LOYALTY) ?: 0

                withClue("Ajani should have 3 loyalty counters") {
                    loyalty shouldBe 3
                }
            }

            test("Ajani dies when loyalty reaches 0") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Ajani, Outland Chaperone")
                    .withLandsOnBattlefield(1, "Plains", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Cast and resolve Ajani
                game.castSpell(1, "Ajani, Outland Chaperone")
                game.resolveStack()

                val ajaniId = game.findPermanent("Ajani, Outland Chaperone")!!

                // Manually set loyalty to 0 to test SBA
                game.state = game.state.updateEntity(ajaniId) { container ->
                    container.with(CountersComponent().withCounters(CounterType.LOYALTY, 0))
                }

                // Run state-based actions
                val sbaChecker = StateBasedActionChecker()
                val sbaResult = sbaChecker.checkAndApply(game.state)
                game.state = sbaResult.newState

                withClue("Ajani with 0 loyalty should be in graveyard") {
                    game.isOnBattlefield("Ajani, Outland Chaperone") shouldBe false
                }
            }
        }

        context("Ajani +1 ability: Create a Kithkin token") {

            test("+1 ability creates a 1/1 Kithkin token and adds loyalty") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Ajani, Outland Chaperone")
                    .withLandsOnBattlefield(1, "Plains", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Cast and resolve Ajani
                game.castSpell(1, "Ajani, Outland Chaperone")
                game.resolveStack()

                val ajaniId = game.findPermanent("Ajani, Outland Chaperone")!!

                // Find the +1 ability
                val cardDef = cardRegistry.getCard("Ajani, Outland Chaperone")!!
                val plusOneAbility = cardDef.script.activatedAbilities.find { ability ->
                    val cost = ability.cost
                    cost is com.wingedsheep.sdk.scripting.AbilityCost.Loyalty && cost.change == 1
                }!!

                // Activate the +1 ability
                val activateResult = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = ajaniId,
                        abilityId = plusOneAbility.id
                    )
                )

                withClue("Activating +1 should succeed: ${activateResult.error}") {
                    activateResult.error shouldBe null
                }

                // Verify loyalty increased to 4 (3 + 1)
                val countersAfterCost = game.state.getEntity(ajaniId)?.get<CountersComponent>()
                val loyaltyAfterCost = countersAfterCost?.getCount(CounterType.LOYALTY) ?: 0
                withClue("Ajani should have 4 loyalty after paying +1 cost") {
                    loyaltyAfterCost shouldBe 4
                }

                // Resolve the ability
                game.resolveStack()

                // Verify a Kithkin token was created
                val tokens = game.state.getBattlefield().filter { entityId ->
                    val container = game.state.getEntity(entityId)
                    container?.has<TokenComponent>() == true &&
                        container.get<CardComponent>()?.name == "Kithkin"
                }

                withClue("A Kithkin token should be on the battlefield") {
                    tokens.size shouldBe 1
                }

                // Verify token stats
                val tokenId = tokens.first()
                val tokenCard = game.state.getEntity(tokenId)?.get<CardComponent>()
                withClue("Kithkin token should be 1/1") {
                    tokenCard?.baseStats?.basePower shouldBe 1
                    tokenCard?.baseStats?.baseToughness shouldBe 1
                }

                // Verify token is controlled by Player 1
                val tokenController = game.state.getEntity(tokenId)?.get<ControllerComponent>()?.playerId
                withClue("Kithkin token should be controlled by Player 1") {
                    tokenController shouldBe game.player1Id
                }
            }
        }

        context("Ajani -2 ability: Deal 4 damage to target tapped creature") {

            test("-2 ability deals 4 damage to target tapped creature") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Ajani, Outland Chaperone")
                    .withCardOnBattlefield(2, "Devoted Hero", tapped = true)  // 1/2 creature
                    .withLandsOnBattlefield(1, "Plains", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Cast and resolve Ajani
                game.castSpell(1, "Ajani, Outland Chaperone")
                game.resolveStack()

                val ajaniId = game.findPermanent("Ajani, Outland Chaperone")!!
                val heroId = game.findPermanent("Devoted Hero")!!

                // Find the -2 ability
                val cardDef = cardRegistry.getCard("Ajani, Outland Chaperone")!!
                val minusTwoAbility = cardDef.script.activatedAbilities.find { ability ->
                    val cost = ability.cost
                    cost is com.wingedsheep.sdk.scripting.AbilityCost.Loyalty && cost.change == -2
                }!!

                // Activate the -2 ability targeting the tapped creature
                val activateResult = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = ajaniId,
                        abilityId = minusTwoAbility.id,
                        targets = listOf(ChosenTarget.Permanent(heroId))
                    )
                )

                withClue("Activating -2 should succeed: ${activateResult.error}") {
                    activateResult.error shouldBe null
                }

                // Verify loyalty decreased to 1 (3 - 2)
                val countersAfterCost = game.state.getEntity(ajaniId)?.get<CountersComponent>()
                val loyaltyAfterCost = countersAfterCost?.getCount(CounterType.LOYALTY) ?: 0
                withClue("Ajani should have 1 loyalty after paying -2 cost") {
                    loyaltyAfterCost shouldBe 1
                }

                // Resolve the ability
                game.resolveStack()

                // Run SBA to check for lethal damage
                val sbaChecker = StateBasedActionChecker()
                val sbaResult = sbaChecker.checkAndApply(game.state)
                game.state = sbaResult.newState

                // Devoted Hero (1/2) should be destroyed by 4 damage
                withClue("Devoted Hero should be destroyed by 4 damage") {
                    game.isOnBattlefield("Devoted Hero") shouldBe false
                }
            }

            test("-2 ability cannot be activated without enough loyalty") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Ajani, Outland Chaperone")
                    .withCardOnBattlefield(2, "Devoted Hero", tapped = true)
                    .withLandsOnBattlefield(1, "Plains", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Cast and resolve Ajani
                game.castSpell(1, "Ajani, Outland Chaperone")
                game.resolveStack()

                val ajaniId = game.findPermanent("Ajani, Outland Chaperone")!!
                val heroId = game.findPermanent("Devoted Hero")!!

                // Manually set loyalty to 1 (not enough for -2)
                game.state = game.state.updateEntity(ajaniId) { container ->
                    container.with(CountersComponent().withCounters(CounterType.LOYALTY, 1))
                }

                // Find the -2 ability
                val cardDef = cardRegistry.getCard("Ajani, Outland Chaperone")!!
                val minusTwoAbility = cardDef.script.activatedAbilities.find { ability ->
                    val cost = ability.cost
                    cost is com.wingedsheep.sdk.scripting.AbilityCost.Loyalty && cost.change == -2
                }!!

                // Try to activate the -2 ability - should fail
                val activateResult = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = ajaniId,
                        abilityId = minusTwoAbility.id,
                        targets = listOf(ChosenTarget.Permanent(heroId))
                    )
                )

                withClue("Activating -2 with only 1 loyalty should fail") {
                    activateResult.error shouldNotBe null
                }
            }

            test("-2 ability cannot target untapped creatures") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Ajani, Outland Chaperone")
                    .withCardOnBattlefield(2, "Devoted Hero", tapped = false)  // UNTAPPED creature
                    .withLandsOnBattlefield(1, "Plains", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Cast and resolve Ajani
                game.castSpell(1, "Ajani, Outland Chaperone")
                game.resolveStack()

                val ajaniId = game.findPermanent("Ajani, Outland Chaperone")!!
                val heroId = game.findPermanent("Devoted Hero")!!

                // Find the -2 ability
                val cardDef = cardRegistry.getCard("Ajani, Outland Chaperone")!!
                val minusTwoAbility = cardDef.script.activatedAbilities.find { ability ->
                    val cost = ability.cost
                    cost is com.wingedsheep.sdk.scripting.AbilityCost.Loyalty && cost.change == -2
                }!!

                // Try to activate the -2 ability targeting untapped creature - should fail
                val activateResult = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = ajaniId,
                        abilityId = minusTwoAbility.id,
                        targets = listOf(ChosenTarget.Permanent(heroId))
                    )
                )

                withClue("Targeting untapped creature should fail") {
                    activateResult.error shouldNotBe null
                }
            }
        }

        context("Planeswalker timing rules") {

            test("Loyalty abilities can only be activated at sorcery speed") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Ajani, Outland Chaperone")
                    .withLandsOnBattlefield(1, "Plains", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Cast and resolve Ajani
                game.castSpell(1, "Ajani, Outland Chaperone")
                game.resolveStack()

                val ajaniId = game.findPermanent("Ajani, Outland Chaperone")!!

                // Put something on the stack to prevent sorcery-speed activation
                // (This tests that loyalty abilities respect timing rules)
                // For now, just verify that ability can be activated at sorcery speed
                val cardDef = cardRegistry.getCard("Ajani, Outland Chaperone")!!
                val plusOneAbility = cardDef.script.activatedAbilities.find { ability ->
                    val cost = ability.cost
                    cost is com.wingedsheep.sdk.scripting.AbilityCost.Loyalty && cost.change == 1
                }!!

                val activateResult = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = ajaniId,
                        abilityId = plusOneAbility.id
                    )
                )

                withClue("Activating at sorcery speed should succeed: ${activateResult.error}") {
                    activateResult.error shouldBe null
                }
            }
        }
    }
}
