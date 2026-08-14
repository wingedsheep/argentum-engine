package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.dft.cards.BoomScholar
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AbilityId
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.TimingRule
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Boom Scholar (DFT #189) — {1}{R}{G} Goblin Advisor, 3/3.
 *
 * "Exhaust abilities of other permanents you control cost {2} less to activate." — the new
 * `ReduceActivatedAbilityCost(exhaustOnly = true)` qualifier, which gates on the *ability*
 * (`isExhaust`, CR 702.177) rather than the permanent. Asserted behaviourally: each board is given
 * *exactly* the mana the reduced (or unreduced) cost needs, so "is this activation affordable" is the
 * assertion — no reliance on a displayed cost string.
 *  - another permanent's exhaust ability is {2} cheaper, and its ordinary activated ability is not,
 *  - Boom Scholar's own exhaust ability is not ("**other** permanents", via `excludeSelf`),
 *  - an opponent's exhaust ability is not ("you control"),
 *  - only generic pips are reduced (CR 118.7),
 *  - two Scholars stack additively down to {0} (2025-02-07 ruling).
 *
 * "Exhaust — {4}{R}{G}: Creatures and Vehicles you control gain trample until end of turn. Put two
 * +1/+1 counters on this creature." — the union filter reaches an uncrewed Vehicle (a noncreature
 * artifact) as well as creatures, and does not touch an opponent's creature.
 */
class BoomScholarScenarioTest : ScenarioTestBase() {

    private val exhaustAbilityId = BoomScholar.activatedAbilities.single { it.isExhaust }.id

    init {
        // A permanent with BOTH an exhaust ability and an ordinary activated ability at the same
        // printed {3} cost, so one card proves the qualifier discriminates by ability, not by source.
        cardRegistry.register(
            card("Boom Test Rig") {
                manaCost = "{2}"
                typeLine = "Artifact"
                oracleText = "Exhaust — {3}: You gain 1 life.\n{3}: You gain 2 life."
                activatedAbility {
                    cost = Costs.Mana("{3}")
                    isExhaust = true
                    effect = Effects.GainLife(1)
                    description = "You gain 1 life."
                }
                activatedAbility {
                    cost = Costs.Mana("{3}")
                    effect = Effects.GainLife(2)
                    timing = TimingRule.InstantSpeed
                    description = "{3}: You gain 2 life."
                }
            }
        )
        // A colored exhaust cost, to prove only the generic portion is reduced (CR 118.7).
        cardRegistry.register(
            card("Boom Test Colored Rig") {
                manaCost = "{2}"
                typeLine = "Artifact"
                oracleText = "Exhaust — {2}{R}{R}: You gain 1 life."
                activatedAbility {
                    cost = Costs.Mana("{2}{R}{R}")
                    isExhaust = true
                    effect = Effects.GainLife(1)
                    description = "You gain 1 life."
                }
            }
        )

        // A plain Vehicle — no abilities beyond crew, so activating Boom Scholar's exhaust doesn't
        // set off anything else (Rangers' Refueler, for instance, draws on every exhaust activation).
        cardRegistry.register(
            card("Boom Test Wagon") {
                manaCost = "{2}"
                typeLine = "Artifact — Vehicle"
                power = 4
                toughness = 4
                oracleText = "Crew 2"
                keywordAbility(KeywordAbility.crew(2))
            }
        )

        fun rigExhaustId(): AbilityId =
            cardRegistry.getCard("Boom Test Rig")!!.activatedAbilities.single { it.isExhaust }.id

        fun rigPlainId(): AbilityId =
            cardRegistry.getCard("Boom Test Rig")!!.activatedAbilities.single { !it.isExhaust }.id

        /** Whether the engine currently offers [abilityId] on [source] as an affordable activation. */
        fun TestGame.canActivate(playerNumber: Int, source: EntityId, abilityId: AbilityId): Boolean =
            getLegalActions(playerNumber).any { info ->
                info.isAffordable &&
                    (info.action as? ActivateAbility)?.let { it.sourceId == source && it.abilityId == abilityId } == true
            }

        /**
         * A board with [scholars] Boom Scholars, a Boom Test Rig, and exactly [mountains] Mountains —
         * so affordability alone reports how far the discount reached.
         */
        fun rigBoard(scholars: Int, mountains: Int): TestGame {
            var builder = scenario().withPlayers("Alice", "Bob")
            repeat(scholars) {
                builder = builder.withCardOnBattlefield(1, "Boom Scholar", summoningSickness = false)
            }
            builder = builder.withCardOnBattlefield(1, "Boom Test Rig")
            if (mountains > 0) builder = builder.withLandsOnBattlefield(1, "Mountain", mountains)
            return builder
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
        }

        context("Boom Scholar") {

            test("another permanent's exhaust ability costs {2} less — {3} becomes payable with {1}") {
                val game = rigBoard(scholars = 1, mountains = 1)
                val rig = game.findPermanent("Boom Test Rig")!!

                withClue("one mana covers the reduced {3} exhaust cost") {
                    game.canActivate(1, rig, rigExhaustId()) shouldBe true
                }

                val lifeBefore = game.getLifeTotal(1)
                game.execute(ActivateAbility(game.player1Id, rig, rigExhaustId())).error shouldBe null
                if (game.hasPendingDecision()) game.submitManaSourcesAutoPay()
                game.resolveStack()
                withClue("and it really resolves") { game.getLifeTotal(1) shouldBe lifeBefore + 1 }
            }

            test("the SAME permanent's ordinary activated ability is not reduced") {
                val game = rigBoard(scholars = 1, mountains = 1)
                val rig = game.findPermanent("Boom Test Rig")!!

                withClue("the non-exhaust {3} ability is unaffordable with one mana") {
                    game.canActivate(1, rig, rigPlainId()) shouldBe false
                }
                withClue("with three mana it becomes affordable — proving nothing else blocks it") {
                    rigBoard(scholars = 1, mountains = 3).let { g ->
                        g.canActivate(1, g.findPermanent("Boom Test Rig")!!, rigPlainId()) shouldBe true
                    }
                }
            }

            test("Boom Scholar's own exhaust ability is not discounted (\"other permanents\")") {
                fun board(mountains: Int, forests: Int): TestGame = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardOnBattlefield(1, "Boom Scholar", summoningSickness = false)
                    .withLandsOnBattlefield(1, "Mountain", mountains)
                    .withLandsOnBattlefield(1, "Forest", forests)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // A self-discount would make {4}{R}{G} cost {2}{R}{G} — four mana.
                val four = board(mountains = 3, forests = 1)
                withClue("four mana must NOT be enough: the Scholar can't discount itself") {
                    four.canActivate(1, four.findPermanent("Boom Scholar")!!, exhaustAbilityId) shouldBe false
                }
                val six = board(mountains = 5, forests = 1)
                withClue("the full {4}{R}{G} — six mana — is affordable") {
                    six.canActivate(1, six.findPermanent("Boom Scholar")!!, exhaustAbilityId) shouldBe true
                }
            }

            test("an opponent's exhaust ability is not discounted (\"you control\")") {
                fun board(mountains: Int): TestGame = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardOnBattlefield(1, "Boom Scholar", summoningSickness = false)
                    .withCardOnBattlefield(2, "Boom Test Rig")
                    .withLandsOnBattlefield(2, "Mountain", mountains)
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val one = board(1)
                withClue("Alice's Scholar must not subsidise Bob's exhaust ability") {
                    one.canActivate(2, one.findPermanent("Boom Test Rig")!!, rigExhaustId()) shouldBe false
                }
                val three = board(3)
                withClue("Bob pays the printed {3} himself") {
                    three.canActivate(2, three.findPermanent("Boom Test Rig")!!, rigExhaustId()) shouldBe true
                }
            }

            test("only the generic portion is reduced (CR 118.7)") {
                fun board(mountains: Int): TestGame = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardOnBattlefield(1, "Boom Scholar", summoningSickness = false)
                    .withCardOnBattlefield(1, "Boom Test Colored Rig")
                    .withLandsOnBattlefield(1, "Mountain", mountains)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val id = cardRegistry.getCard("Boom Test Colored Rig")!!.activatedAbilities.single().id
                // {2}{R}{R} loses its two generic pips → {R}{R}, so two Mountains suffice…
                val two = board(2)
                withClue("{2}{R}{R} reduces to {R}{R}") {
                    two.canActivate(1, two.findPermanent("Boom Test Colored Rig")!!, id) shouldBe true
                }
                // …but not one: the two red pips survive the reduction.
                val one = board(1)
                withClue("the two colored pips are untouched, so one mana is not enough") {
                    one.canActivate(1, one.findPermanent("Boom Test Colored Rig")!!, id) shouldBe false
                }
            }

            test("two Scholars stack additively and can take the cost to {0} (2025-02-07 ruling)") {
                val one = rigBoard(scholars = 1, mountains = 0)
                withClue("one Scholar leaves {1}, unaffordable with no mana sources") {
                    one.canActivate(1, one.findPermanent("Boom Test Rig")!!, rigExhaustId()) shouldBe false
                }
                val two = rigBoard(scholars = 2, mountains = 0)
                withClue("two Scholars reduce {3} past zero — the ability is free") {
                    two.canActivate(1, two.findPermanent("Boom Test Rig")!!, rigExhaustId()) shouldBe true
                }
            }

            test("exhaust grants trample to creatures and Vehicles you control, and adds two counters") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardOnBattlefield(1, "Boom Scholar", summoningSickness = false)
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    // An uncrewed Vehicle: a noncreature artifact that must still gain trample.
                    .withCardOnBattlefield(1, "Boom Test Wagon")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Mountain", 5)
                    .withLandsOnBattlefield(1, "Forest", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val scholar = game.findPermanent("Boom Scholar")!!
                game.execute(ActivateAbility(game.player1Id, scholar, exhaustAbilityId)).error shouldBe null
                if (game.hasPendingDecision()) game.submitManaSourcesAutoPay()
                game.resolveStack()

                val projected = StateProjector().project(game.state)
                val myBear = game.findAllPermanents("Grizzly Bears")
                    .first { projected.getController(it) == game.player1Id }
                val theirBear = game.findAllPermanents("Grizzly Bears")
                    .first { projected.getController(it) == game.player2Id }
                val vehicle = game.findPermanent("Boom Test Wagon")!!

                withClue("your creature gains trample") {
                    projected.hasKeyword(myBear, Keyword.TRAMPLE) shouldBe true
                }
                withClue("your uncrewed Vehicle gains trample too") {
                    projected.hasKeyword(vehicle, Keyword.TRAMPLE) shouldBe true
                }
                withClue("Boom Scholar is itself a creature you control") {
                    projected.hasKeyword(scholar, Keyword.TRAMPLE) shouldBe true
                }
                withClue("the opponent's creature does not") {
                    projected.hasKeyword(theirBear, Keyword.TRAMPLE) shouldBe false
                }
                withClue("two +1/+1 counters land on Boom Scholar: a 3/3 becomes a 5/5") {
                    projected.getPower(scholar) shouldBe 5
                    projected.getToughness(scholar) shouldBe 5
                }
            }

            test("the exhaust ability may be activated only once") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardOnBattlefield(1, "Boom Scholar", summoningSickness = false)
                    .withLandsOnBattlefield(1, "Mountain", 10)
                    .withLandsOnBattlefield(1, "Forest", 4)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val scholar = game.findPermanent("Boom Scholar")!!
                game.execute(ActivateAbility(game.player1Id, scholar, exhaustAbilityId)).error shouldBe null
                if (game.hasPendingDecision()) game.submitManaSourcesAutoPay()
                game.resolveStack()

                withClue("a second activation is rejected even with mana to spare") {
                    game.execute(
                        ActivateAbility(game.player1Id, scholar, exhaustAbilityId)
                    ).error shouldNotBe null
                }
            }
        }
    }
}
