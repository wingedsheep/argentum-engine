package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.DistributeDecision
import com.wingedsheep.engine.core.DistributionResponse
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for the TDM "group 2" batch:
 *  - Worthy Cost (#99): {B} sorcery — sacrifice a creature, exile target creature or planeswalker.
 *  - Desperate Measures (#78): {B} instant — +1/-1 + delayed "when it dies this turn, draw two".
 *  - Knockout Maneuver (#147): {2}{G} sorcery — +1/+1 counter, then it deals damage equal to its
 *    power to a creature an opponent controls.
 *  - Synchronized Charge (#162): {1}{G} sorcery — distribute two +1/+1 counters, then creatures you
 *    control with counters gain vigilance and trample.
 */
class TdmGroup2ScenarioTest : ScenarioTestBase() {

    private val stateProjector = StateProjector()

    init {
        // Inline test creatures (ScenarioTestBase only has set-registered cards).
        cardRegistry.register(
            CardDefinition.creature("Bear Cub", ManaCost.parse("{1}{G}"), emptySet(), power = 2, toughness = 2)
        )
        cardRegistry.register(
            CardDefinition.creature("Sacrificial Goat", ManaCost.parse("{G}"), emptySet(), power = 0, toughness = 1)
        )

        context("Worthy Cost") {
            test("sacrifices a creature and exiles the target creature") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Worthy Cost")
                    .withLandsOnBattlefield(1, "Swamp", 1)
                    .withCardOnBattlefield(1, "Sacrificial Goat")
                    .withCardOnBattlefield(2, "Bear Cub")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val goat = game.findPermanent("Sacrificial Goat")!!
                val bear = game.findPermanent("Bear Cub")!!

                val cast = game.execute(
                    CastSpell(
                        playerId = game.player1Id,
                        cardId = game.findCardsInHand(1, "Worthy Cost").first(),
                        targets = listOf(ChosenTarget.Permanent(bear)),
                        additionalCostPayment = AdditionalCostPayment(sacrificedPermanents = listOf(goat))
                    )
                )
                withClue("Casting Worthy Cost should succeed: ${cast.error}") {
                    cast.error shouldBe null
                }
                withClue("The sacrificed creature should be gone from the battlefield") {
                    game.isOnBattlefield("Sacrificial Goat") shouldBe false
                }
                game.resolveStack()

                withClue("The targeted creature should be exiled") {
                    game.isOnBattlefield("Bear Cub") shouldBe false
                    game.state.getExile(game.player2Id).any {
                        game.state.getEntity(it)?.get<CardComponent>()?.name == "Bear Cub"
                    } shouldBe true
                }
            }
        }

        context("Desperate Measures") {
            test("applies +1/-1 and draws two when the creature dies this turn") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Desperate Measures")
                    .withLandsOnBattlefield(1, "Swamp", 1)
                    .withCardOnBattlefield(1, "Sacrificial Goat") // 0/1 -> 1/0 -> dies
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(1, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val goat = game.findPermanent("Sacrificial Goat")!!
                val handBefore = game.handSize(1)

                val cast = game.castSpell(1, "Desperate Measures", goat)
                withClue("Casting Desperate Measures should succeed: ${cast.error}") {
                    cast.error shouldBe null
                }
                game.resolveStack()

                // The 0/1 becomes 1/0 and dies as a state-based action, firing the delayed draw trigger.
                withClue("The creature should have died from -1 toughness") {
                    game.isOnBattlefield("Sacrificial Goat") shouldBe false
                }
                game.resolveStack()

                withClue("Controller should have drawn two cards (net +1: spell left hand, +2 drawn)") {
                    game.handSize(1) shouldBe handBefore + 1
                }
            }
        }

        context("Knockout Maneuver") {
            test("adds a counter then deals damage equal to boosted power") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Knockout Maneuver")
                    .withLandsOnBattlefield(1, "Forest", 3)
                    .withCardOnBattlefield(1, "Bear Cub")     // 2/2 -> 3/3 after counter
                    .withCardOnBattlefield(2, "Bear Cub")     // 2/2 opponent target, takes 3 -> dies
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                fun controllerOf(id: com.wingedsheep.sdk.model.EntityId) =
                    game.state.getEntity(id)?.get<ControllerComponent>()?.playerId
                val mine = game.findPermanents("Bear Cub").first { controllerOf(it) == game.player1Id }
                val theirs = game.findPermanents("Bear Cub").first { controllerOf(it) == game.player2Id }

                val cast = game.execute(
                    CastSpell(
                        playerId = game.player1Id,
                        cardId = game.findCardsInHand(1, "Knockout Maneuver").first(),
                        targets = listOf(ChosenTarget.Permanent(mine), ChosenTarget.Permanent(theirs))
                    )
                )
                withClue("Casting Knockout Maneuver should succeed: ${cast.error}") {
                    cast.error shouldBe null
                }
                game.resolveStack()

                withClue("My creature should have a +1/+1 counter") {
                    game.state.getEntity(mine)?.get<CountersComponent>()
                        ?.getCount(CounterType.PLUS_ONE_PLUS_ONE) shouldBe 1
                }
                withClue("The 3 damage from the boosted 3/3 should have killed the opponent's 2/2") {
                    game.state.getBattlefield().contains(theirs) shouldBe false
                }
                withClue("Opponent's Bear Cub should be in their graveyard") {
                    game.findCardsInGraveyard(2, "Bear Cub").size shouldBe 1
                }
            }
        }

        context("Synchronized Charge") {
            test("distributes two counters then grants vigilance and trample to counter-bearing creatures") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Synchronized Charge")
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withCardOnBattlefield(1, "Bear Cub")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bear = game.findPermanent("Bear Cub")!!

                val cast = game.execute(
                    CastSpell(
                        playerId = game.player1Id,
                        cardId = game.findCardsInHand(1, "Synchronized Charge").first(),
                        targets = listOf(ChosenTarget.Permanent(bear))
                    )
                )
                withClue("Casting Synchronized Charge should succeed: ${cast.error}") {
                    cast.error shouldBe null
                }
                game.resolveStack()

                // Distribute both counters onto the single target.
                if (game.hasPendingDecision()) {
                    val decision = game.getPendingDecision() as DistributeDecision
                    game.submitDecision(
                        DistributionResponse(decision.id, mapOf(bear to decision.totalAmount))
                    )
                    game.resolveStack()
                }

                withClue("Bear Cub should carry two +1/+1 counters") {
                    game.state.getEntity(bear)?.get<CountersComponent>()
                        ?.getCount(CounterType.PLUS_ONE_PLUS_ONE) shouldBe 2
                }
                val keywords = stateProjector.getProjectedKeywords(game.state, bear)
                withClue("Bear Cub should have gained vigilance") {
                    keywords.contains(Keyword.VIGILANCE) shouldBe true
                }
                withClue("Bear Cub should have gained trample") {
                    keywords.contains(Keyword.TRAMPLE) shouldBe true
                }
            }
        }
    }
}
