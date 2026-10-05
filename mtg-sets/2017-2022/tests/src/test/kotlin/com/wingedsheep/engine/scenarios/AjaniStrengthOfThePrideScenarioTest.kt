package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Scenario test for Ajani, Strength of the Pride (M20 #2) — {2}{W}{W} Legendary Planeswalker — Ajani,
 * loyalty 5.
 *
 *   +1: You gain life equal to the number of creatures you control plus the number of
 *       planeswalkers you control.
 *   −2: Create a 2/2 white Cat Soldier creature token named Ajani's Pridemate with "Whenever you
 *       gain life, put a +1/+1 counter on this token."
 *   0: If you have at least 15 life more than your starting life total, exile Ajani and each
 *      artifact and creature your opponents control.
 */
class AjaniStrengthOfThePrideScenarioTest : ScenarioTestBase() {

    private val ajaniName = "Ajani, Strength of the Pride"

    private fun abilityId(index: Int) = cardRegistry.getCard(ajaniName)!!.activatedAbilities[index].id

    private fun counters(game: TestGame, id: EntityId, type: CounterType): Int =
        game.state.getEntity(id)?.get<CountersComponent>()?.getCount(type) ?: 0

    private fun TestGame.activate(index: Int) {
        val ajani = findPermanent(ajaniName)!!
        execute(ActivateAbility(playerId = player1Id, sourceId = ajani, abilityId = abilityId(index)))
            .error shouldBe null
        resolveStack()
    }

    init {
        context("Ajani, Strength of the Pride") {

            test("+1 gains life equal to creatures plus planeswalkers you control, Ajani included") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, ajaniName)
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(1, "Hill Giant")
                    .withCardOnBattlefield(2, "Savannah Lions")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val ajani = game.findPermanent(ajaniName)!!
                game.activate(0)

                withClue("two creatures + one planeswalker (Ajani) = 3 life; the opponent's Lions don't count") {
                    game.getLifeTotal(1) shouldBe 23
                }
                game.getLifeTotal(2) shouldBe 20
                counters(game, ajani, CounterType.LOYALTY) shouldBe 6
            }

            test("−2 creates Ajani's Pridemate, which gets a +1/+1 counter whenever you gain life") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, ajaniName)
                    .withCardInHand(1, "Sacred Nectar")
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val ajani = game.findPermanent(ajaniName)!!
                game.activate(1)

                counters(game, ajani, CounterType.LOYALTY) shouldBe 3
                val token = game.findPermanent("Ajani's Pridemate").shouldNotBeNull()
                val projected = game.state.projectedState
                withClue("a 2/2 white Cat Soldier") {
                    projected.getPower(token) shouldBe 2
                    projected.getToughness(token) shouldBe 2
                    projected.getColors(token) shouldBe setOf(Color.WHITE.name)
                    projected.hasSubtype(token, "Cat") shouldBe true
                    projected.hasSubtype(token, "Soldier") shouldBe true
                }

                game.castSpell(1, "Sacred Nectar").error shouldBe null
                if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()
                game.resolveStack()

                game.getLifeTotal(1) shouldBe 24
                withClue("the life gain put a +1/+1 counter on the token") {
                    counters(game, token, CounterType.PLUS_ONE_PLUS_ONE) shouldBe 1
                    game.state.projectedState.getPower(token) shouldBe 3
                    game.state.projectedState.getToughness(token) shouldBe 3
                }
            }

            test("0 does nothing at only 14 life above the starting total") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, ajaniName)
                    .withCardOnBattlefield(2, "Savannah Lions")
                    .withCardOnBattlefield(2, "Bonesplitter")
                    .withLifeTotal(1, 34)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val ajani = game.findPermanent(ajaniName)!!
                game.activate(2)

                withClue("Ajani stays, loyalty unchanged") {
                    game.isOnBattlefield(ajaniName) shouldBe true
                    counters(game, ajani, CounterType.LOYALTY) shouldBe 5
                }
                withClue("the opponent's creature and artifact stay") {
                    game.isOnBattlefield("Savannah Lions") shouldBe true
                    game.isOnBattlefield("Bonesplitter") shouldBe true
                }
            }

            test("0 at 15 life above the starting total exiles Ajani and each opposing artifact and creature") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, ajaniName)
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Savannah Lions")
                    .withCardOnBattlefield(2, "Hill Giant")
                    .withCardOnBattlefield(2, "Bonesplitter")
                    .withLandsOnBattlefield(2, "Forest", 1)
                    .withLifeTotal(1, 35)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.activate(2)

                withClue("Ajani exiles himself") {
                    game.isInExile(1, ajaniName) shouldBe true
                }
                withClue("each artifact and creature the opponent controls is exiled") {
                    game.isInExile(2, "Savannah Lions") shouldBe true
                    game.isInExile(2, "Hill Giant") shouldBe true
                    game.isInExile(2, "Bonesplitter") shouldBe true
                }
                withClue("your own creature and the opponent's land are untouched") {
                    game.isOnBattlefield("Grizzly Bears") shouldBe true
                    game.isOnBattlefield("Forest") shouldBe true
                }
            }
        }
    }
}
