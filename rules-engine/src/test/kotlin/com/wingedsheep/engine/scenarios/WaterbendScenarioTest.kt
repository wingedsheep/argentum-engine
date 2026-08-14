package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.scripting.AlternativePaymentChoice
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario tests for the Waterbend cost (Avatar: The Last Airbender).
 *
 * Waterbend {N} means: "Pay {N}. For each generic mana in that cost, you may tap an untapped
 * artifact or creature you control rather than pay that mana." It is modelled on activated
 * abilities by [com.wingedsheep.sdk.scripting.ActivatedAbility.hasWaterbend] + the
 * [AlternativePaymentChoice.tapForGenericPermanents] carrier, mirroring Convoke but generic-only
 * and widened to artifacts.
 *
 * Card used (defined inline — no real TLA waterbend card is registered in the test pool):
 * - "Waterbend Tester" — Creature with "Waterbend {4}: Draw a card."
 *
 * Rules pinned (from the official waterbend reminder + rulings):
 *  1. The cost can be paid entirely with mana.
 *  2. Each tapped artifact/creature pays {1} of the generic — both types qualify.
 *  3. Tapping may be partial (mix mana + taps).
 *  4. You may tap a creature that just came under your control (no summoning-sickness gate).
 *  5. You can't tap more permanents than the generic mana in the cost.
 *  6. The legal action surfaces only untapped permanents you control (creatures + artifacts).
 */
class WaterbendScenarioTest : ScenarioTestBase() {

    init {
        val waterbender = card("Waterbend Tester") {
            manaCost = "{1}{U}"
            colorIdentity = "U"
            typeLine = "Creature — Wizard"
            power = 1
            toughness = 1
            oracleText = "Waterbend {4}: Draw a card."
            activatedAbility {
                cost = Costs.Mana("{4}")
                hasWaterbend = true
                effect = Effects.DrawCards(1)
            }
        }
        cardRegistry.register(waterbender)

        // A vanilla artifact used to prove artifacts can be tapped for waterbend.
        val trinket = card("Waterbend Trinket") {
            manaCost = "{1}"
            colorIdentity = ""
            typeLine = "Artifact"
            oracleText = ""
        }
        cardRegistry.register(trinket)

        fun abilityId() = cardRegistry.getCard("Waterbend Tester")!!.script.activatedAbilities.first().id

        context("Waterbend cost on an activated ability") {

            test("can pay the whole waterbend cost with mana") {
                val game = scenario()
                    .withPlayers("P1", "P2")
                    .withCardOnBattlefield(1, "Waterbend Tester")
                    .withLandsOnBattlefield(1, "Island", 4)
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(2, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val before = game.handSize(1)
                val sourceId = game.findPermanent("Waterbend Tester")!!

                val result = game.execute(
                    ActivateAbility(playerId = game.player1Id, sourceId = sourceId, abilityId = abilityId())
                )
                withClue("activating with full mana should succeed: ${result.error}") {
                    result.error shouldBe null
                }
                game.resolveStack()
                game.handSize(1) shouldBe before + 1
            }

            test("tapping creatures pays the generic cost (no mana needed)") {
                val game = scenario()
                    .withPlayers("P1", "P2")
                    .withCardOnBattlefield(1, "Waterbend Tester")
                    .withCardOnBattlefield(1, "Glory Seeker")
                    .withCardOnBattlefield(1, "Glory Seeker")
                    .withCardOnBattlefield(1, "Glory Seeker")
                    .withCardOnBattlefield(1, "Glory Seeker")
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(2, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val before = game.handSize(1)
                val sourceId = game.findPermanent("Waterbend Tester")!!
                val creatures = game.findAllPermanents("Glory Seeker")
                creatures.size shouldBe 4

                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = sourceId,
                        abilityId = abilityId(),
                        alternativePayment = AlternativePaymentChoice(tapForGenericPermanents = creatures.toSet())
                    )
                )
                withClue("activating by tapping 4 creatures should succeed: ${result.error}") {
                    result.error shouldBe null
                }
                game.resolveStack()
                game.handSize(1) shouldBe before + 1
                withClue("all four tapped creatures should be tapped") {
                    creatures.all { game.state.getEntity(it)!!.has<TappedComponent>() } shouldBe true
                }
            }

            test("artifacts qualify for waterbend taps") {
                val game = scenario()
                    .withPlayers("P1", "P2")
                    .withCardOnBattlefield(1, "Waterbend Tester")
                    .withCardOnBattlefield(1, "Waterbend Trinket")
                    .withCardOnBattlefield(1, "Waterbend Trinket")
                    .withCardOnBattlefield(1, "Waterbend Trinket")
                    .withCardOnBattlefield(1, "Waterbend Trinket")
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(2, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val before = game.handSize(1)
                val sourceId = game.findPermanent("Waterbend Tester")!!
                val artifacts = game.findAllPermanents("Waterbend Trinket")
                artifacts.size shouldBe 4

                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = sourceId,
                        abilityId = abilityId(),
                        alternativePayment = AlternativePaymentChoice(tapForGenericPermanents = artifacts.toSet())
                    )
                )
                withClue("activating by tapping 4 artifacts should succeed: ${result.error}") {
                    result.error shouldBe null
                }
                game.resolveStack()
                game.handSize(1) shouldBe before + 1
                artifacts.all { game.state.getEntity(it)!!.has<TappedComponent>() } shouldBe true
            }

            test("mix of mana and taps pays the cost") {
                val game = scenario()
                    .withPlayers("P1", "P2")
                    .withCardOnBattlefield(1, "Waterbend Tester")
                    .withCardOnBattlefield(1, "Glory Seeker")
                    .withCardOnBattlefield(1, "Waterbend Trinket")
                    .withLandsOnBattlefield(1, "Island", 2)
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(2, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val before = game.handSize(1)
                val sourceId = game.findPermanent("Waterbend Tester")!!
                val creature = game.findPermanent("Glory Seeker")!!
                val artifact = game.findPermanent("Waterbend Trinket")!!

                // {4} = 2 mana (from 2 Islands) + tap 1 creature + tap 1 artifact.
                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = sourceId,
                        abilityId = abilityId(),
                        alternativePayment = AlternativePaymentChoice(tapForGenericPermanents = setOf(creature, artifact))
                    )
                )
                withClue("mixed mana + waterbend payment should succeed: ${result.error}") {
                    result.error shouldBe null
                }
                game.resolveStack()
                game.handSize(1) shouldBe before + 1
                game.state.getEntity(creature)!!.has<TappedComponent>() shouldBe true
                game.state.getEntity(artifact)!!.has<TappedComponent>() shouldBe true
            }

            test("cannot tap more permanents than the generic in the cost") {
                val game = scenario()
                    .withPlayers("P1", "P2")
                    .withCardOnBattlefield(1, "Waterbend Tester")
                    .withCardOnBattlefield(1, "Glory Seeker")
                    .withCardOnBattlefield(1, "Glory Seeker")
                    .withCardOnBattlefield(1, "Glory Seeker")
                    .withCardOnBattlefield(1, "Glory Seeker")
                    .withCardOnBattlefield(1, "Glory Seeker")
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(2, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val before = game.handSize(1)
                val sourceId = game.findPermanent("Waterbend Tester")!!
                val creatures = game.findAllPermanents("Glory Seeker")
                creatures.size shouldBe 5

                // Offer 5 for a {4} cost — only 4 should be tapped (CR: "can't tap more than the
                // amount of generic mana in the waterbend cost"); the 5th pays for nothing.
                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = sourceId,
                        abilityId = abilityId(),
                        alternativePayment = AlternativePaymentChoice(tapForGenericPermanents = creatures.toSet())
                    )
                )
                withClue("activating should succeed: ${result.error}") { result.error shouldBe null }
                game.resolveStack()
                game.handSize(1) shouldBe before + 1
                val tapped = creatures.count { game.state.getEntity(it)!!.has<TappedComponent>() }
                withClue("exactly 4 of the 5 creatures pay the {4} cost (can't tap more than the generic)") {
                    tapped shouldBe 4
                }
            }

            // Regression: floating mana in the pool must count toward an Explicit payment.
            // The client always routes a waterbend (or convoke) activation through Explicit payment
            // (the mana-source pipeline phase is forced whenever an alternative-payment phase runs),
            // and the enumerator deems the ability affordable counting pool + sources. The Explicit
            // execution branch used to solve the whole cost from the tapped sources alone, ignoring
            // the pool — so mana already floating was stranded ("not used and not available"), and a
            // legal activation could even fail with "Selected mana sources cannot pay this ability's
            // cost". These two tests pin the pool-first behaviour (parity with the auto-tap branch
            // and CastPaymentProcessor.autoPay).
            test("floating mana pays part of an Explicit waterbend-ability cost (pool not stranded)") {
                val game = scenario()
                    .withPlayers("P1", "P2")
                    .withCardOnBattlefield(1, "Waterbend Tester")
                    .withLandsOnBattlefield(1, "Island", 2)
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(2, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Two mana already floating; the {4} cost is 2 from the pool + 2 from the two Islands.
                game.state = game.state.updateEntity(game.player1Id) { c ->
                    c.with(ManaPoolComponent(blue = 2))
                }

                val before = game.handSize(1)
                val sourceId = game.findPermanent("Waterbend Tester")!!
                val islands = game.findAllPermanents("Island")

                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = sourceId,
                        abilityId = abilityId(),
                        paymentStrategy = PaymentStrategy.Explicit(islands),
                    )
                )
                withClue("floating mana should cover the rest of the Explicit payment: ${result.error}") {
                    result.error shouldBe null
                }
                game.resolveStack()
                game.handSize(1) shouldBe before + 1
                withClue("the floating pool is spent, not stranded") {
                    game.state.getEntity(game.player1Id)!!.get<ManaPoolComponent>()!!.blue shouldBe 0
                }
                withClue("both Islands were tapped for the remaining {2}") {
                    islands.all { game.state.getEntity(it)!!.has<TappedComponent>() } shouldBe true
                }
            }

            test("floating mana plus waterbend taps pay the cost (pool spent, no sources tapped)") {
                val game = scenario()
                    .withPlayers("P1", "P2")
                    .withCardOnBattlefield(1, "Waterbend Tester")
                    .withCardOnBattlefield(1, "Glory Seeker")
                    .withCardOnBattlefield(1, "Glory Seeker")
                    .withLandsOnBattlefield(1, "Island", 2) // available but should NOT be needed
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(2, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Two mana floating; tap 2 creatures for waterbend → {4} = 2 taps + 2 pool.
                game.state = game.state.updateEntity(game.player1Id) { c ->
                    c.with(ManaPoolComponent(blue = 2))
                }

                val before = game.handSize(1)
                val sourceId = game.findPermanent("Waterbend Tester")!!
                val creatures = game.findAllPermanents("Glory Seeker")
                val islands = game.findAllPermanents("Island")

                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = sourceId,
                        abilityId = abilityId(),
                        paymentStrategy = PaymentStrategy.Explicit(emptyList()),
                        alternativePayment = AlternativePaymentChoice(tapForGenericPermanents = creatures.toSet())
                    )
                )
                withClue("waterbend taps + floating mana should pay the whole cost: ${result.error}") {
                    result.error shouldBe null
                }
                game.resolveStack()
                game.handSize(1) shouldBe before + 1
                withClue("the floating pool is spent") {
                    game.state.getEntity(game.player1Id)!!.get<ManaPoolComponent>()!!.blue shouldBe 0
                }
                withClue("both creatures were tapped for waterbend") {
                    creatures.all { game.state.getEntity(it)!!.has<TappedComponent>() } shouldBe true
                }
                withClue("no Island needed to be tapped — the pool covered the non-waterbend remainder") {
                    islands.none { game.state.getEntity(it)!!.has<TappedComponent>() } shouldBe true
                }
            }

            test("legal action surfaces hasTapForGeneric and only untapped controlled permanents") {
                val game = scenario()
                    .withPlayers("P1", "P2")
                    .withCardOnBattlefield(1, "Waterbend Tester")
                    .withCardOnBattlefield(1, "Glory Seeker")       // eligible creature
                    .withCardOnBattlefield(1, "Waterbend Trinket")  // eligible artifact
                    .withCardOnBattlefield(2, "Glory Seeker")       // opponent's — not eligible
                    .withLandsOnBattlefield(1, "Island", 4)
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(2, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val sourceId = game.findPermanent("Waterbend Tester")!!
                val action = game.getLegalActions(1).find {
                    it.actionType == "ActivateAbility" &&
                        (it.action as? ActivateAbility)?.sourceId == sourceId &&
                        it.isAffordable
                }

                withClue("waterbend ability should be offered with waterbend info") {
                    action shouldNotBe null
                    action!!.hasTapForGeneric shouldBe true
                    action.validTapForGenericPermanents shouldNotBe null
                }
                // The source creature itself is also untapped & eligible, so the pool is: the tester,
                // the controlled Glory Seeker, and the Trinket — but NOT the opponent's creature.
                val myCreature = game.findAllPermanents("Glory Seeker").first {
                    game.state.getEntity(it)!!.get<ControllerComponent>()?.playerId == game.player1Id
                }
                val myArtifact = game.findPermanent("Waterbend Trinket")!!
                val offered = action!!.validTapForGenericPermanents!!.map { it.entityId }.toSet()
                withClue("only the controller's untapped artifacts/creatures are offered") {
                    offered shouldContainExactlyInAnyOrder setOf(sourceId, myCreature, myArtifact)
                }
            }
        }
    }
}
