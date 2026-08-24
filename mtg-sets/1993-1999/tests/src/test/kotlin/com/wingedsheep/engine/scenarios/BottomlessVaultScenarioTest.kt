package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe

/**
 * Bottomless Vault (FEM #92) — the storage-land cycle's mana ability.
 *
 * "{T}, Remove any number of storage counters from this land: Add {B} for each storage counter
 * removed this way."
 *
 * The counters are only half the cost: the {T} is the other half, and a storage land that is
 * *still tapped* (the state the cycle spends most of its life in, since charging it means choosing
 * not to untap) cannot pay it. Both halves are pinned here, because "I have counters but can't
 * spend them" reads like a bug and is the rules working.
 */
class BottomlessVaultScenarioTest : ScenarioTestBase() {

    private val vaultAbilityId =
        cardRegistry.getCard("Bottomless Vault")!!.activatedAbilities.first().id

    private fun storageCounters(game: TestGame, id: EntityId): Int =
        game.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.STORAGE) ?: 0

    init {
        context("Bottomless Vault") {

            test("removing storage counters adds that much black mana") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Bottomless Vault")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val vault = game.findPermanent("Bottomless Vault")!!
                game.state = game.state.updateEntity(vault) { c ->
                    c.with((c.get<CountersComponent>() ?: CountersComponent())
                        .withAdded(CounterType.STORAGE, 3))
                }

                val activation = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = vault,
                        abilityId = vaultAbilityId,
                        xValue = 2,
                    )
                )
                withClue("Activating for X=2 should succeed: ${activation.error}") {
                    activation.error shouldBe null
                }
                game.resolveStack()

                withClue("two storage counters paid for two black mana") {
                    game.state.getEntity(game.player1Id)?.get<ManaPoolComponent>()?.black shouldBe 2
                }
                withClue("only the counters spent are gone — X is a choice, not the whole pile") {
                    storageCounters(game, vault) shouldBe 1
                }
                withClue("the {T} half of the cost was paid too") {
                    (game.state.getEntity(vault)?.get<TappedComponent>() != null).shouldBeTrue()
                }
            }

            test("the legal action tells the client there is an X to choose, and its ceiling") {
                // The client only opens its X picker for an action flagged hasXCost, and it caps
                // the picker at maxAffordableX. If either is missing the player is handed an
                // activation with no choice, which pays X = 0 and produces no mana at all.
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Bottomless Vault")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val vault = game.findPermanent("Bottomless Vault")!!
                game.state = game.state.updateEntity(vault) { c ->
                    c.with((c.get<CountersComponent>() ?: CountersComponent())
                        .withAdded(CounterType.STORAGE, 3))
                }

                val action = game.getLegalActions(1).single {
                    (it.action as? ActivateAbility)?.sourceId == vault
                }
                withClue("the ability carries an X the player must choose") {
                    action.hasXCost.shouldBeTrue()
                }
                withClue("X is capped by the counters actually on the land") {
                    action.maxAffordableX shouldBe 3
                }
            }

            test("all of the counters can be spent at once") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Bottomless Vault")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val vault = game.findPermanent("Bottomless Vault")!!
                game.state = game.state.updateEntity(vault) { c ->
                    c.with((c.get<CountersComponent>() ?: CountersComponent())
                        .withAdded(CounterType.STORAGE, 3))
                }

                game.execute(
                    ActivateAbility(game.player1Id, vault, vaultAbilityId, xValue = 3)
                ).error shouldBe null
                game.resolveStack()

                game.state.getEntity(game.player1Id)?.get<ManaPoolComponent>()?.black shouldBe 3
                storageCounters(game, vault) shouldBe 0
            }

            test("a still-tapped storage land cannot spend its counters — the {T} is unpayable") {
                // The state a charged storage land is usually found in, and the one that reads as a
                // bug: three counters sitting there with no way to cash them until it untaps.
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Bottomless Vault", tapped = true)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val vault = game.findPermanent("Bottomless Vault")!!
                game.state = game.state.updateEntity(vault) { c ->
                    c.with((c.get<CountersComponent>() ?: CountersComponent())
                        .withAdded(CounterType.STORAGE, 3))
                }

                val activation = game.execute(
                    ActivateAbility(game.player1Id, vault, vaultAbilityId, xValue = 2)
                )
                withClue("the engine must refuse: a tapped permanent cannot pay {T}") {
                    (activation.error != null).shouldBeTrue()
                }
                withClue("and no counters are consumed by the refused activation") {
                    storageCounters(game, vault) shouldBe 3
                }
                withClue("nor is it offered to the client as something the player can actually do") {
                    game.getLegalActions(1).any {
                        (it.action as? ActivateAbility)?.sourceId == vault && it.isAffordable
                    }.shouldBeFalse()
                }
            }
        }
    }
}
