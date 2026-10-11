package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.war.cards.KarnTheGreatCreator
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Karn, the Great Creator — the opponents'-artifacts activation lock (mana abilities included),
 * the +1's until-your-next-turn animate with P/T = mana value, and the −2's single pick across
 * "outside the game" (the sideboard) and your own face-up exiled artifact cards.
 */
class KarnTheGreatCreatorScenarioTest : ScenarioTestBase() {

    private val plusOne = KarnTheGreatCreator.activatedAbilities[0].id
    private val minusTwo = KarnTheGreatCreator.activatedAbilities[1].id

    private fun ScenarioTestBase.TestGame.setKarnLoyalty(n: Int) {
        val karn = findPermanent("Karn, the Great Creator")!!
        state = state.updateEntity(karn) { it.with(CountersComponent().withAdded(CounterType.LOYALTY, n)) }
    }

    private fun ScenarioTestBase.TestGame.canActivateAnyAbilityOf(playerNumber: Int, sourceId: com.wingedsheep.sdk.model.EntityId) =
        getLegalActions(playerNumber).any { (it.action as? ActivateAbility)?.sourceId == sourceId }

    init {
        context("Karn, the Great Creator") {

            test("opponents can't activate abilities of their artifacts, mana abilities included; yours still work") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Karn, the Great Creator")
                    .withCardOnBattlefield(1, "Mind Stone")
                    .withCardOnBattlefield(2, "Mind Stone")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val (mine, theirs) = game.findPermanents("Mind Stone").partition {
                    game.state.getEntity(it)?.get<com.wingedsheep.engine.state.components.identity.ControllerComponent>()?.playerId == game.player1Id
                }

                withClue("Karn's controller can still activate their own artifact") {
                    game.canActivateAnyAbilityOf(1, mine.single()) shouldBe true
                }
                game.passPriority()
                withClue("the opponent can't activate their Mind Stone, not even its mana ability") {
                    game.state.priorityPlayerId shouldBe game.player2Id
                    game.canActivateAnyAbilityOf(2, theirs.single()) shouldBe false
                }
            }

            test("+1 animates a noncreature artifact as a P/T = mana value creature until your next turn") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Karn, the Great Creator")
                    .withCardOnBattlefield(1, "Millstone")
                    .apply { repeat(4) { withCardInLibrary(1, "Island"); withCardInLibrary(2, "Island") } }
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                game.setKarnLoyalty(5)
                val karn = game.findPermanent("Karn, the Great Creator")!!
                val millstone = game.findPermanent("Millstone")!!

                game.execute(
                    ActivateAbility(
                        playerId = game.player1Id, sourceId = karn, abilityId = plusOne,
                        targets = listOf(ChosenTarget.Permanent(millstone)),
                    )
                ).error shouldBe null
                game.resolveStack()

                game.state.getEntity(karn)?.get<CountersComponent>()?.getCount(CounterType.LOYALTY) shouldBe 6
                val projected = game.state.projectedState
                withClue("Millstone (mana value 2) is a 2/2 artifact creature") {
                    projected.isCreature(millstone) shouldBe true
                    projected.hasType(millstone, "ARTIFACT") shouldBe true
                    projected.getPower(millstone) shouldBe 2
                    projected.getToughness(millstone) shouldBe 2
                }

                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
                withClue("still a creature during the opponent's turn") {
                    game.state.activePlayerId shouldBe game.player2Id
                    game.state.projectedState.isCreature(millstone) shouldBe true
                }

                game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
                withClue("the animate ended as Karn's controller's next turn began") {
                    game.state.activePlayerId shouldBe game.player1Id
                    game.state.projectedState.isCreature(millstone) shouldBe false
                }
            }

            test("+1 may be activated with no target") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Karn, the Great Creator")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                game.setKarnLoyalty(5)
                val karn = game.findPermanent("Karn, the Great Creator")!!

                game.execute(ActivateAbility(playerId = game.player1Id, sourceId = karn, abilityId = plusOne))
                    .error shouldBe null
                game.resolveStack()
                game.state.getEntity(karn)?.get<CountersComponent>()?.getCount(CounterType.LOYALTY) shouldBe 6
            }

            test("−2 offers sideboard artifacts and your own face-up exiled artifacts, and puts the pick into hand") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Karn, the Great Creator")
                    .withCardInSideboard(1, "Millstone")      // artifact outside the game — eligible
                    .withCardInSideboard(1, "Grizzly Bears")  // not an artifact
                    .withCardInExile(1, "Mind Stone")         // your face-up exiled artifact — eligible
                    .withCardInExile(1, "Jayemdae Tome")      // turned face down below — ineligible
                    .withCardInExile(2, "Ornithopter")        // the opponent owns it — ineligible
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                game.setKarnLoyalty(5)
                val tome = game.state.getExile(game.player1Id).single {
                    game.state.getEntity(it)?.get<CardComponent>()?.name == "Jayemdae Tome"
                }
                game.state = game.state.updateEntity(tome) { it.with(FaceDownComponent) }
                val karn = game.findPermanent("Karn, the Great Creator")!!

                game.execute(ActivateAbility(playerId = game.player1Id, sourceId = karn, abilityId = minusTwo))
                    .error shouldBe null
                game.resolveStack()

                val decision = game.getPendingDecision() as? SelectCardsDecision
                decision.shouldNotBeNull()
                withClue("only the sideboard artifact and the face-up exiled artifact you own are offered") {
                    decision.options.map { game.state.getEntity(it)?.get<CardComponent>()?.name }.toSet() shouldBe
                        setOf("Millstone", "Mind Stone")
                }
                val mindStone = decision.options.single {
                    game.state.getEntity(it)?.get<CardComponent>()?.name == "Mind Stone"
                }
                game.selectCards(listOf(mindStone))

                game.isInHand(1, "Mind Stone") shouldBe true
                game.isInExile(1, "Mind Stone") shouldBe false
                game.isInSideboard(1, "Millstone") shouldBe true
                game.state.getEntity(karn)?.get<CountersComponent>()?.getCount(CounterType.LOYALTY) shouldBe 3
            }

            test("−2 can fetch from outside the game, or be declined") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Karn, the Great Creator")
                    .withCardInSideboard(1, "Millstone")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                game.setKarnLoyalty(5)
                val karn = game.findPermanent("Karn, the Great Creator")!!

                game.execute(ActivateAbility(playerId = game.player1Id, sourceId = karn, abilityId = minusTwo))
                    .error shouldBe null
                game.resolveStack()
                game.skipSelection()

                withClue("declining leaves the card outside the game") {
                    game.isInSideboard(1, "Millstone") shouldBe true
                    game.isInHand(1, "Millstone") shouldBe false
                }
            }
        }
    }
}
