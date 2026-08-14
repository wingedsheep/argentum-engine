package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Sharae of Numbing Depths (WOE #213) — {2}{W}{U} Legendary Creature 2/3.
 *
 *   When Sharae enters, tap target creature an opponent controls and put a stun counter on it.
 *   Whenever you tap one or more untapped creatures your opponents control, draw a card. This
 *   ability triggers only once each turn.
 *
 * The draw ability differs from its per-tap siblings on two axes, and each gets a test: it is a
 * **batch** trigger (CR 603.2c — tapping two of their creatures at once draws one card, not two) and
 * it is limited to **once each turn** (two separate taps in one turn still draw one). Attribution is
 * shared with the rest of the cluster: an opponent tapping their own creatures never fires it.
 */
class SharaeOfNumbingDepthsScenarioTest : ScenarioTestBase() {

    private fun TestGame.drain(targets: List<EntityId> = emptyList()) {
        var asked = 0
        var guard = 0
        while (guard++ < 40) {
            when (state.pendingDecision) {
                is ChooseTargetsDecision -> {
                    val pick = targets.getOrNull(asked)
                        ?: error("unexpected extra ChooseTargetsDecision (#${asked + 1})")
                    asked++
                    selectTargets(listOf(pick))
                }
                is SelectManaSourcesDecision -> submitManaSourcesAutoPay()
                null -> {
                    if (state.stack.isEmpty()) return
                    resolveStack()
                }
                else -> error("unexpected decision: ${state.pendingDecision}")
            }
        }
        error("decision loop did not settle")
    }

    private fun TestGame.crownTap(activator: Int, victim: EntityId) {
        val crown = findPermanent("Hylda's Crown of Winter")!!
        execute(
            ActivateAbility(
                playerId = if (activator == 1) player1Id else player2Id,
                sourceId = crown,
                abilityId = cardRegistry.getCard("Hylda's Crown of Winter")!!
                    .activatedAbilities[0].id,
                targets = listOf(ChosenTarget.Permanent(victim)),
            )
        ).error shouldBe null
        drain()
        // The Crown's own {T} is part of the cost; free it up for a second activation.
        state = state.updateEntity(crown) { it.without<TappedComponent>() }
    }

    init {
        context("Sharae of Numbing Depths") {

            test("the entry trigger taps an opposing creature, stuns it, and that tap draws a card") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Sharae of Numbing Depths")
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withLandsOnBattlefield(1, "Island", 2)
                    .withCardInLibrary(1, "Craw Wurm")
                    .withCardOnBattlefield(2, "Hill Giant", summoningSickness = false)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val giant = game.findPermanent("Hill Giant")!!
                val handBefore = game.handSize(1)

                game.castSpell(1, "Sharae of Numbing Depths").error shouldBe null
                game.drain(listOf(giant))

                withClue("tapped with a stun counter") {
                    game.state.getEntity(giant)?.has<TappedComponent>() shouldBe true
                    game.state.getEntity(giant)?.get<CountersComponent>()
                        ?.getCount(CounterType.STUN) shouldBe 1
                }
                withClue("Sharae left your hand and one card was drawn off her own entry tap") {
                    game.handSize(1) shouldBe handBefore - 1 + 1
                }
            }

            test("tapping two of their creatures at once draws one card, not two (CR 603.2c)") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Sharae of Numbing Depths", summoningSickness = false)
                    .withCardInHand(1, "Deluge")
                    .withLandsOnBattlefield(1, "Island", 3)
                    .withCardInLibrary(1, "Craw Wurm")
                    .withCardInLibrary(1, "Bog Imp")
                    .withCardOnBattlefield(2, "Hill Giant", summoningSickness = false)
                    .withCardOnBattlefield(2, "Grizzly Bears", summoningSickness = false)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val giant = game.findPermanent("Hill Giant")!!
                val bears = game.findPermanent("Grizzly Bears")!!
                val handBefore = game.handSize(1)

                // Deluge taps every creature without flying — both of theirs, in one batch.
                game.castSpell(1, "Deluge").error shouldBe null
                game.drain()

                withClue("both opposing creatures got tapped by your spell") {
                    game.state.getEntity(giant)?.has<TappedComponent>() shouldBe true
                    game.state.getEntity(bears)?.has<TappedComponent>() shouldBe true
                }
                withClue("one batch → one card (Deluge left the hand, so net zero)") {
                    game.handSize(1) shouldBe handBefore - 1 + 1
                }
            }

            test("two separate taps in one turn still draw only one card") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Sharae of Numbing Depths", summoningSickness = false)
                    .withCardOnBattlefield(1, "Hylda's Crown of Winter")
                    .withCardInLibrary(1, "Craw Wurm")
                    .withCardInLibrary(1, "Bog Imp")
                    .withCardOnBattlefield(2, "Hill Giant", summoningSickness = false)
                    .withCardOnBattlefield(2, "Grizzly Bears", summoningSickness = false)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val handBefore = game.handSize(1)

                game.crownTap(1, game.findPermanent("Hill Giant")!!)
                game.crownTap(1, game.findPermanent("Grizzly Bears")!!)

                withClue("both taps happened") {
                    game.state.getEntity(game.findPermanent("Hill Giant")!!)
                        ?.has<TappedComponent>() shouldBe true
                    game.state.getEntity(game.findPermanent("Grizzly Bears")!!)
                        ?.has<TappedComponent>() shouldBe true
                }
                withClue("'triggers only once each turn' caps the draw at one") {
                    game.handSize(1) shouldBe handBefore + 1
                }
            }

            test("an opponent tapping their own creature draws nothing") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Sharae of Numbing Depths", summoningSickness = false)
                    .withCardInLibrary(1, "Craw Wurm")
                    .withCardOnBattlefield(2, "Hylda's Crown of Winter")
                    .withCardOnBattlefield(2, "Hill Giant", summoningSickness = false)
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val handBefore = game.handSize(1)
                val giant = game.findPermanent("Hill Giant")!!

                game.crownTap(2, giant)

                withClue("their tap of their own creature is not a tap you made") {
                    game.state.getEntity(giant)?.has<TappedComponent>() shouldBe true
                    game.handSize(1) shouldBe handBefore
                }
            }
        }
    }
}
