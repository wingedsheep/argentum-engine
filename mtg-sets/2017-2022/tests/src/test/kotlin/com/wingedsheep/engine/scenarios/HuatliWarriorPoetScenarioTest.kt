package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.lea.cards.SamiteHealer
import com.wingedsheep.mtg.sets.definitions.xln.cards.HuatliWarriorPoet
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AbilityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Huatli, Warrior Poet (XLN #224, {3}{R}{W}, Loyalty 3).
 *
 *   +2: You gain life equal to the greatest power among creatures you control.
 *   0: Create a 3/3 green Dinosaur creature token with trample.
 *   −X: Huatli deals X damage divided as you choose among any number of target creatures.
 *       Creatures dealt damage this way can't block this turn.
 *
 * The −X announces X (CR 601.2b), then its targets and the division (CR 601.2d), so the legal action
 * prices the division per X. "Dealt damage this way" is only what the division actually reached.
 */
class HuatliWarriorPoetScenarioTest : ScenarioTestBase() {

    private val plusTwo = HuatliWarriorPoet.activatedAbilities[0].id
    private val zero = HuatliWarriorPoet.activatedAbilities[1].id
    private val minusX = HuatliWarriorPoet.activatedAbilities[2].id

    init {
        context("Huatli, Warrior Poet") {

            test("+2 gains life equal to the greatest power among your creatures") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Huatli, Warrior Poet")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(1, "Force of Nature")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val huatli = game.findPermanent("Huatli, Warrior Poet")!!
                seedLoyalty(game, huatli, 3)

                game.execute(ActivateAbility(game.player1Id, huatli, plusTwo)).error shouldBe null
                game.resolveStack()

                game.getLifeTotal(1) shouldBe 25
                loyalty(game, huatli) shouldBe 5
            }

            test("0 creates a 3/3 green Dinosaur with trample") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Huatli, Warrior Poet")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val huatli = game.findPermanent("Huatli, Warrior Poet")!!
                seedLoyalty(game, huatli, 3)

                game.execute(ActivateAbility(game.player1Id, huatli, zero)).error shouldBe null
                game.resolveStack()

                val dino = game.findPermanent("Dinosaur Token") ?: game.findPermanent("Dinosaur")
                dino shouldNotBe null
                val projected = game.state.projectedState
                projected.getPower(dino!!) shouldBe 3
                projected.getToughness(dino) shouldBe 3
                projected.hasKeyword(dino, com.wingedsheep.sdk.core.Keyword.TRAMPLE) shouldBe true
                loyalty(game, huatli) shouldBe 3
            }

            test("−X offers a division total for every X up to Huatli's loyalty") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Huatli, Warrior Poet")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Centaur Courser")
                    .withActivePlayer(1)
                    .withPriorityPlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val huatli = game.findPermanent("Huatli, Warrior Poet")!!
                seedLoyalty(game, huatli, 4)

                val action = game.getLegalActions(1).single { it.abilityId() == minusX }
                action.hasXCost shouldBe true
                action.maxAffordableX shouldBe 4
                action.requiresDamageDistribution shouldBe true
                action.minDamagePerTarget shouldBe 1
                action.totalDamageByX shouldBe mapOf(0 to 0, 1 to 1, 2 to 2, 3 to 3, 4 to 4)
            }

            test("−X deals the announced division, and the creatures it damaged can't block") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Huatli, Warrior Poet")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Force of Nature")
                    .withCardOnBattlefield(2, "Centaur Courser")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val huatli = game.findPermanent("Huatli, Warrior Poet")!!
                seedLoyalty(game, huatli, 4)
                val bears = game.findPermanent("Grizzly Bears")!!
                val force = game.findPermanent("Force of Nature")!!
                val courser = game.findPermanent("Centaur Courser")!!
                val targets = listOf(ChosenTarget.Permanent(bears), ChosenTarget.Permanent(force))

                withClue("the division must total the announced X") {
                    game.execute(ActivateAbility(game.player1Id, huatli, minusX, targets = targets, xValue = 3,
                        damageDistribution = mapOf(bears to 1, force to 1))).error shouldNotBe null
                }
                withClue("X = 1 can't reach two targets") {
                    game.execute(ActivateAbility(game.player1Id, huatli, minusX, targets = targets, xValue = 1,
                        damageDistribution = mapOf(bears to 1, force to 0))).error shouldNotBe null
                }
                loyalty(game, huatli) shouldBe 4

                game.execute(ActivateAbility(game.player1Id, huatli, minusX, targets = targets, xValue = 3,
                    damageDistribution = mapOf(bears to 1, force to 2))).error shouldBe null
                loyalty(game, huatli) shouldBe 1
                game.resolveStack()

                game.hasPendingDecision() shouldBe false
                val projected = game.state.projectedState
                projected.cantBlock(bears) shouldBe true
                projected.cantBlock(force) shouldBe true
                withClue("an untargeted creature can still block") {
                    projected.cantBlock(courser) shouldBe false
                }
            }

            test("a target whose share was prevented wasn't dealt damage this way and can still block") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Huatli, Warrior Poet")
                    .withCardOnBattlefield(1, "Samite Healer", summoningSickness = false)
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Force of Nature")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val huatli = game.findPermanent("Huatli, Warrior Poet")!!
                seedLoyalty(game, huatli, 3)
                val healer = game.findPermanent("Samite Healer")!!
                val bears = game.findPermanent("Grizzly Bears")!!
                val force = game.findPermanent("Force of Nature")!!

                // Prevent the next 1 damage to the Bears.
                game.execute(ActivateAbility(game.player1Id, healer, SamiteHealer.activatedAbilities.first().id,
                    targets = listOf(ChosenTarget.Permanent(bears)))).error shouldBe null
                game.resolveStack()

                game.execute(ActivateAbility(game.player1Id, huatli, minusX,
                    targets = listOf(ChosenTarget.Permanent(bears), ChosenTarget.Permanent(force)), xValue = 2,
                    damageDistribution = mapOf(bears to 1, force to 1))).error shouldBe null
                game.resolveStack()

                val projected = game.state.projectedState
                withClue("the Bears' 1 was prevented") {
                    projected.cantBlock(bears) shouldBe false
                }
                projected.cantBlock(force) shouldBe true
            }

            test("a target that became illegal loses its share and isn't dealt damage this way") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Huatli, Warrior Poet")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Force of Nature")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val huatli = game.findPermanent("Huatli, Warrior Poet")!!
                seedLoyalty(game, huatli, 3)
                val bears = game.findPermanent("Grizzly Bears")!!
                val force = game.findPermanent("Force of Nature")!!

                game.execute(ActivateAbility(game.player1Id, huatli, minusX,
                    targets = listOf(ChosenTarget.Permanent(bears), ChosenTarget.Permanent(force)), xValue = 2,
                    damageDistribution = mapOf(bears to 1, force to 1))).error shouldBe null

                // The Bears leave in response; they are no longer a legal target.
                game.state = game.state.moveToZone(bears,
                    ZoneKey(game.player2Id, Zone.BATTLEFIELD), ZoneKey(game.player2Id, Zone.HAND))
                game.resolveStack()

                withClue("the Force keeps its announced 1, not the whole X") {
                    (game.state.getEntity(force)?.get<DamageComponent>()?.amount ?: 0) shouldBe 1
                }
                game.state.projectedState.cantBlock(force) shouldBe true
            }

            test("a division left to resolution still marks what it damaged") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Huatli, Warrior Poet")
                    .withCardOnBattlefield(2, "Force of Nature")
                    .withCardOnBattlefield(2, "Centaur Courser")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val huatli = game.findPermanent("Huatli, Warrior Poet")!!
                seedLoyalty(game, huatli, 3)
                val force = game.findPermanent("Force of Nature")!!
                val courser = game.findPermanent("Centaur Courser")!!

                game.execute(ActivateAbility(game.player1Id, huatli, minusX,
                    targets = listOf(ChosenTarget.Permanent(force), ChosenTarget.Permanent(courser)), xValue = 2)
                ).error shouldBe null
                game.resolveStack()

                game.hasPendingDecision() shouldBe true
                game.submitDistribution(mapOf(force to 1, courser to 1)).error shouldBe null

                val projected = game.state.projectedState
                projected.cantBlock(force) shouldBe true
                projected.cantBlock(courser) shouldBe true
            }

            test("−0 may be activated with no targets") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Huatli, Warrior Poet")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val huatli = game.findPermanent("Huatli, Warrior Poet")!!
                seedLoyalty(game, huatli, 3)
                val bears = game.findPermanent("Grizzly Bears")!!

                withClue("with X = 0 no target can be chosen") {
                    game.execute(ActivateAbility(game.player1Id, huatli, minusX,
                        targets = listOf(ChosenTarget.Permanent(bears)), xValue = 0)).error shouldNotBe null
                }
                game.execute(ActivateAbility(game.player1Id, huatli, minusX, xValue = 0)).error shouldBe null
                game.resolveStack()
                game.state.projectedState.cantBlock(bears) shouldBe false
                loyalty(game, huatli) shouldBe 3
            }
        }
    }

    private fun seedLoyalty(game: TestGame, id: EntityId, amount: Int) {
        // The scenario builder doesn't run "enters with its starting loyalty", so seed it.
        game.state = game.state.updateEntity(id) { c ->
            c.with(CountersComponent().withAdded(CounterType.LOYALTY, amount))
        }
    }

    private fun loyalty(game: TestGame, id: EntityId): Int =
        game.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.LOYALTY) ?: 0

    private fun com.wingedsheep.engine.view.LegalActionInfo.abilityId(): AbilityId? =
        (action as? ActivateAbility)?.abilityId
}
