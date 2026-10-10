package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.UntappedEvent
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AbilityId
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * The untap mirror of a tap-permanents cost — `Costs.UntapPermanents(n)`, "untap N tapped
 * creatures you control" (Halo Fountain).
 *
 * Rules pinned here:
 *  - Only *tapped* permanents you control that match the filter can pay it, and too few means the
 *    ability can't be activated at all (CR 118.3).
 *  - It is not the `{Q}` symbol on the creature's own ability, so summoning sickness (CR 302.6)
 *    doesn't stop a creature from being untapped for it.
 *  - Untapping is a real untap: it emits an [UntappedEvent], and a stun counter replaces it
 *    (CR 122.1d) — the counter is spent and the creature stays tapped.
 *  - A creature that "can't become untapped" can't be used to pay it.
 */
class UntapPermanentsCostTest : ScenarioTestBase() {

    private val fountain = card("Test Untap Fountain") {
        manaCost = "{2}"
        typeLine = "Artifact"
        activatedAbility {
            cost = Costs.Composite(Costs.Tap, Costs.UntapPermanents(1))
            effect = Effects.GainLife(1)
        }
        activatedAbility {
            cost = Costs.Composite(Costs.Tap, Costs.UntapPermanents(2))
            effect = Effects.GainLife(5)
        }
    }

    private val untapLock = card("Test Untap Lock") {
        manaCost = "{2}"
        typeLine = "Artifact"
        staticAbility {
            ability = GrantKeyword(AbilityFlag.CANT_BECOME_UNTAPPED.name, GroupFilter.AllCreatures)
        }
    }

    private fun abilityId(index: Int): AbilityId =
        cardRegistry.getCard("Test Untap Fountain")!!.activatedAbilities[index].id

    private fun TestGame.activate(index: Int, chosen: List<EntityId>) = execute(
        ActivateAbility(
            playerId = player1Id,
            sourceId = findPermanent("Test Untap Fountain")!!,
            abilityId = abilityId(index),
            costPayment = AdditionalCostPayment(tappedPermanents = chosen)
        )
    )

    private fun TestGame.isTapped(id: EntityId) = state.getEntity(id)?.has<TappedComponent>() == true

    private fun TestGame.fountainActions() = getLegalActions(1).filter {
        it.isAffordable && (it.action as? ActivateAbility)?.sourceId == findPermanent("Test Untap Fountain")
    }

    init {
        cardRegistry.register(fountain)
        cardRegistry.register(untapLock)

        context("Untap N tapped creatures you control as a cost") {

            test("offers only your tapped creatures, with the untap wording") {
                val game = scenario()
                    .withPlayers()
                    .withCardOnBattlefield(1, "Test Untap Fountain")
                    .withCardOnBattlefield(1, "Centaur Courser", tapped = true)
                    .withCardOnBattlefield(1, "Savannah Lions")
                    .withCardOnBattlefield(2, "Goblin Guide", tapped = true)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val courser = game.findPermanent("Centaur Courser")!!
                val actions = game.fountainActions()
                withClue("only the one-creature ability is affordable with a single tapped creature") {
                    actions.size shouldBe 1
                }
                val info = actions.single().additionalCostInfo!!
                info.costType shouldBe "TapPermanents"
                info.description shouldBe "Untap a tapped creature you control"
                info.validTapTargets shouldContainExactlyInAnyOrder listOf(courser)
                withClue("an untap cost never batches like station") { info.tapBatchMaxActivations shouldBe 1 }
            }

            test("is not offered without enough tapped creatures (CR 118.3)") {
                val game = scenario()
                    .withPlayers()
                    .withCardOnBattlefield(1, "Test Untap Fountain")
                    .withCardOnBattlefield(1, "Centaur Courser")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.fountainActions().shouldBeEmpty()
                val courser = game.findPermanent("Centaur Courser")!!
                withClue("an untapped creature can't be submitted as payment") {
                    game.activate(0, listOf(courser)).error shouldNotBe null
                }
            }

            test("paying untaps the chosen creatures and emits untap events") {
                val game = scenario()
                    .withPlayers()
                    .withCardOnBattlefield(1, "Test Untap Fountain")
                    .withCardOnBattlefield(1, "Centaur Courser", tapped = true)
                    .withCardOnBattlefield(1, "Savannah Lions", tapped = true)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val courser = game.findPermanent("Centaur Courser")!!
                val lions = game.findPermanent("Savannah Lions")!!
                val result = game.activate(1, listOf(courser, lions))
                withClue("activation should succeed: ${result.error}") { result.error shouldBe null }
                result.events.filterIsInstance<UntappedEvent>().map { it.entityId } shouldContainExactlyInAnyOrder
                    listOf(courser, lions)
                game.isTapped(courser) shouldBe false
                game.isTapped(lions) shouldBe false
                game.isTapped(game.findPermanent("Test Untap Fountain")!!) shouldBe true

                game.resolveStack()
                game.getLifeTotal(1) shouldBe 25
            }

            test("a summoning-sick creature can be untapped for it (not the {Q} symbol)") {
                val game = scenario()
                    .withPlayers()
                    .withCardOnBattlefield(1, "Test Untap Fountain")
                    .withCardOnBattlefield(1, "Centaur Courser", tapped = true, summoningSickness = true)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val courser = game.findPermanent("Centaur Courser")!!
                game.fountainActions().single().additionalCostInfo!!.validTapTargets shouldBe listOf(courser)
                game.activate(0, listOf(courser)).error shouldBe null
                game.isTapped(courser) shouldBe false
            }

            test("rejects an opponent's creature and the same creature chosen twice") {
                val game = scenario()
                    .withPlayers()
                    .withCardOnBattlefield(1, "Test Untap Fountain")
                    .withCardOnBattlefield(1, "Centaur Courser", tapped = true)
                    .withCardOnBattlefield(2, "Goblin Guide", tapped = true)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val courser = game.findPermanent("Centaur Courser")!!
                val guide = game.findPermanent("Goblin Guide")!!
                game.activate(0, listOf(guide)).error shouldNotBe null
                game.activate(1, listOf(courser, courser)).error shouldNotBe null
                withClue("a rejected payment changes nothing") {
                    game.isTapped(courser) shouldBe true
                    game.isTapped(guide) shouldBe true
                }
            }

            test("a stun counter replaces the untap: the counter is spent, the creature stays tapped (CR 122.1d)") {
                val game = scenario()
                    .withPlayers()
                    .withCardOnBattlefield(1, "Test Untap Fountain")
                    .withCardOnBattlefield(1, "Centaur Courser", tapped = true)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val courser = game.findPermanent("Centaur Courser")!!
                game.state = game.state.updateEntity(courser) {
                    it.with(CountersComponent(mapOf(CounterType.STUN to 1)))
                }
                val result = game.activate(0, listOf(courser))
                result.error shouldBe null
                game.isTapped(courser) shouldBe true
                game.state.getEntity(courser)!!.get<CountersComponent>()!!.getCount(CounterType.STUN) shouldBe 0
                result.events.filterIsInstance<UntappedEvent>().shouldBeEmpty()
            }

            test("a creature that can't become untapped can't pay it") {
                val game = scenario()
                    .withPlayers()
                    .withCardOnBattlefield(1, "Test Untap Fountain")
                    .withCardOnBattlefield(1, "Test Untap Lock")
                    .withCardOnBattlefield(1, "Centaur Courser", tapped = true)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val courser = game.findPermanent("Centaur Courser")!!
                game.fountainActions().shouldBeEmpty()
                game.activate(0, listOf(courser)).error shouldNotBe null
                game.isTapped(courser) shouldBe true
            }
        }
    }
}
