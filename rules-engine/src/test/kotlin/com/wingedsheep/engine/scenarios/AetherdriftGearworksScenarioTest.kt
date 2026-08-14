package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Speed
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AbilityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Five Aetherdrift cards whose behaviour the card snapshot can't prove, because each one leans on
 * an engine path rather than on the shape of its own script:
 *
 * | Card | Under test |
 * |---|---|
 * | Thunderous Velocipede | two `EntersWithCounters` replacements partitioned by the *entering* permanent's mana value |
 * | Repurposing Bay | a library search whose mana-value filter is read from the artifact sacrificed to pay the cost |
 * | Momentum Breaker | "each opponent sacrifices … each opponent who can't discards" evaluated per opponent |
 * | Rocketeer Boostbuggy | an exhaust ability that animates a Vehicle permanently, and refuses a second activation |
 * | Lumbering Worldwagon | a characteristic-defining power that tracks lands, plus its enters/attacks search |
 */
class AetherdriftGearworksScenarioTest : ScenarioTestBase() {

    init {
        context("Thunderous Velocipede") {

            test("a cheap creature enters with one extra counter, an expensive one with three") {
                val game = velocipedeGame(handCards = listOf("Grizzly Bears", "Craw Wurm"))

                game.castAndResolve(1, "Grizzly Bears")
                val bears = game.findPermanent("Grizzly Bears")!!
                withClue("Grizzly Bears is mana value 2, so the 'four or less' clause applies") {
                    game.plusCounters(bears) shouldBe 1
                    game.state.projectedState.getPower(bears) shouldBe 3
                    game.state.projectedState.getToughness(bears) shouldBe 3
                }

                game.castAndResolve(1, "Craw Wurm")
                val wurm = game.findPermanent("Craw Wurm")!!
                withClue("Craw Wurm is mana value 6, so the 'otherwise' clause gives three") {
                    game.plusCounters(wurm) shouldBe 3
                    game.state.projectedState.getPower(wurm) shouldBe 9
                    game.state.projectedState.getToughness(wurm) shouldBe 7
                }
            }

            test("a Vehicle counts too, and the Velocipede never counters itself") {
                val game = velocipedeGame(handCards = listOf("Rover Blades"))

                val velocipede = game.findPermanent("Thunderous Velocipede")!!
                withClue("'Each *other* …' — the Velocipede's own entry skipped both replacements") {
                    game.plusCounters(velocipede) shouldBe 0
                }

                game.castAndResolve(1, "Rover Blades")
                val blades = game.findPermanent("Rover Blades")!!
                withClue("Rover Blades is a mana value 3 Vehicle, not a creature — still one counter") {
                    game.plusCounters(blades) shouldBe 1
                }
            }

            test("an opponent's creature is untouched") {
                val game = velocipedeGame(
                    handCards = emptyList(),
                    opponentHand = listOf("Grizzly Bears"),
                    activePlayer = 2
                )

                game.castAndResolve(2, "Grizzly Bears")
                val bears = game.findPermanent("Grizzly Bears")!!
                withClue("The replacement is scoped to creatures and Vehicles *you* control") {
                    game.plusCounters(bears) shouldBe 0
                }
            }
        }

        context("Repurposing Bay") {

            test("the search is filtered to one plus the sacrificed artifact's mana value") {
                val game = bayGame()
                val bauble = game.findPermanent("Grim Bauble")!!

                game.activateBay()
                val sacrifice = game.getPendingDecision() as SelectCardsDecision
                withClue("The cost sacrifices another artifact — the Bay itself isn't offered") {
                    sacrifice.options.map { game.cardName(it) }.toSet() shouldBe
                        setOf("Grim Bauble", "Aetherjacket")
                }
                game.selectCards(listOf(bauble))
                game.settle()

                val search = game.getPendingDecision() as SelectCardsDecision
                withClue("Grim Bauble is mana value 1, so only a mana value 2 artifact qualifies") {
                    search.options.map { game.cardName(it) } shouldBe listOf("Burner Rocket")
                }
                withClue("Searching never requires finding (CR 701.23b)") {
                    search.minSelections shouldBe 0
                }

                game.selectCards(search.options.take(1))
                game.settle()

                withClue("The found artifact arrives on the battlefield, the Bauble is in the yard") {
                    game.isOnBattlefield("Burner Rocket") shouldBe true
                    game.isInGraveyard(1, "Grim Bauble") shouldBe true
                }
            }

            test("sacrificing a costlier artifact moves the window up") {
                val game = bayGame()
                val jacket = game.findPermanent("Aetherjacket")!!

                game.activateBay()
                game.selectCards(listOf(jacket))
                game.settle()

                val search = game.getPendingDecision() as SelectCardsDecision
                withClue("Aetherjacket is mana value 3, so the window is mana value 4") {
                    search.options.map { game.cardName(it) } shouldBe listOf("Racers' Scoreboard")
                }
            }
        }

        context("Momentum Breaker") {

            test("an opponent with a board sacrifices rather than discards") {
                val game = breakerGame(
                    opponentBattlefield = listOf("Grizzly Bears"),
                    opponentHand = listOf("Hill Giant", "Craw Wurm")
                )

                game.castAndResolve(1, "Momentum Breaker")
                game.settle()

                withClue("They control a creature, so the sacrifice branch runs") {
                    game.isInGraveyard(2, "Grizzly Bears") shouldBe true
                }
                withClue("Sacrificing means they never reach 'each opponent who can't'") {
                    game.handSize(2) shouldBe 2
                }
            }

            test("an opponent with an empty board discards instead") {
                val game = breakerGame(
                    opponentBattlefield = emptyList(),
                    opponentHand = listOf("Hill Giant", "Craw Wurm")
                )

                game.castAndResolve(1, "Momentum Breaker")

                val discard = game.getPendingDecision() as SelectCardsDecision
                withClue("The discard is the opponent's own choice, from their own hand") {
                    discard.playerId shouldBe game.player2Id
                    discard.options.map { game.cardName(it) }.toSet() shouldBe
                        setOf("Hill Giant", "Craw Wurm")
                }
                game.selectCards(discard.options.take(1))
                game.settle()

                withClue("No creature or Vehicle to give up — the fallback discard fires") {
                    game.handSize(2) shouldBe 1
                    game.graveyardSize(2) shouldBe 1
                }
            }

            test("a Vehicle satisfies the sacrifice even though it isn't a creature") {
                val game = breakerGame(
                    opponentBattlefield = listOf("Burner Rocket"),
                    opponentHand = listOf("Hill Giant")
                )

                game.castAndResolve(1, "Momentum Breaker")
                game.settle()

                withClue("'a creature or Vehicle' — an uncrewed Vehicle still counts") {
                    game.isInGraveyard(2, "Burner Rocket") shouldBe true
                    game.handSize(2) shouldBe 1
                }
            }

            test("the sacrifice outlet pays out the controller's speed") {
                val game = breakerGame(
                    opponentBattlefield = listOf("Grizzly Bears"),
                    opponentHand = emptyList()
                )
                game.castAndResolve(1, "Momentum Breaker")
                game.settle()

                withClue("Start your engines! set an unset speed to 1 (CR 702.179b)") {
                    game.state.speed(game.player1Id) shouldBe Speed.STARTING
                }
                val lifeBefore = game.getLifeTotal(1)

                game.activateBreakerOutlet()
                game.settle()

                withClue("You gain life equal to your speed — 1 at starting speed") {
                    game.getLifeTotal(1) shouldBe lifeBefore + 1
                }
                withClue("The enchantment was sacrificed as a cost") {
                    game.isInGraveyard(1, "Momentum Breaker") shouldBe true
                }
            }
        }

        context("Rocketeer Boostbuggy") {

            test("the exhaust ability animates it for good, and only ever fires once") {
                val builder = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Rocketeer Boostbuggy")
                    .withLandsOnBattlefield(1, "Mountain", 6)
                stockLibraries(builder)
                val game = builder
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val buggy = game.findPermanent("Rocketeer Boostbuggy")!!
                withClue("An uncrewed Vehicle is not a creature") {
                    game.state.projectedState.isCreature(buggy) shouldBe false
                }

                game.activateExhaust("Rocketeer Boostbuggy", buggy)
                game.settle()

                withClue("No 'until end of turn' clause — the animation is permanent") {
                    game.state.projectedState.isCreature(buggy) shouldBe true
                }
                withClue("3/2 plus the +1/+1 counter it put on itself") {
                    game.plusCounters(buggy) shouldBe 1
                    game.state.projectedState.getPower(buggy) shouldBe 4
                    game.state.projectedState.getToughness(buggy) shouldBe 3
                }

                val second = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = buggy,
                        abilityId = game.exhaustAbilityId("Rocketeer Boostbuggy")
                    )
                )
                withClue("Exhaust — activate each exhaust ability only once") {
                    (second.error != null) shouldBe true
                }
            }
        }

        context("Lumbering Worldwagon") {

            test("its power tracks the lands you control and its enters trigger fetches a basic") {
                val builder = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Lumbering Worldwagon")
                    .withLandsOnBattlefield(1, "Forest", 3)
                repeat(4) { builder.withCardInLibrary(1, "Forest") }
                stockLibraries(builder)
                val game = builder
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castAndResolve(1, "Lumbering Worldwagon")
                withClue("'You may search' asks first — the whole search can be declined") {
                    game.getPendingDecision().shouldBeInstanceOf<YesNoDecision>()
                }
                game.answerYesNo(true)
                game.settle()

                val search = game.getPendingDecision() as SelectCardsDecision
                withClue("Only basic lands are offered") {
                    search.options.map { game.cardName(it) }.toSet() shouldBe setOf("Forest")
                }
                game.selectCards(search.options.take(1))
                game.settle()

                val wagon = game.findPermanent("Lumbering Worldwagon")!!
                withClue("Three Forests were out, the trigger fetched a fourth") {
                    game.findAllPermanents("Forest").size shouldBe 4
                }
                withClue("Power is the land count; toughness stays the printed 4") {
                    game.state.projectedState.getPower(wagon) shouldBe 4
                    game.state.projectedState.getToughness(wagon) shouldBe 4
                }
            }
        }
    }

    // ---- board setups -------------------------------------------------------------------------

    /** Player 1's precombat main with the Velocipede already out and seven Forests for casting. */
    private fun velocipedeGame(
        handCards: List<String>,
        opponentHand: List<String> = emptyList(),
        activePlayer: Int = 1
    ): TestGame {
        val builder = scenario()
            .withPlayers("Player", "Opponent")
            .withCardOnBattlefield(1, "Thunderous Velocipede")
            .withLandsOnBattlefield(1, "Forest", 10)
            .withLandsOnBattlefield(2, "Forest", 10)
        handCards.forEach { builder.withCardInHand(1, it) }
        opponentHand.forEach { builder.withCardInHand(2, it) }
        stockLibraries(builder)
        return builder
            .withActivePlayer(activePlayer)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()
    }

    /**
     * Repurposing Bay plus two sacrificeable artifacts of different mana values (Grim Bauble 1,
     * Aetherjacket 3), and a library holding one artifact at each of mana value 2, 3 and 4 so the
     * cost-linked filter has something to discriminate between.
     */
    private fun bayGame(): TestGame {
        val builder = scenario()
            .withPlayers("Player", "Opponent")
            .withCardOnBattlefield(1, "Repurposing Bay")
            .withCardOnBattlefield(1, "Grim Bauble")
            .withCardOnBattlefield(1, "Aetherjacket")
            .withLandsOnBattlefield(1, "Island", 4)
            .withCardInLibrary(1, "Burner Rocket")
            .withCardInLibrary(1, "Rover Blades")
            .withCardInLibrary(1, "Racers' Scoreboard")
        repeat(6) { builder.withCardInLibrary(2, "Grizzly Bears") }
        return builder
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()
    }

    /** Momentum Breaker in hand with the mana to cast it; the opponent's board and hand are set up. */
    private fun breakerGame(
        opponentBattlefield: List<String>,
        opponentHand: List<String>
    ): TestGame {
        val builder = scenario()
            .withPlayers("Player", "Opponent")
            .withCardInHand(1, "Momentum Breaker")
            .withLandsOnBattlefield(1, "Swamp", 4)
        opponentBattlefield.forEach { builder.withCardOnBattlefield(2, it) }
        opponentHand.forEach { builder.withCardInHand(2, it) }
        stockLibraries(builder)
        return builder
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()
    }

    private fun stockLibraries(builder: ScenarioBuilder) {
        repeat(8) {
            builder.withCardInLibrary(1, "Grizzly Bears")
            builder.withCardInLibrary(2, "Grizzly Bears")
        }
    }

    // ---- drivers ------------------------------------------------------------------------------

    /** Cast [cardName] from [playerNumber]'s hand, auto-pay, and let it resolve. */
    private fun TestGame.castAndResolve(playerNumber: Int, cardName: String) {
        val cast = castSpell(playerNumber, cardName)
        withClue("Casting $cardName failed: ${cast.error}") { cast.error shouldBe null }
        settle()
    }

    private fun TestGame.activateBay() {
        val abilityId = cardRegistry.getCard("Repurposing Bay")!!.script.activatedAbilities.single().id
        val result = execute(
            ActivateAbility(
                playerId = player1Id,
                sourceId = findPermanent("Repurposing Bay")!!,
                abilityId = abilityId
            )
        )
        withClue("Activation failed: ${result.error}") { result.error shouldBe null }
        settle()
    }

    private fun TestGame.exhaustAbilityId(cardName: String): AbilityId =
        cardRegistry.getCard(cardName)!!.script.activatedAbilities.single { it.isExhaust }.id

    private fun TestGame.activateExhaust(cardName: String, sourceId: EntityId) {
        val result = execute(
            ActivateAbility(
                playerId = player1Id,
                sourceId = sourceId,
                abilityId = exhaustAbilityId(cardName)
            )
        )
        withClue("Activation failed: ${result.error}") { result.error shouldBe null }
        settle()
    }

    private fun TestGame.activateBreakerOutlet() {
        val abilityId = cardRegistry.getCard("Momentum Breaker")!!.script.activatedAbilities.single().id
        val result = execute(
            ActivateAbility(
                playerId = player1Id,
                sourceId = findPermanent("Momentum Breaker")!!,
                abilityId = abilityId
            )
        )
        withClue("Activation failed: ${result.error}") { result.error shouldBe null }
        settle()
    }

    /**
     * Drain mana prompts and the stack until the game is idle or waiting on a decision the test
     * itself has to answer. Auto-paying inline keeps the tests about the cards, not about mana.
     */
    private fun TestGame.settle() {
        var guard = 0
        while (guard++ < 30) {
            if (getPendingDecision() is SelectManaSourcesDecision) {
                submitManaSourcesAutoPay()
                continue
            }
            if (hasPendingDecision()) return
            if (state.stack.isEmpty()) return
            resolveStack()
        }
    }

    private fun TestGame.plusCounters(entityId: EntityId): Int {
        val counters = state.getEntity(entityId)?.get<CountersComponent>() ?: return 0
        return counters.getCount(CounterType.PLUS_ONE_PLUS_ONE)
    }

    private fun TestGame.cardName(id: EntityId): String? =
        state.getEntity(id)?.get<CardComponent>()?.name
}
