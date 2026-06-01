package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.ActionProcessor
import com.wingedsheep.engine.core.GameConfig
import com.wingedsheep.engine.core.GameInitializer
import com.wingedsheep.engine.core.KeepHand
import com.wingedsheep.engine.core.PlayerConfig
import com.wingedsheep.engine.core.SubmitDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.core.YesNoResponse
import com.wingedsheep.engine.handlers.effects.LifeGainModifiers
import com.wingedsheep.engine.mechanics.layers.ContinuousEffectSourceComponent
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.ReplacementEffectSourceComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.LifeTotalComponent
import com.wingedsheep.engine.state.components.player.MulliganStateComponent
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Scenario tests for Leyline of Hope (DSK #18).
 *
 * "{2}{W}{W} Enchantment
 *  If this card is in your opening hand, you may begin the game with it on the battlefield.
 *  If you would gain life, you gain that much life plus 1 instead.
 *  As long as you have at least 7 life more than your starting life total,
 *    creatures you control get +2/+2."
 */
class LeylineOfHopeScenarioTest : ScenarioTestBase() {

    // A minimal sorcery that gives the caster 3 life. Used to drive [ModifyLifeGain].
    private val gainThreeLife = card("Test Gain Three") {
        manaCost = "{W}"
        typeLine = "Sorcery"
        spell { effect = Effects.GainLife(3) }
    }

    // A vanilla 1/1 creature so we can observe the conditional anthem applying / lifting.
    private val testGrunt = card("Test Grunt") {
        manaCost = "{W}"
        typeLine = "Creature — Soldier"
        power = 1
        toughness = 1
    }

    private val stateProjector = StateProjector()

    init {
        cardRegistry.register(gainThreeLife)
        cardRegistry.register(testGrunt)

        context("Leyline of Hope") {

            test("life gain is increased by 1 while Leyline of Hope is on the battlefield") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Leyline of Hope")
                    .withCardInHand(1, "Test Gain Three")
                    .withLandsOnBattlefield(1, "Plains", 1)
                    .withLifeTotal(1, 20)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val cast = game.castSpell(1, "Test Gain Three")
                withClue("Casting the test spell should succeed: ${cast.error}") {
                    cast.error shouldBe null
                }
                game.resolveStack()

                withClue("Gaining 3 life with Leyline of Hope should net 4") {
                    game.getLifeTotal(1) shouldBe 24
                }
            }

            test("combat lifelink life gain is increased by 1 while Leyline of Hope is on the battlefield") {
                // Regression: combat lifelink used to bypass LifeGainModifiers, so Leyline of
                // Hope's +1 only applied to spell-driven gainLife and noncombat lifelink.
                // Healer's Hawk (1/1 flying lifelink) attacking unblocked should gain its
                // controller 1 + 1 = 2 life with Leyline of Hope out.
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Leyline of Hope")
                    .withCardOnBattlefield(1, "Healer's Hawk", summoningSickness = false)
                    .withLifeTotal(1, 20)
                    .withLifeTotal(2, 20)
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                game.declareAttackers(mapOf("Healer's Hawk" to 2))
                game.passUntilPhase(Phase.COMBAT, Step.COMBAT_DAMAGE)
                var iterations = 0
                while (game.state.step != Step.POSTCOMBAT_MAIN && iterations++ < 20) {
                    game.passPriority()
                }

                withClue("Healer's Hawk (1/1 lifelink) deals 1 combat damage to the defender") {
                    game.getLifeTotal(2) shouldBe 19
                }
                withClue("Lifelink life gain should run through Leyline of Hope: 1 + 1 = 2") {
                    game.getLifeTotal(1) shouldBe 22
                }
            }

            test("multiple Leylines of Hope stack additively") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Leyline of Hope")
                    .withCardOnBattlefield(1, "Leyline of Hope")
                    .withCardInHand(1, "Test Gain Three")
                    .withLandsOnBattlefield(1, "Plains", 1)
                    .withLifeTotal(1, 20)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val cast = game.castSpell(1, "Test Gain Three")
                withClue("Casting the test spell should succeed: ${cast.error}") {
                    cast.error shouldBe null
                }
                game.resolveStack()

                // Two Leylines → +1 +1 → 3 + 2 = 5 gained.
                withClue("Two Leylines of Hope should add 2 to every life gain") {
                    game.getLifeTotal(1) shouldBe 25
                }
            }

            test("the +1 only applies to its controller, not opponents") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Leyline of Hope")
                    .withCardInHand(2, "Test Gain Three")
                    .withLandsOnBattlefield(2, "Plains", 1)
                    .withLifeTotal(2, 20)
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val cast = game.castSpell(2, "Test Gain Three")
                withClue("Casting the test spell should succeed: ${cast.error}") {
                    cast.error shouldBe null
                }
                game.resolveStack()

                withClue("Opponent's life gain should be unaffected by player 1's Leyline of Hope") {
                    game.getLifeTotal(2) shouldBe 23
                }
            }

            test("creatures you control get +2/+2 once you reach 7 life above starting") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Leyline of Hope")
                    .withCardOnBattlefield(1, "Test Grunt", summoningSickness = false)
                    .withLifeTotal(1, 27)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val gruntId = game.findPermanent("Test Grunt")!!
                val projected = stateProjector.project(game.state)
                withClue("At life=27 (starting 20 + 7) the anthem should be active") {
                    projected.getPower(gruntId) shouldBe 3
                    projected.getToughness(gruntId) shouldBe 3
                }
            }

            test("the anthem does NOT apply when life is below starting + 7") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Leyline of Hope")
                    .withCardOnBattlefield(1, "Test Grunt", summoningSickness = false)
                    .withLifeTotal(1, 26)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val gruntId = game.findPermanent("Test Grunt")!!
                val projected = stateProjector.project(game.state)
                withClue("At life=26 the anthem should be inactive (need ≥27)") {
                    projected.getPower(gruntId) shouldBe 1
                    projected.getToughness(gruntId) shouldBe 1
                }
            }

            test("opening-hand leyline: yes puts the card onto the battlefield before turn 1") {
                // Build a real game via GameInitializer so the mulligan → leyline → turn 1
                // hand-off goes through the production code path.
                val plainsHeavy = Deck.of("Leyline of Hope" to 1, "Plains" to 59)
                val deck = Deck.of("Plains" to 60)
                val initializer = GameInitializer(cardRegistry)
                val init = initializer.initializeGame(
                    GameConfig(
                        players = listOf(
                            PlayerConfig("P1", plainsHeavy),
                            PlayerConfig("P2", deck)
                        ),
                        startingPlayerIndex = 0,
                        // Force the Leyline into the opening hand by drawing the whole deck;
                        // the leyline scan only inspects the opening hand.
                        startingHandSize = 60
                    )
                )
                val processor = ActionProcessor(cardRegistry)
                var state = init.state
                val p1Id = init.playerIds[0]
                val p2Id = init.playerIds[1]

                // Both players keep their hands without mulligans.
                state = processor.process(state, KeepHand(p1Id)).result.let {
                    withClue("P1 KeepHand should succeed: ${it.error}") { it.error shouldBe null }
                    it.state
                }
                val afterP2Keep = processor.process(state, KeepHand(p2Id)).result
                withClue("P2 KeepHand should pause for the leyline yes/no: ${afterP2Keep.error}") {
                    afterP2Keep.error shouldBe null
                }
                state = afterP2Keep.state

                val leylineDecision = state.pendingDecision
                leylineDecision.shouldBeInstanceOf<YesNoDecision>()
                withClue("The leyline prompt should be addressed to the player who owns the leyline (P1)") {
                    leylineDecision.playerId shouldBe p1Id
                }

                // Answer yes — Leyline of Hope should move to P1's battlefield.
                val afterYes = processor.process(
                    state,
                    SubmitDecision(p1Id, YesNoResponse(leylineDecision.id, true))
                ).result
                withClue("Submitting yes should succeed: ${afterYes.error}") {
                    afterYes.error shouldBe null
                }
                state = afterYes.state

                val battlefield = state.getZone(ZoneKey(p1Id, Zone.BATTLEFIELD))
                val leylineId = battlefield.firstOrNull { id ->
                    state.getEntity(id)?.get<CardComponent>()?.name == "Leyline of Hope"
                }
                withClue("Leyline of Hope should be on the battlefield after the yes answer") {
                    leylineId shouldNotBe null
                }

                withClue("Pending decision should be cleared once the leyline phase finishes") {
                    state.pendingDecision shouldBe null
                }

                val mulliganState = state.getEntity(p1Id)?.get<MulliganStateComponent>()
                withClue("P1's pending leyline list should be empty") {
                    mulliganState?.pendingLeylineCardIds shouldBe emptyList()
                }

                // Regression for leyline-yes wiring: the static ability + replacement effect
                // components must be attached so the anthem reaches the projector and the +1
                // life rider reaches the replacement-application path. Without these the
                // permanent is on the battlefield in name only.
                val leylineEntity = state.getEntity(leylineId!!)
                withClue("Leyline of Hope must carry ContinuousEffectSourceComponent so its anthem reaches the projector") {
                    leylineEntity?.get<ContinuousEffectSourceComponent>() shouldNotBe null
                }
                withClue("Leyline of Hope must carry ReplacementEffectSourceComponent so its +1 life rider fires") {
                    leylineEntity?.get<ReplacementEffectSourceComponent>() shouldNotBe null
                }
                withClue("LifeGainModifiers should add 1 to a 3-life gain when Leyline of Hope is on P1's battlefield") {
                    LifeGainModifiers.apply(state, p1Id, 3) shouldBe 4
                }
            }

            test("opening-hand leyline: no leaves the card in hand and advances to turn 1") {
                val plainsHeavy = Deck.of("Leyline of Hope" to 1, "Plains" to 59)
                val deck = Deck.of("Plains" to 60)
                val initializer = GameInitializer(cardRegistry)
                val init = initializer.initializeGame(
                    GameConfig(
                        players = listOf(
                            PlayerConfig("P1", plainsHeavy),
                            PlayerConfig("P2", deck)
                        ),
                        startingPlayerIndex = 0,
                        startingHandSize = 60
                    )
                )
                val processor = ActionProcessor(cardRegistry)
                var state = init.state
                val p1Id = init.playerIds[0]
                val p2Id = init.playerIds[1]

                state = processor.process(state, KeepHand(p1Id)).result.state
                state = processor.process(state, KeepHand(p2Id)).result.state

                val leylineDecision = state.pendingDecision
                leylineDecision.shouldBeInstanceOf<YesNoDecision>()

                val afterNo = processor.process(
                    state,
                    SubmitDecision(p1Id, YesNoResponse(leylineDecision.id, false))
                ).result
                withClue("Submitting no should succeed: ${afterNo.error}") {
                    afterNo.error shouldBe null
                }
                state = afterNo.state

                val handHasLeyline = state.getHand(p1Id).any { id ->
                    state.getEntity(id)?.get<CardComponent>()?.name == "Leyline of Hope"
                }
                withClue("Leyline of Hope should remain in P1's hand after declining") {
                    handHasLeyline shouldBe true
                }

                val battlefield = state.getZone(ZoneKey(p1Id, Zone.BATTLEFIELD))
                val leylineOnBattlefield = battlefield.any { id ->
                    state.getEntity(id)?.get<CardComponent>()?.name == "Leyline of Hope"
                }
                withClue("Leyline of Hope should NOT be on the battlefield after declining") {
                    leylineOnBattlefield shouldBe false
                }

                withClue("Pending decision should be cleared once the leyline phase finishes") {
                    state.pendingDecision shouldBe null
                }

                // Players each still have 20 life (the leyline never resolved into life-gain).
                state.getEntity(p1Id)?.get<LifeTotalComponent>()?.life shouldBe 20
                state.getEntity(p2Id)?.get<LifeTotalComponent>()?.life shouldBe 20
            }
        }
    }
}
