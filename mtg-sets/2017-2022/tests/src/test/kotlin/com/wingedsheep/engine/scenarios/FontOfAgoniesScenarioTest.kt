package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for **Font of Agonies** (RNA #74).
 *
 * "Whenever you pay life" is `Triggers.you.paysLife()`: it fires on a life *payment* only — not on
 * a "loses N life" effect, not on damage, not on an opponent's payment — and "that many" is the life
 * the payment took. Festival of Embers' graveyard-cast life cost is a payment too.
 */
class FontOfAgoniesScenarioTest : ScenarioTestBase() {

    private val lifeTap = card("Test Life Tap") {
        manaCost = "{1}"
        typeLine = "Artifact"
        activatedAbility {
            cost = Costs.PayLife(3)
            effect = Effects.GainLife(0)
        }
    }

    private val lifeDrain = card("Test Life Drain") {
        manaCost = "{1}"
        typeLine = "Artifact"
        activatedAbility {
            cost = Costs.Mana("{0}")
            effect = Effects.LoseLife(3, EffectTarget.Controller)
        }
    }

    private fun TestGame.bloodCounters(): Int =
        state.getEntity(findPermanent("Font of Agonies")!!)?.get<CountersComponent>()?.getCount(CounterType.BLOOD) ?: 0

    private fun TestGame.activate(playerNumber: Int, cardName: String, targets: List<ChosenTarget> = emptyList()) =
        execute(
            ActivateAbility(
                playerId = if (playerNumber == 1) player1Id else player2Id,
                sourceId = findPermanent(cardName)!!,
                abilityId = cardRegistry.getCard(cardName)!!.script.activatedAbilities.first().id,
                targets = targets,
            )
        )

    init {
        cardRegistry.register(lifeTap)
        cardRegistry.register(lifeDrain)

        test("paying 3 life puts three blood counters on it") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Font of Agonies")
                .withCardOnBattlefield(1, "Test Life Tap")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.activate(1, "Test Life Tap").error shouldBe null
            game.resolveStack()

            game.getLifeTotal(1) shouldBe 17
            game.bloodCounters() shouldBe 3
        }

        test("losing life, taking damage and an opponent's payment add no counters") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Font of Agonies")
                .withCardOnBattlefield(1, "Test Life Drain")
                .withCardOnBattlefield(2, "Test Life Tap")
                .withCardInHand(2, "Lightning Bolt")
                .withLandsOnBattlefield(2, "Mountain", 1)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.activate(1, "Test Life Drain").error shouldBe null
            game.resolveStack()
            withClue("a \"loses 3 life\" effect isn't a payment") {
                game.getLifeTotal(1) shouldBe 17
                game.bloodCounters() shouldBe 0
            }

            game.passPriority()
            val bolt = game.state.getHand(game.player2Id).first {
                game.state.getEntity(it)?.get<CardComponent>()?.name == "Lightning Bolt"
            }
            game.execute(CastSpell(game.player2Id, bolt, listOf(ChosenTarget.Player(game.player1Id)))).error shouldBe null
            game.activate(2, "Test Life Tap").error shouldBe null
            game.resolveStack()
            withClue("damage and the opponent's own payment don't count") {
                game.getLifeTotal(1) shouldBe 14
                game.getLifeTotal(2) shouldBe 17
                game.bloodCounters() shouldBe 0
            }
        }

        test("Festival of Embers' graveyard life cost is a life payment") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Font of Agonies")
                .withCardOnBattlefield(1, "Festival of Embers")
                .withCardInGraveyard(1, "Lightning Bolt")
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bolt = game.state.getGraveyard(game.player1Id).first()
            game.execute(
                CastSpell(game.player1Id, bolt, listOf(ChosenTarget.Player(game.player2Id)), graveyardLifeCost = 1)
            ).error shouldBe null
            game.resolveStack()

            game.getLifeTotal(1) shouldBe 19
            game.getLifeTotal(2) shouldBe 17
            game.bloodCounters() shouldBe 1
        }

        test("{1}{B}, remove four blood counters: destroy target creature") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Font of Agonies")
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withLandsOnBattlefield(1, "Swamp", 2)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val font = game.findPermanent("Font of Agonies")!!
            game.state = game.state.updateEntity(font) {
                it.with(CountersComponent(mapOf(CounterType.BLOOD to 5)))
            }

            val bears = game.findPermanent("Grizzly Bears")!!
            game.activate(1, "Font of Agonies", listOf(ChosenTarget.Permanent(bears))).error shouldBe null
            game.resolveStack()

            game.isOnBattlefield("Grizzly Bears") shouldBe false
            game.bloodCounters() shouldBe 1
        }
    }
}
