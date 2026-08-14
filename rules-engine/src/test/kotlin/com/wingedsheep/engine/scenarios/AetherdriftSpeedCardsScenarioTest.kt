package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.EngineServices
import com.wingedsheep.engine.mechanics.speed.SpeedService
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Speed
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe

/**
 * The Aetherdrift speed cards added with the mechanic, exercised as printed — one per
 * "Max speed — [Ability]" payload kind, so each gate the `maxSpeed { }` block installs is proven on
 * real content rather than only on the inline test cards in [SpeedMechanicScenarioTest]:
 *
 * | Card | Payload | Gate under test |
 * |---|---|---|
 * | Burnout Bashtronaut | static keyword grant | `ConditionalStaticAbility` |
 * | Walking Sarcophagus | static P/T buff | `ConditionalStaticAbility` |
 * | Endrider Catalyzer | activated mana ability | `ActivationRestriction.OnlyIfCondition` |
 * | Risen Necroregent | end-step trigger | `triggerCondition` |
 * | Racers' Scoreboard | spell cost reduction | `CostGating.OnlyIf` |
 *
 * Each also carries "Start your engines!", so every test doubles as a check that the CR 704.5z
 * state-based action starts speed from a real card's printed keyword.
 */
class AetherdriftSpeedCardsScenarioTest : ScenarioTestBase() {

    init {
        context("Burnout Bashtronaut") {

            test("starts speed at 1 and only has double strike at max speed") {
                val game = speedGame("Burnout Bashtronaut")
                val goblin = game.findPermanent("Burnout Bashtronaut")!!

                withClue("Its printed \"Start your engines!\" starts speed via the CR 704.5z SBA") {
                    game.state.speed(game.player1Id) shouldBe Speed.STARTING
                }
                withClue("Menace is printed and unconditional; double strike is behind the gate") {
                    game.state.projectedState.hasKeyword(goblin, Keyword.MENACE) shouldBe true
                    game.state.projectedState.hasKeyword(goblin, Keyword.DOUBLE_STRIKE) shouldBe false
                }

                game.state = SpeedService.set(game.state, game.player1Id, Speed.MAX, "test").first

                withClue("At max speed it has double strike") {
                    game.state.projectedState.hasKeyword(goblin, Keyword.DOUBLE_STRIKE) shouldBe true
                }
            }

            test("its ungated pump ability works at any speed") {
                val game = speedGame("Burnout Bashtronaut", lands = 2)
                val goblin = game.findPermanent("Burnout Bashtronaut")!!
                val abilityId = cardRegistry.getCard("Burnout Bashtronaut")!!
                    .script.activatedAbilities.single { it.restrictions.isEmpty() }.id

                val activate = game.execute(
                    ActivateAbility(playerId = game.player1Id, sourceId = goblin, abilityId = abilityId)
                )
                withClue("The {2} pump has no max-speed gate: ${activate.error}") {
                    activate.error shouldBe null
                }
                game.autoPayIfAsked()
                game.resolveStack()

                withClue("1/1 pumped to 2/1 while still at speed 1") {
                    game.state.speed(game.player1Id) shouldBe Speed.STARTING
                    game.state.projectedState.getPower(goblin) shouldBe 2
                }
            }
        }

        context("Walking Sarcophagus") {

            test("is a 2/1 below max speed and a 3/3 at max speed") {
                val game = speedGame("Walking Sarcophagus")
                val zombie = game.findPermanent("Walking Sarcophagus")!!

                withClue("Base 2/1 while the gate is closed") {
                    game.state.projectedState.getPower(zombie) shouldBe 2
                    game.state.projectedState.getToughness(zombie) shouldBe 1
                }

                game.state = SpeedService.set(game.state, game.player1Id, Speed.MAX, "test").first

                withClue("+1/+2 applies at max speed") {
                    game.state.projectedState.getPower(zombie) shouldBe 3
                    game.state.projectedState.getToughness(zombie) shouldBe 3
                }
            }
        }

        context("Endrider Catalyzer") {

            test("its mana ability is only activatable at max speed") {
                val game = speedGame("Endrider Catalyzer")
                val catalyzer = game.findPermanent("Endrider Catalyzer")!!
                val abilityId = cardRegistry.getCard("Endrider Catalyzer")!!
                    .script.activatedAbilities.single().id

                val blocked = game.execute(
                    ActivateAbility(playerId = game.player1Id, sourceId = catalyzer, abilityId = abilityId)
                )
                withClue("At speed 1 the ability doesn't exist, so activating it fails") {
                    (blocked.error != null).shouldBeTrue()
                }

                game.state = SpeedService.set(game.state, game.player1Id, Speed.MAX, "test").first

                val allowed = game.execute(
                    ActivateAbility(playerId = game.player1Id, sourceId = catalyzer, abilityId = abilityId)
                )
                withClue("At max speed it activates: ${allowed.error}") { allowed.error shouldBe null }
                game.resolveStack()

                withClue("…and adds {R}{R} to the pool") {
                    game.state.getEntity(game.player1Id)
                        ?.get<com.wingedsheep.engine.state.components.player.ManaPoolComponent>()
                        ?.total shouldBe 2
                }
            }
        }

        context("Risen Necroregent") {

            test("makes a Zombie at end of turn only at max speed") {
                val belowMax = speedGame("Risen Necroregent")
                belowMax.passUntilPhase(Phase.ENDING, Step.END)
                belowMax.resolveStack()
                withClue("No end-step trigger below max speed") {
                    belowMax.zombieTokens() shouldBe 0
                }

                val atMax = speedGame("Risen Necroregent")
                atMax.state = SpeedService.set(atMax.state, atMax.player1Id, Speed.MAX, "test").first
                atMax.passUntilPhase(Phase.ENDING, Step.END)
                atMax.resolveStack()
                withClue("At max speed the end step makes one 2/2 Zombie") {
                    atMax.zombieTokens() shouldBe 1
                }
            }
        }

        context("Racers' Scoreboard") {

            // A cost reduction never reaches the layer system: CostCalculator scans the raw static
            // list for `is ModifySpellCost`, so the max-speed gate has to live in the modifier's own
            // CostGating.OnlyIf slot. Wrapped in a ConditionalStaticAbility it would be invisible
            // here and the reduction would apply at every speed.
            test("its reduction applies only at max speed") {
                val game = speedGame("Racers' Scoreboard")
                val calculator = EngineServices(cardRegistry).costCalculator
                val bears = cardRegistry.getCard("Grizzly Bears")!!

                withClue("At speed 1 Grizzly Bears still costs its printed {1}{G}") {
                    calculator.calculateEffectiveCost(game.state, bears, game.player1Id)
                        .toString() shouldBe ManaCost.parse("{1}{G}").toString()
                }

                game.state = SpeedService.set(game.state, game.player1Id, Speed.MAX, "test").first

                withClue("At max speed the generic pip is shaved off") {
                    calculator.calculateEffectiveCost(game.state, bears, game.player1Id)
                        .toString() shouldBe ManaCost.parse("{G}").toString()
                }
            }

            test("the reduction is scoped to its controller's spells") {
                val game = speedGame("Racers' Scoreboard")
                val calculator = EngineServices(cardRegistry).costCalculator
                val bears = cardRegistry.getCard("Grizzly Bears")!!

                // "Spells you cast" reads the source's controller, so raising the *opponent's* speed
                // must not discount their spells off a Scoreboard player 1 controls.
                game.state = SpeedService.set(game.state, game.player2Id, Speed.MAX, "test").first

                withClue("The opponent having max speed doesn't discount their spells") {
                    calculator.calculateEffectiveCost(game.state, bears, game.player2Id)
                        .toString() shouldBe ManaCost.parse("{1}{G}").toString()
                }
            }

            test("it can actually be cast down to a cheaper real cast at max speed") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Racers' Scoreboard")
                    .withLandsOnBattlefield(1, "Forest", 1)
                    .withCardInHand(1, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.BEGINNING, Step.UPKEEP)
                    .build()
                game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)

                val blocked = game.castSpell(1, "Grizzly Bears")
                withClue("One Forest can't pay {1}{G} at speed 1") {
                    (blocked.error != null).shouldBeTrue()
                }

                game.state = SpeedService.set(game.state, game.player1Id, Speed.MAX, "test").first

                val allowed = game.castSpell(1, "Grizzly Bears")
                withClue("At max speed the cost is {G}, which one Forest covers: ${allowed.error}") {
                    allowed.error shouldBe null
                }
            }
        }
    }

    /**
     * Player 1's precombat main phase with [cardName] on the battlefield and stocked libraries.
     *
     * Built in the upkeep step and advanced a step so the CR 704.5z state-based action actually runs
     * (it is polled on step changes) — the same reasoning as [SpeedMechanicScenarioTest.speedGame].
     */
    private fun speedGame(cardName: String, lands: Int = 0): TestGame {
        val builder = scenario()
            .withPlayers("Player", "Opponent")
            .withCardOnBattlefield(1, cardName, summoningSickness = false)
        if (lands > 0) builder.withLandsOnBattlefield(1, "Mountain", lands)
        repeat(12) {
            builder.withCardInLibrary(1, "Grizzly Bears")
            builder.withCardInLibrary(2, "Grizzly Bears")
        }
        val game = builder
            .withActivePlayer(1)
            .inPhase(Phase.BEGINNING, Step.UPKEEP)
            .build()
        game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        return game
    }

    /** Zombie creature tokens player 1 controls. */
    private fun TestGame.zombieTokens(): Int =
        state.getBattlefield().count { id ->
            val container = state.getEntity(id) ?: return@count false
            container.has<TokenComponent>() &&
                container.get<CardComponent>()?.typeLine?.subtypes?.any { it.value == "Zombie" } == true
        }

    /** Submit auto-pay if the engine paused for a mana-source decision. */
    private fun TestGame.autoPayIfAsked() {
        if (getPendingDecision() is com.wingedsheep.engine.core.SelectManaSourcesDecision) {
            submitManaSourcesAutoPay()
        }
    }
}
