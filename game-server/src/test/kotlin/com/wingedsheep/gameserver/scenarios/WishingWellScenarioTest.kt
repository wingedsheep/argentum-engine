package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Wishing Well.
 *
 * Card reference:
 * - Wishing Well {3}{U} — Artifact
 *   "{T}: Put a coin counter on this artifact. When you do, you may cast target instant or
 *   sorcery card with mana value equal to the number of coin counters on this artifact from
 *   your graveyard without paying its mana cost. If that spell would be put into your
 *   graveyard, exile it instead. Activate only as a sorcery."
 */
class WishingWellScenarioTest : ScenarioTestBase() {

    init {
        context("Wishing Well") {

            test("activating with no matching spell in graveyard adds a coin counter") {
                val game = scenario()
                    .withPlayers("Player 1", "Player 2")
                    .withCardOnBattlefield(1, "Wishing Well")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val well = game.findPermanent("Wishing Well")!!
                val ability = cardRegistry.getCard("Wishing Well")!!.script.activatedAbilities[0]

                game.execute(ActivateAbility(game.player1Id, well, ability.id, emptyList()))
                game.resolveStack()

                // Empty graveyard → SelectFromCollection auto-resolves with no decision
                game.hasPendingDecision() shouldBe false
                val coins = game.state.getEntity(well)
                    ?.get<CountersComponent>()?.getCount(CounterType.COIN) ?: 0
                coins shouldBe 1
            }

            test("activating exiles a graveyard MV-1 instant matching the coin counter count") {
                val game = scenario()
                    .withPlayers("Player 1", "Player 2")
                    .withCardOnBattlefield(1, "Wishing Well")
                    .withCardInGraveyard(1, "Scorching Spear")     // MV 1 (matches)
                    .withCardInGraveyard(1, "Volcanic Hammer")    // MV 2 (does not match)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val well = game.findPermanent("Wishing Well")!!
                val ability = cardRegistry.getCard("Wishing Well")!!.script.activatedAbilities[0]

                game.execute(ActivateAbility(game.player1Id, well, ability.id, emptyList()))
                game.resolveStack()

                // Should pause for SelectFromCollection on the matching spell
                withClue("Expected SelectCardsDecision for the matching spell") {
                    game.hasPendingDecision() shouldBe true
                }
                val boltId = game.findCardsInGraveyard(1, "Scorching Spear").single()
                game.selectCards(listOf(boltId))
                game.resolveStack()

                // Bolt now exiled; Volcanic Hammer remains in graveyard
                val exile = game.state.getExile(game.player1Id)
                val boltExiled = exile.any {
                    game.state.getEntity(it)?.get<CardComponent>()?.name == "Scorching Spear"
                }
                boltExiled shouldBe true
                game.isInGraveyard(1, "Scorching Spear") shouldBe false
                game.isInGraveyard(1, "Volcanic Hammer") shouldBe true

                // Coin counter was added (and the filter saw the new value of 1)
                val coins = game.state.getEntity(well)
                    ?.get<CountersComponent>()?.getCount(CounterType.COIN) ?: 0
                coins shouldBe 1
            }

            test("declining the optional cast leaves matching spell in the graveyard") {
                val game = scenario()
                    .withPlayers("Player 1", "Player 2")
                    .withCardOnBattlefield(1, "Wishing Well")
                    .withCardInGraveyard(1, "Scorching Spear")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val well = game.findPermanent("Wishing Well")!!
                val ability = cardRegistry.getCard("Wishing Well")!!.script.activatedAbilities[0]

                game.execute(ActivateAbility(game.player1Id, well, ability.id, emptyList()))
                game.resolveStack()

                game.hasPendingDecision() shouldBe true
                game.skipSelection()
                game.resolveStack()

                game.isInGraveyard(1, "Scorching Spear") shouldBe true
                val exile = game.state.getExile(game.player1Id)
                exile.isEmpty() shouldBe true
            }

            test("after two activations, an MV-2 spell becomes castable") {
                val game = scenario()
                    .withPlayers("Player 1", "Player 2")
                    .withCardOnBattlefield(1, "Wishing Well")
                    .withCardInGraveyard(1, "Volcanic Hammer") // MV 2
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val well = game.findPermanent("Wishing Well")!!
                val ability = cardRegistry.getCard("Wishing Well")!!.script.activatedAbilities[0]

                // First activation: 1 coin counter; MV-2 hammer is NOT eligible
                game.execute(ActivateAbility(game.player1Id, well, ability.id, emptyList()))
                game.resolveStack()
                game.hasPendingDecision() shouldBe false
                game.isInGraveyard(1, "Volcanic Hammer") shouldBe true

                // Untap the artifact so we can re-activate (no cleanup involved)
                game.state = game.state.updateEntity(well) { container ->
                    container.without<TappedComponent>()
                }

                // Second activation: 2 coin counters; MV-2 hammer IS eligible
                game.execute(ActivateAbility(game.player1Id, well, ability.id, emptyList()))
                game.resolveStack()
                game.hasPendingDecision() shouldBe true
                val hammerId = game.findCardsInGraveyard(1, "Volcanic Hammer").single()
                game.selectCards(listOf(hammerId))
                game.resolveStack()

                val coins = game.state.getEntity(well)
                    ?.get<CountersComponent>()?.getCount(CounterType.COIN) ?: 0
                coins shouldBe 2

                val exile = game.state.getExile(game.player1Id)
                val hammerExiled = exile.any {
                    game.state.getEntity(it)?.get<CardComponent>()?.name == "Volcanic Hammer"
                }
                hammerExiled shouldBe true
            }
        }
    }
}
