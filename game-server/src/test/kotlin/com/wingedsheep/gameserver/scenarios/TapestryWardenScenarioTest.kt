package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.battlefield.AttachmentsComponent
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import com.wingedsheep.sdk.scripting.AbilityCost
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Tapestry Warden (EOE).
 *
 * Tapestry Warden: {3}{G} Artifact Creature — Robot Soldier 3/4
 * Vigilance
 * Each creature you control with toughness greater than its power assigns combat damage
 * equal to its toughness rather than its power.
 * Each creature you control with toughness greater than its power stations permanents
 * using its toughness rather than its power.
 */
class TapestryWardenScenarioTest : ScenarioTestBase() {

    /**
     * Manually wire an Aura that's already on the battlefield onto a target permanent.
     * `In Bolas's Clutches` grants control via Layer 2; once attached, projected state
     * reports the Aura's controller as the controller of the enchanted permanent.
     */
    private fun ScenarioTestBase.TestGame.attachTo(attachmentName: String, targetName: String) {
        val attachmentId = findPermanent(attachmentName)!!
        val targetId = findPermanent(targetName)!!
        val attachment = state.getEntity(attachmentId)!!.with(AttachedToComponent(targetId))
        state = state.withEntity(attachmentId, attachment)
        val target = state.getEntity(targetId)!!
        val existing = target.get<AttachmentsComponent>()?.attachedIds ?: emptyList()
        state = state.withEntity(targetId, target.with(AttachmentsComponent(existing + attachmentId)))
    }

    init {
        context("Tapestry Warden — assigns damage as toughness") {

            test("1/2 creature assigns 2 (toughness) damage when Tapestry Warden is in play") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Tapestry Warden", summoningSickness = false)
                    .withCardOnBattlefield(1, "Devoted Hero", summoningSickness = false) // 1/2 (T > P)
                    .withActivePlayer(1)
                    .withLifeTotal(2, 20)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                game.declareAttackers(mapOf("Devoted Hero" to 2))
                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

                withClue("Defending player should have taken 2 (toughness of 1/2 Devoted Hero) damage") {
                    game.getLifeTotal(2) shouldBe 18
                }
            }

            test("Tapestry Warden itself (3/4, T > P) assigns 4 (toughness) damage") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Tapestry Warden", summoningSickness = false)
                    .withActivePlayer(1)
                    .withLifeTotal(2, 20)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                game.declareAttackers(mapOf("Tapestry Warden" to 2))
                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

                withClue("Tapestry Warden (3/4) should assign 4 (its toughness) damage") {
                    game.getLifeTotal(2) shouldBe 16
                }
            }

            test("creature with power >= toughness is not affected (still uses power)") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Tapestry Warden", summoningSickness = false)
                    .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false) // 2/2 (T == P)
                    .withActivePlayer(1)
                    .withLifeTotal(2, 20)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                game.declareAttackers(mapOf("Grizzly Bears" to 2))
                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

                withClue("Grizzly Bears (2/2, T == P) should still deal 2 (power) damage") {
                    game.getLifeTotal(2) shouldBe 18
                }
            }

            test("stolen 1/2 creature assigns toughness damage — exercises projected controller") {
                // Player 2 owns Devoted Hero; In Bolas's Clutches transfers control to player 1.
                // Tapestry Warden's "creatures you control" filter must read the projected
                // controller (player 1) of Devoted Hero, not its base ControllerComponent
                // (still player 2), or the toughness-as-damage substitution silently drops.
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Tapestry Warden", summoningSickness = false)
                    .withCardOnBattlefield(2, "Devoted Hero", summoningSickness = false) // owned by p2
                    .withCardOnBattlefield(1, "In Bolas's Clutches")
                    .withActivePlayer(1)
                    .withLifeTotal(2, 20)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.attachTo("In Bolas's Clutches", "Devoted Hero")
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)

                game.declareAttackers(mapOf("Devoted Hero" to 2))
                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

                withClue("Stolen Devoted Hero (1/2) should deal 2 (toughness) damage under Tapestry Warden's effect") {
                    game.getLifeTotal(2) shouldBe 18
                }
            }

            test("ability only applies to controller's creatures, not opponent's") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Tapestry Warden", summoningSickness = false)
                    .withCardOnBattlefield(2, "Devoted Hero", summoningSickness = false) // opponent's 1/2
                    .withActivePlayer(2)
                    .withLifeTotal(1, 20)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                game.declareAttackers(mapOf("Devoted Hero" to 1))
                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

                withClue("Opponent's 1/2 Devoted Hero should deal 1 (power) damage, not 2 (toughness)") {
                    game.getLifeTotal(1) shouldBe 19
                }
            }
        }

        context("Tapestry Warden — stations using toughness") {

            test("1/2 creature contributes 2 (toughness) charge counters when stationing with Tapestry Warden in play") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Tapestry Warden", summoningSickness = false)
                    .withCardOnBattlefield(1, "Devoted Hero", summoningSickness = false) // 1/2 tapper
                    .withCardOnBattlefield(1, "Debris Field Crusher") // the Spacecraft being stationed
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val stationAbility = cardRegistry.getCard("Debris Field Crusher")!!
                    .script.activatedAbilities.first { it.cost is AbilityCost.TapPermanents }
                val crusherId = game.findPermanent("Debris Field Crusher")!!
                val devotedHeroId = game.findPermanent("Devoted Hero")!!

                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = crusherId,
                        abilityId = stationAbility.id,
                        costPayment = AdditionalCostPayment(tappedPermanents = listOf(devotedHeroId))
                    )
                )
                withClue("Station activation should succeed: ${result.error}") {
                    result.error shouldBe null
                }
                game.resolveStack()

                val counters = game.state.getEntity(crusherId)?.get<CountersComponent>()
                withClue("Debris Field Crusher should have 2 charge counters (Devoted Hero toughness), not 1 (power)") {
                    counters?.getCount(CounterType.CHARGE) shouldBe 2
                }
            }

            test("stolen 1/2 creature stations using its toughness — exercises projected controller") {
                // Tapestry Warden, Debris Field Crusher, and In Bolas's Clutches all start under
                // player 1's control. Player 2's Devoted Hero is "stolen" via the Aura, so its
                // projected controller is player 1 even though its base ControllerComponent
                // still points at player 2. Reading base controllers in the station-toughness
                // lookup would miss this and put only 1 charge counter on the Spacecraft.
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Tapestry Warden", summoningSickness = false)
                    .withCardOnBattlefield(2, "Devoted Hero", summoningSickness = false) // owned by p2
                    .withCardOnBattlefield(1, "Debris Field Crusher")
                    .withCardOnBattlefield(1, "In Bolas's Clutches")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.attachTo("In Bolas's Clutches", "Devoted Hero")

                val stationAbility = cardRegistry.getCard("Debris Field Crusher")!!
                    .script.activatedAbilities.first { it.cost is AbilityCost.TapPermanents }
                val crusherId = game.findPermanent("Debris Field Crusher")!!
                val devotedHeroId = game.findPermanent("Devoted Hero")!!

                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = crusherId,
                        abilityId = stationAbility.id,
                        costPayment = AdditionalCostPayment(tappedPermanents = listOf(devotedHeroId))
                    )
                )
                withClue("Station activation should succeed: ${result.error}") {
                    result.error shouldBe null
                }
                game.resolveStack()

                val counters = game.state.getEntity(crusherId)?.get<CountersComponent>()
                withClue("Stolen Devoted Hero should contribute 2 charge counters (toughness) under projected controller") {
                    counters?.getCount(CounterType.CHARGE) shouldBe 2
                }
            }

            test("tapped creature that left the battlefield still contributes toughness (last known information)") {
                // Per the 2025-07-25 ruling: "If a station ability you control resolves while you
                // control Tapestry Warden, but the creature tapped to pay the cost of that station
                // ability is no longer on the battlefield, check the characteristics of that creature
                // as it last existed on the battlefield. If its toughness was greater than its power,
                // use its toughness to determine how many counters are put on the permanent with
                // station."
                //
                // Simulates Devoted Hero (1/2) being killed in response to the station activation —
                // by manually moving it from battlefield to graveyard while the station ability is
                // on the stack. The station should still place 2 charge counters (its LKI toughness).
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Tapestry Warden", summoningSickness = false)
                    .withCardOnBattlefield(1, "Devoted Hero", summoningSickness = false) // 1/2 tapper
                    .withCardOnBattlefield(1, "Debris Field Crusher")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val stationAbility = cardRegistry.getCard("Debris Field Crusher")!!
                    .script.activatedAbilities.first { it.cost is AbilityCost.TapPermanents }
                val crusherId = game.findPermanent("Debris Field Crusher")!!
                val devotedHeroId = game.findPermanent("Devoted Hero")!!

                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = crusherId,
                        abilityId = stationAbility.id,
                        costPayment = AdditionalCostPayment(tappedPermanents = listOf(devotedHeroId))
                    )
                )
                withClue("Station activation should succeed: ${result.error}") {
                    result.error shouldBe null
                }

                // Simulate Devoted Hero being killed in response (e.g., a Lightning Bolt) — it
                // leaves the battlefield before the station ability resolves.
                game.state = game.state.moveToZone(
                    devotedHeroId,
                    ZoneKey(game.player1Id, Zone.BATTLEFIELD),
                    ZoneKey(game.player1Id, Zone.GRAVEYARD)
                )

                game.resolveStack()

                val counters = game.state.getEntity(crusherId)?.get<CountersComponent>()
                withClue("Tapped creature left the battlefield; per LKI ruling Crusher should still get 2 charge counters (toughness), not 1 (power) or 0") {
                    counters?.getCount(CounterType.CHARGE) shouldBe 2
                }
            }

            test("losing control of Tapestry Warden mid-resolution drops back to power") {
                // Per the 2025-07-25 ruling: "If you activate a station ability while you control
                // Tapestry Warden, but you no longer control Tapestry Warden at the time that
                // ability resolves, use the power of the creature tapped to pay the cost of the
                // station ability to determine how many counters are put on the permanent with
                // station."
                //
                // Simulates removing Tapestry Warden after the station ability is on the stack:
                // the override evaluator checks the current battlefield at resolution time, so the
                // tapped 1/2 should now contribute 1 (power) charge counter.
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Tapestry Warden", summoningSickness = false)
                    .withCardOnBattlefield(1, "Devoted Hero", summoningSickness = false) // 1/2 tapper
                    .withCardOnBattlefield(1, "Debris Field Crusher")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val stationAbility = cardRegistry.getCard("Debris Field Crusher")!!
                    .script.activatedAbilities.first { it.cost is AbilityCost.TapPermanents }
                val crusherId = game.findPermanent("Debris Field Crusher")!!
                val wardenId = game.findPermanent("Tapestry Warden")!!
                val devotedHeroId = game.findPermanent("Devoted Hero")!!

                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = crusherId,
                        abilityId = stationAbility.id,
                        costPayment = AdditionalCostPayment(tappedPermanents = listOf(devotedHeroId))
                    )
                )
                withClue("Station activation should succeed: ${result.error}") {
                    result.error shouldBe null
                }

                // Remove Tapestry Warden from the battlefield before resolution (e.g., destroyed in
                // response). The toughness-substitution should no longer apply.
                game.state = game.state.moveToZone(
                    wardenId,
                    ZoneKey(game.player1Id, Zone.BATTLEFIELD),
                    ZoneKey(game.player1Id, Zone.GRAVEYARD)
                )

                game.resolveStack()

                val counters = game.state.getEntity(crusherId)?.get<CountersComponent>()
                withClue("Tapestry Warden gone at resolution; Crusher should get 1 charge counter (power), not 2 (toughness)") {
                    counters?.getCount(CounterType.CHARGE) shouldBe 1
                }
            }

            test("without Tapestry Warden, 1/2 creature contributes only 1 (power) charge counter") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Devoted Hero", summoningSickness = false) // 1/2 tapper
                    .withCardOnBattlefield(1, "Debris Field Crusher")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val stationAbility = cardRegistry.getCard("Debris Field Crusher")!!
                    .script.activatedAbilities.first { it.cost is AbilityCost.TapPermanents }
                val crusherId = game.findPermanent("Debris Field Crusher")!!
                val devotedHeroId = game.findPermanent("Devoted Hero")!!

                game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = crusherId,
                        abilityId = stationAbility.id,
                        costPayment = AdditionalCostPayment(tappedPermanents = listOf(devotedHeroId))
                    )
                )
                game.resolveStack()

                val counters = game.state.getEntity(crusherId)?.get<CountersComponent>()
                withClue("Without Tapestry Warden, Debris Field Crusher should have 1 charge counter (power)") {
                    counters?.getCount(CounterType.CHARGE) shouldBe 1
                }
            }

            test("creature with power > toughness stations using power even with Tapestry Warden in play") {
                // The station-toughness substitution is gated on toughness > power. A 3/2
                // Aphetto Vulture under Tapestry Warden should still contribute 3 charge
                // counters from its power, not be downgraded to its (lower) toughness.
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Tapestry Warden", summoningSickness = false)
                    .withCardOnBattlefield(1, "Aphetto Vulture", summoningSickness = false) // 3/2 (P > T)
                    .withCardOnBattlefield(1, "Debris Field Crusher")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val stationAbility = cardRegistry.getCard("Debris Field Crusher")!!
                    .script.activatedAbilities.first { it.cost is AbilityCost.TapPermanents }
                val crusherId = game.findPermanent("Debris Field Crusher")!!
                val vultureId = game.findPermanent("Aphetto Vulture")!!

                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = crusherId,
                        abilityId = stationAbility.id,
                        costPayment = AdditionalCostPayment(tappedPermanents = listOf(vultureId))
                    )
                )
                withClue("Station activation should succeed: ${result.error}") {
                    result.error shouldBe null
                }
                game.resolveStack()

                val counters = game.state.getEntity(crusherId)?.get<CountersComponent>()
                withClue("Aphetto Vulture (3/2, P > T) should contribute 3 charge counters from power; substitution must not downgrade to toughness") {
                    counters?.getCount(CounterType.CHARGE) shouldBe 3
                }
            }
        }
    }
}
