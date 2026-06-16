package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.costs.CostAtom
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.scripting.values.EntityNumericProperty
import com.wingedsheep.sdk.scripting.values.EntityReference
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * End-to-end rules coverage for the Station keyword ability (CR 702.184) and the `{N+}` station
 * symbols (CR 721.2). Station = "Tap another untapped creature you control: Put a number of charge
 * counters on this permanent equal to the tapped creature's power. Activate only as a sorcery."
 *
 * Cards:
 *  - Wedgelight Rammer — Artifact — Spacecraft, base 3/4; at 9+ charge counters it is an artifact
 *    creature with flying and first strike (CR 721.2b).
 *  - Tapestry Warden — has [com.wingedsheep.sdk.scripting.StationUsingToughness]: each creature its
 *    controller controls with toughness > power stations using toughness instead of power (702.184c).
 */
class StationMechanicTest : ScenarioTestBase() {

    // A wall-type creature whose toughness exceeds its power, for the Tapestry Warden case.
    private val testWall = card("Test Bulwark") {
        typeLine = "Creature — Wall"
        power = 1
        toughness = 5
    }

    // A non-station ability that ALSO reads `EntityProperty(TappedAsCost, Power)` — proves the
    // 702.184c toughness substitution is scoped to the station amount node (DynamicAmount.
    // StationCharge) and does NOT bleed onto an unrelated "tap a creature: do X equal to its
    // power" ability even while a Tapestry Warden is on the battlefield.
    private val tapCollector = card("Test Tap Collector") {
        typeLine = "Artifact"
        activatedAbility {
            cost = Costs.TapPermanents(count = 1, filter = GameObjectFilter.Creature, excludeSelf = true)
            effect = Effects.AddDynamicCounters(
                counterType = Counters.CHARGE,
                amount = DynamicAmount.EntityProperty(EntityReference.TappedAsCost(), EntityNumericProperty.Power),
                target = EffectTarget.Self
            )
            timing = TimingRule.SorcerySpeed
        }
    }

    // A minimal "destroy target creature" instant, to remove the tapped creature in response and
    // exercise the last-known-information path (CR 112.7a).
    private val doomBlade = card("Test Doom Blade") {
        manaCost = "{1}{B}"
        typeLine = "Instant"
        spell {
            val t = target("target creature", com.wingedsheep.sdk.scripting.targets.TargetCreature())
            effect = Effects.Destroy(t)
        }
    }

    private fun stationAbilityId(cardName: String) =
        cardRegistry.getCard(cardName)!!.activatedAbilities.first { (it.cost as? AbilityCost.Atom)?.atom is CostAtom.TapPermanents }.id

    init {
        cardRegistry.register(testWall)
        cardRegistry.register(tapCollector)
        cardRegistry.register(doomBlade)

        context("Station (CR 702.184)") {

            test("adds charge counters equal to the tapped creature's power (702.184a)") {
                val game = scenario()
                    .withPlayers()
                    .withCardOnBattlefield(1, "Wedgelight Rammer", summoningSickness = false)
                    .withCardOnBattlefield(1, "Spined Wurm", summoningSickness = false)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val rammer = game.findPermanent("Wedgelight Rammer")!!
                val wurm = game.findPermanent("Spined Wurm")!! // 5/4

                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = rammer,
                        abilityId = stationAbilityId("Wedgelight Rammer"),
                        costPayment = AdditionalCostPayment(tappedPermanents = listOf(wurm))
                    )
                )
                withClue("station activation should succeed: ${result.error}") { result.error shouldBe null }
                game.resolveStack()

                withClue("tapped creature is tapped to pay the cost") {
                    game.state.getEntity(wurm)?.has<TappedComponent>() shouldBe true
                }
                withClue("charge counters equal the tapped creature's power (5)") {
                    game.state.getEntity(rammer)?.get<CountersComponent>()?.getCount(CounterType.CHARGE) shouldBe 5
                }
            }

            test("crossing the {9+} threshold turns the Spacecraft into a flying, first-strike creature (721.2)") {
                val game = scenario()
                    .withPlayers()
                    .withCardOnBattlefield(1, "Wedgelight Rammer", summoningSickness = false)
                    .withCardOnBattlefield(1, "Spined Wurm", summoningSickness = false)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val rammer = game.findPermanent("Wedgelight Rammer")!!
                val wurm = game.findPermanent("Spined Wurm")!! // power 5

                // Seed 4 charge counters: still below the 9+ threshold, so not yet a creature.
                game.state = game.state.updateEntity(rammer) {
                    it.with(CountersComponent(mapOf(CounterType.CHARGE to 4)))
                }
                withClue("below threshold: not a creature") { game.state.projectedState.isCreature(rammer) shouldBe false }

                // Station with the 5-power Wurm → 9 charge → meets the {9+} symbol.
                game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = rammer,
                        abilityId = stationAbilityId("Wedgelight Rammer"),
                        costPayment = AdditionalCostPayment(tappedPermanents = listOf(wurm))
                    )
                ).error shouldBe null
                game.resolveStack()

                game.state.getEntity(rammer)?.get<CountersComponent>()?.getCount(CounterType.CHARGE) shouldBe 9
                val projected = game.state.projectedState
                withClue("at 9+ it is an artifact creature") { projected.isCreature(rammer) shouldBe true }
                withClue("at 9+ it has flying") { projected.hasKeyword(rammer, Keyword.FLYING) shouldBe true }
                withClue("at 9+ it has first strike") { projected.hasKeyword(rammer, Keyword.FIRST_STRIKE) shouldBe true }
            }

            test("Tapestry Warden makes a toughness>power creature station using toughness (702.184c)") {
                val game = scenario()
                    .withPlayers()
                    .withCardOnBattlefield(1, "Wedgelight Rammer", summoningSickness = false)
                    .withCardOnBattlefield(1, "Tapestry Warden", summoningSickness = false)
                    .withCardOnBattlefield(1, "Test Bulwark", summoningSickness = false) // 1/5
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val rammer = game.findPermanent("Wedgelight Rammer")!!
                val wall = game.findPermanent("Test Bulwark")!!

                game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = rammer,
                        abilityId = stationAbilityId("Wedgelight Rammer"),
                        costPayment = AdditionalCostPayment(tappedPermanents = listOf(wall))
                    )
                ).error shouldBe null
                game.resolveStack()

                withClue("uses toughness (5) rather than power (1) under Tapestry Warden") {
                    game.state.getEntity(rammer)?.get<CountersComponent>()?.getCount(CounterType.CHARGE) shouldBe 5
                }
            }

            test("the toughness substitution is scoped to station — a generic tapped-power ability still reads power") {
                val game = scenario()
                    .withPlayers()
                    .withCardOnBattlefield(1, "Test Tap Collector", summoningSickness = false)
                    .withCardOnBattlefield(1, "Tapestry Warden", summoningSickness = false)
                    .withCardOnBattlefield(1, "Test Bulwark", summoningSickness = false) // 1/5
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val collector = game.findPermanent("Test Tap Collector")!!
                val wall = game.findPermanent("Test Bulwark")!!

                game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = collector,
                        abilityId = stationAbilityId("Test Tap Collector"),
                        costPayment = AdditionalCostPayment(tappedPermanents = listOf(wall))
                    )
                ).error shouldBe null
                game.resolveStack()

                withClue("non-station ability reads plain power (1), not toughness — Tapestry Warden must not bleed onto it") {
                    game.state.getEntity(collector)?.get<CountersComponent>()?.getCount(CounterType.CHARGE) shouldBe 1
                }
            }

            test("cannot pay the station cost by tapping an already-tapped creature") {
                val game = scenario()
                    .withPlayers()
                    .withCardOnBattlefield(1, "Wedgelight Rammer", summoningSickness = false)
                    .withCardOnBattlefield(1, "Spined Wurm", tapped = true, summoningSickness = false)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val rammer = game.findPermanent("Wedgelight Rammer")!!
                val wurm = game.findPermanent("Spined Wurm")!!

                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = rammer,
                        abilityId = stationAbilityId("Wedgelight Rammer"),
                        costPayment = AdditionalCostPayment(tappedPermanents = listOf(wurm))
                    )
                )
                withClue("tapping an already-tapped creature is an illegal payment") { result.error shouldNotBe null }
            }

            test("cannot tap the Spacecraft itself to pay its own station cost (another creature)") {
                val game = scenario()
                    .withPlayers()
                    .withCardOnBattlefield(1, "Wedgelight Rammer", summoningSickness = false)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val rammer = game.findPermanent("Wedgelight Rammer")!!
                // Make the Rammer a creature so it could (illegally) be chosen as a tap target.
                game.state = game.state.updateEntity(rammer) {
                    it.with(CountersComponent(mapOf(CounterType.CHARGE to 9)))
                }

                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = rammer,
                        abilityId = stationAbilityId("Wedgelight Rammer"),
                        costPayment = AdditionalCostPayment(tappedPermanents = listOf(rammer))
                    )
                )
                withClue("station taps ANOTHER creature — the source is excluded") { result.error shouldNotBe null }
            }

            test("station is sorcery-speed: not activatable on the opponent's turn") {
                val game = scenario()
                    .withPlayers()
                    .withCardOnBattlefield(1, "Wedgelight Rammer", summoningSickness = false)
                    .withCardOnBattlefield(1, "Spined Wurm", summoningSickness = false)
                    .withActivePlayer(2) // opponent's turn
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .withPriorityPlayer(1)
                    .build()

                val rammer = game.findPermanent("Wedgelight Rammer")!!
                val wurm = game.findPermanent("Spined Wurm")!!

                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = rammer,
                        abilityId = stationAbilityId("Wedgelight Rammer"),
                        costPayment = AdditionalCostPayment(tappedPermanents = listOf(wurm))
                    )
                )
                withClue("a sorcery-speed ability can't be activated on the opponent's turn") { result.error shouldNotBe null }
            }

            test("last-known information: charge equals the tapped creature's power even if it leaves first (112.7a)") {
                val game = scenario()
                    .withPlayers()
                    .withCardOnBattlefield(1, "Wedgelight Rammer", summoningSickness = false)
                    .withCardOnBattlefield(1, "Spined Wurm", summoningSickness = false) // 5/4
                    .withCardInHand(2, "Test Doom Blade")
                    .withLandsOnBattlefield(2, "Swamp", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val rammer = game.findPermanent("Wedgelight Rammer")!!
                val wurm = game.findPermanent("Spined Wurm")!!

                // P1 activates station tapping the Wurm; ability goes on the stack.
                game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = rammer,
                        abilityId = stationAbilityId("Wedgelight Rammer"),
                        costPayment = AdditionalCostPayment(tappedPermanents = listOf(wurm))
                    )
                ).error shouldBe null

                // P1 passes priority; P2 destroys the tapped Wurm in response.
                game.passPriority()
                game.castSpell(2, "Test Doom Blade", targetId = wurm).error shouldBe null
                game.resolveStack()

                withClue("Wurm has left the battlefield") { game.isOnBattlefield("Spined Wurm") shouldBe false }
                withClue("charge counters use the tapped creature's last-known power (5)") {
                    game.state.getEntity(rammer)?.get<CountersComponent>()?.getCount(CounterType.CHARGE) shouldBe 5
                }
            }
        }

        // The multi-select shortcut: a single gesture taps several creatures and queues one station
        // activation per creature on the stack (each taps exactly its creature). This is purely a
        // convenience over activating station repeatedly — the resulting game state is identical to
        // doing it one creature at a time, so it is modelled on the existing `repeatCount`
        // batch-activation path rather than a rules change.
        context("Station multi-select shortcut") {

            test("the legal action advertises a batch cap equal to the number of tappable creatures") {
                val game = scenario()
                    .withPlayers()
                    .withCardOnBattlefield(1, "Wedgelight Rammer", summoningSickness = false)
                    .withCardOnBattlefield(1, "Spined Wurm", summoningSickness = false) // 5/4
                    .withCardOnBattlefield(1, "Test Bulwark", summoningSickness = false) // 1/5
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val rammer = game.findPermanent("Wedgelight Rammer")!!
                val stationAction = game.getLegalActions(1).single {
                    (it.action as? ActivateAbility)?.let { a -> a.sourceId == rammer } == true
                }
                withClue("two other untapped creatures ⇒ up to two activations can be queued at once") {
                    stationAction.additionalCostInfo?.tapBatchMaxActivations shouldBe 2
                }
            }

            test("selecting two creatures queues two station activations, each charging by its own creature's power") {
                val game = scenario()
                    .withPlayers()
                    .withCardOnBattlefield(1, "Wedgelight Rammer", summoningSickness = false)
                    .withCardOnBattlefield(1, "Spined Wurm", summoningSickness = false) // power 5
                    .withCardOnBattlefield(1, "Test Bulwark", summoningSickness = false) // power 1
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val rammer = game.findPermanent("Wedgelight Rammer")!!
                val wurm = game.findPermanent("Spined Wurm")!!
                val wall = game.findPermanent("Test Bulwark")!!

                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = rammer,
                        abilityId = stationAbilityId("Wedgelight Rammer"),
                        costPayment = AdditionalCostPayment(tappedPermanents = listOf(wurm, wall)),
                        repeatCount = 2
                    )
                )
                withClue("batch station activation should succeed: ${result.error}") { result.error shouldBe null }
                withClue("one ability per chosen creature is on the stack") { game.state.stack.size shouldBe 2 }
                withClue("both chosen creatures are tapped to pay") {
                    game.state.getEntity(wurm)?.has<TappedComponent>() shouldBe true
                    game.state.getEntity(wall)?.has<TappedComponent>() shouldBe true
                }

                game.resolveStack()

                // 5 (Wurm) + 1 (Bulwark) = 6. A single shared snapshot would give 10 or 2; the
                // distinct total proves each queued activation read its own creature's power.
                withClue("charge = sum of each tapped creature's power (5 + 1)") {
                    game.state.getEntity(rammer)?.get<CountersComponent>()?.getCount(CounterType.CHARGE) shouldBe 6
                }
            }

            test("picking a single creature still works while the batch shortcut is available (back-compat)") {
                val game = scenario()
                    .withPlayers()
                    .withCardOnBattlefield(1, "Wedgelight Rammer", summoningSickness = false)
                    .withCardOnBattlefield(1, "Spined Wurm", summoningSickness = false) // power 5
                    .withCardOnBattlefield(1, "Test Bulwark", summoningSickness = false) // power 1
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val rammer = game.findPermanent("Wedgelight Rammer")!!
                val wurm = game.findPermanent("Spined Wurm")!!
                val wall = game.findPermanent("Test Bulwark")!!

                game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = rammer,
                        abilityId = stationAbilityId("Wedgelight Rammer"),
                        costPayment = AdditionalCostPayment(tappedPermanents = listOf(wurm))
                    )
                ).error shouldBe null
                game.resolveStack()

                game.state.getEntity(rammer)?.get<CountersComponent>()?.getCount(CounterType.CHARGE) shouldBe 5
                withClue("the unchosen creature is left untapped") {
                    game.state.getEntity(wall)?.has<TappedComponent>() shouldBe false
                }
            }

            test("a batch whose creature count does not match repeatCount is rejected") {
                val game = scenario()
                    .withPlayers()
                    .withCardOnBattlefield(1, "Wedgelight Rammer", summoningSickness = false)
                    .withCardOnBattlefield(1, "Spined Wurm", summoningSickness = false)
                    .withCardOnBattlefield(1, "Test Bulwark", summoningSickness = false)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val rammer = game.findPermanent("Wedgelight Rammer")!!
                val wurm = game.findPermanent("Spined Wurm")!!

                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = rammer,
                        abilityId = stationAbilityId("Wedgelight Rammer"),
                        costPayment = AdditionalCostPayment(tappedPermanents = listOf(wurm)), // only 1
                        repeatCount = 2 // but asked for 2 activations
                    )
                )
                withClue("one creature can't pay for two activations") { result.error shouldNotBe null }
            }

            test("a batch cannot tap the same creature for more than one activation") {
                val game = scenario()
                    .withPlayers()
                    .withCardOnBattlefield(1, "Wedgelight Rammer", summoningSickness = false)
                    .withCardOnBattlefield(1, "Spined Wurm", summoningSickness = false)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val rammer = game.findPermanent("Wedgelight Rammer")!!
                val wurm = game.findPermanent("Spined Wurm")!!

                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = rammer,
                        abilityId = stationAbilityId("Wedgelight Rammer"),
                        costPayment = AdditionalCostPayment(tappedPermanents = listOf(wurm, wurm)),
                        repeatCount = 2
                    )
                )
                withClue("the same creature can't be tapped twice in one batch") { result.error shouldNotBe null }
            }
        }
    }
}
