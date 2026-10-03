package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe

/**
 * Emperor of Bones (MH3 #90) — {1}{B} Creature — Skeleton Noble 2/2
 *
 *   At the beginning of combat on your turn, exile up to one target card from a graveyard.
 *   {1}{B}: Adapt 2.
 *   Whenever one or more +1/+1 counters are put on this creature, put a creature card exiled with
 *   this creature onto the battlefield under your control with a finality counter on it. It gains
 *   haste. Sacrifice it at the beginning of the next end step.
 */
class EmperorOfBonesScenarioTest : ScenarioTestBase() {

    init {
        context("Emperor of Bones") {

            test("exiles an opposing creature card, adapt reanimates it with finality + haste, end step exiles it") {
                val game = scenario()
                    .withPlayers("Player1", "Opponent")
                    .withCardOnBattlefield(1, "Emperor of Bones", summoningSickness = false)
                    .withCardInGraveyard(2, "Centaur Courser")
                    .withLandsOnBattlefield(1, "Swamp", 2)
                    .withCardInLibrary(1, "Swamp")
                    .withCardInLibrary(2, "Swamp")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val emperor = game.findPermanent("Emperor of Bones")!!
                val courser = game.findCardsInGraveyard(2, "Centaur Courser").single()

                game.passUntilPhase(Phase.COMBAT, Step.BEGIN_COMBAT)
                withClue("the begin-combat trigger asks for up to one target card in a graveyard") {
                    (game.getPendingDecision() is ChooseTargetsDecision) shouldBe true
                }
                game.selectTargets(listOf(courser))
                game.resolveStack()

                withClue("Centaur Courser is exiled") {
                    game.state.getExile(game.player2Id) shouldContain courser
                }

                // {1}{B}: Adapt 2 — the +1/+1 counters fire the reanimation trigger.
                val adapt = cardRegistry.getCard("Emperor of Bones")!!.script.activatedAbilities[0]
                val result = game.execute(
                    ActivateAbility(playerId = game.player1Id, sourceId = emperor, abilityId = adapt.id)
                )
                withClue("adapt activation should succeed: ${result.error}") { result.error shouldBe null }
                game.resolveStack()

                withClue("Emperor got two +1/+1 counters") {
                    game.state.getEntity(emperor)?.get<CountersComponent>()
                        ?.getCount(CounterType.PLUS_ONE_PLUS_ONE) shouldBe 2
                }

                val reanimated = game.findPermanent("Centaur Courser")
                withClue("Centaur Courser is on the battlefield under Player1's control") {
                    (reanimated != null) shouldBe true
                    game.state.projectedState.getController(reanimated!!) shouldBe game.player1Id
                }
                withClue("it has a finality counter and haste") {
                    game.state.getEntity(reanimated!!)?.get<CountersComponent>()
                        ?.getCount(CounterType.FINALITY) shouldBe 1
                    game.state.projectedState.hasKeyword(reanimated, Keyword.HASTE) shouldBe true
                }

                // At the beginning of the next end step it is sacrificed — and the finality counter
                // exiles it instead of putting it into its owner's graveyard.
                game.passUntilPhase(Phase.ENDING, Step.END)
                game.resolveStack()

                withClue("Centaur Courser left the battlefield and was exiled, not put into a graveyard") {
                    game.isOnBattlefield("Centaur Courser") shouldBe false
                    game.isInGraveyard(2, "Centaur Courser") shouldBe false
                    game.isInExile(2, "Centaur Courser") shouldBe true
                }
            }

            test("a non-creature card exiled with it is not put onto the battlefield") {
                val game = scenario()
                    .withPlayers("Player1", "Opponent")
                    .withCardOnBattlefield(1, "Emperor of Bones", summoningSickness = false)
                    .withCardInGraveyard(2, "Lightning Bolt")
                    .withLandsOnBattlefield(1, "Swamp", 2)
                    .withCardInLibrary(1, "Swamp")
                    .withCardInLibrary(2, "Swamp")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val emperor = game.findPermanent("Emperor of Bones")!!
                val bolt = game.findCardsInGraveyard(2, "Lightning Bolt").single()

                game.passUntilPhase(Phase.COMBAT, Step.BEGIN_COMBAT)
                game.selectTargets(listOf(bolt))
                game.resolveStack()

                val adapt = cardRegistry.getCard("Emperor of Bones")!!.script.activatedAbilities[0]
                game.execute(
                    ActivateAbility(playerId = game.player1Id, sourceId = emperor, abilityId = adapt.id)
                ).error shouldBe null
                game.resolveStack()

                withClue("the instant stays in exile") {
                    game.state.getExile(game.player2Id) shouldContain bolt
                    game.state.getBattlefield(game.player1Id) shouldNotContain bolt
                }
            }

            test("adapting again with counters already on it adds nothing and reanimates nothing") {
                val game = scenario()
                    .withPlayers("Player1", "Opponent")
                    .withCardOnBattlefield(1, "Emperor of Bones", summoningSickness = false)
                    .withCardInGraveyard(2, "Centaur Courser")
                    .withLandsOnBattlefield(1, "Swamp", 2)
                    .withCardInLibrary(1, "Swamp")
                    .withCardInLibrary(2, "Swamp")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val emperor = game.findPermanent("Emperor of Bones")!!
                game.state = game.state.updateEntity(emperor) {
                    it.with(CountersComponent(mapOf(CounterType.PLUS_ONE_PLUS_ONE to 1)))
                }
                val courser = game.findCardsInGraveyard(2, "Centaur Courser").single()

                game.passUntilPhase(Phase.COMBAT, Step.BEGIN_COMBAT)
                game.selectTargets(listOf(courser))
                game.resolveStack()

                val adapt = cardRegistry.getCard("Emperor of Bones")!!.script.activatedAbilities[0]
                game.execute(
                    ActivateAbility(playerId = game.player1Id, sourceId = emperor, abilityId = adapt.id)
                ).error shouldBe null
                game.resolveStack()

                withClue("no counters added, so the Courser stays exiled") {
                    game.state.getEntity(emperor)?.get<CountersComponent>()
                        ?.getCount(CounterType.PLUS_ONE_PLUS_ONE) shouldBe 1
                    game.isOnBattlefield("Centaur Courser") shouldBe false
                    game.state.getExile(game.player2Id) shouldContain courser
                }
            }
        }
    }
}
