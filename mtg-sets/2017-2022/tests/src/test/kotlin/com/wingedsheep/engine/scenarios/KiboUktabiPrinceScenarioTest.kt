package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.j22.cards.KiboUktabiPrince
import com.wingedsheep.mtg.sets.tokens.PredefinedTokens
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Kibo, Uktabi Prince (J22) — "{T}: Each player creates a Banana token. / Whenever an artifact an
 * opponent controls is put into a graveyard from the battlefield, put a +1/+1 counter on each
 * creature you control that's an Ape or a Monkey. / Whenever Kibo attacks, defending player
 * sacrifices an artifact of their choice."
 *
 * Pins the per-player Banana creation, the opponent-artifact-dies counter (fed by the opponent
 * cracking their own Banana, which also gains them 2 life), and the attack-trigger sacrifice.
 */
class KiboUktabiPrinceScenarioTest : ScenarioTestBase() {

    private val kiboAbilityId = KiboUktabiPrince.activatedAbilities.single().id
    private val bananaAbilityId = PredefinedTokens.Banana.activatedAbilities.single().id

    private fun TestGame.plusOneCounters(id: EntityId): Int =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    init {
        test("tapping Kibo gives each player a Banana they control") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Kibo, Uktabi Prince")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val kibo = game.findPermanent("Kibo, Uktabi Prince")!!
            game.execute(ActivateAbility(game.player1Id, kibo, kiboAbilityId)).error shouldBe null
            game.resolveStack()

            withClue("one Banana per player, each controlled by its own player") {
                val bananas = game.findPermanents("Banana")
                bananas.size shouldBe 2
                bananas.map { game.state.projectedState.getController(it) }.toSet() shouldBe
                    setOf(game.player1Id, game.player2Id)
            }
        }

        test("an opponent cracking their Banana gains them 2 life and grows Kibo") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Kibo, Uktabi Prince")
                .withCardOnBattlefield(2, "Banana", isToken = true)
                .withActivePlayer(1)
                .withPriorityPlayer(2)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val kibo = game.findPermanent("Kibo, Uktabi Prince")!!
            val banana = game.findPermanent("Banana")!!
            game.execute(
                ActivateAbility(
                    playerId = game.player2Id,
                    sourceId = banana,
                    abilityId = bananaAbilityId,
                    manaColorChoice = Color.GREEN,
                )
            ).error shouldBe null
            game.resolveStack()

            withClue("the Banana's controller gained 2 life") {
                game.getLifeTotal(2) shouldBe 22
                game.findPermanent("Banana") shouldBe null
            }
            withClue("Kibo (a Monkey) got a +1/+1 counter from the opponent's artifact dying") {
                game.plusOneCounters(kibo) shouldBe 1
                game.state.projectedState.getPower(kibo) shouldBe 3
            }
        }

        test("cracking your own Banana does not grow Kibo") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Kibo, Uktabi Prince")
                .withCardOnBattlefield(1, "Banana", isToken = true)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val kibo = game.findPermanent("Kibo, Uktabi Prince")!!
            val banana = game.findPermanent("Banana")!!
            game.execute(
                ActivateAbility(
                    playerId = game.player1Id,
                    sourceId = banana,
                    abilityId = bananaAbilityId,
                    manaColorChoice = Color.RED,
                )
            ).error shouldBe null
            game.resolveStack()

            withClue("only an opponent's artifact feeds the trigger") {
                game.getLifeTotal(1) shouldBe 22
                game.plusOneCounters(kibo) shouldBe 0
            }
        }

        test("Kibo attacking makes the defending player sacrifice an artifact") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Kibo, Uktabi Prince")
                .withCardOnBattlefield(2, "Banana", isToken = true)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val kibo = game.findPermanent("Kibo, Uktabi Prince")!!
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Kibo, Uktabi Prince" to 2)).error shouldBe null
            game.resolveStack()

            withClue("the defender's only artifact was sacrificed, which in turn grew Kibo") {
                game.findPermanent("Banana") shouldBe null
                game.getLifeTotal(2) shouldBe 20
                game.plusOneCounters(kibo) shouldBe 1
            }
        }
    }
}
