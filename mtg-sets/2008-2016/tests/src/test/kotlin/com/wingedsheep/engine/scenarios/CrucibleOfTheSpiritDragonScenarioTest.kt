package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.ChooseColorDecision
import com.wingedsheep.engine.core.ColorChosenResponse
import com.wingedsheep.engine.core.CycleCard
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.KeywordAbility
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Crucible of the Spirit Dragon (FRF #167).
 *
 * "{T}, Remove X storage counters from this land: Add X mana in any combination of colors. Spend
 * this mana only to cast Dragon spells or activate abilities of Dragons."
 *
 * Pins the X of the counter-removal cost feeding the any-combination amount, and both sides of the
 * spend restriction — a Dragon spell and a Dragon permanent's ability pay; a non-Dragon spell and a
 * Dragon card's ability from hand (2014-11-24 ruling) don't.
 */
class CrucibleOfTheSpiritDragonScenarioTest : ScenarioTestBase() {

    private val dragonSpell = card("Crucible Test Dragon") {
        manaCost = "{2}"
        typeLine = "Creature — Dragon"
        power = 2
        toughness = 2
    }

    private val goblinSpell = card("Crucible Test Goblin") {
        manaCost = "{2}"
        typeLine = "Creature — Goblin"
        power = 2
        toughness = 2
    }

    private val cyclingDragon = card("Crucible Test Cycling Dragon") {
        manaCost = "{5}"
        typeLine = "Creature — Dragon"
        power = 5
        toughness = 5
        keywordAbility(KeywordAbility.cycling("{2}"))
    }

    private val cashOutAbilityId
        get() = cardRegistry.requireCard("Crucible of the Spirit Dragon").activatedAbilities[2].id

    private fun TestGame.pool(): ManaPoolComponent = state.getEntity(player1Id)!!.get<ManaPoolComponent>()!!

    private fun TestGame.storageCounters(id: EntityId): Int =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.STORAGE) ?: 0

    private fun board() = scenario()
        .withPlayers("Player", "Opponent")
        .withCardOnBattlefield(1, "Crucible of the Spirit Dragon")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)

    /** Gives the Crucible [stored] storage counters, then removes [x] of them for [x] mana of [color]. */
    private fun TestGame.cashOut(stored: Int, x: Int, color: Color): EntityId {
        val crucible = findPermanent("Crucible of the Spirit Dragon")!!
        state = state.updateEntity(crucible) { c ->
            c.with((c.get<CountersComponent>() ?: CountersComponent()).withAdded(CounterType.STORAGE, stored))
        }
        val activation = execute(ActivateAbility(player1Id, crucible, cashOutAbilityId, xValue = x))
        withClue("Activating for X=$x should succeed: ${activation.error}") {
            activation.error shouldBe null
        }
        repeat(x) {
            val decision = getPendingDecision() as ChooseColorDecision
            submitDecision(ColorChosenResponse(decision.id, color)).error shouldBe null
        }
        return crucible
    }

    init {
        cardRegistry.register(dragonSpell)
        cardRegistry.register(goblinSpell)
        cardRegistry.register(cyclingDragon)

        test("removing X storage counters adds X restricted mana that pays for a Dragon spell") {
            val game = board().withCardInHand(1, "Crucible Test Dragon").build()
            val crucible = game.cashOut(stored = 3, x = 2, color = Color.RED)

            withClue("two counters paid for two Dragon-only mana") {
                game.pool().restrictedMana.size shouldBe 2
                game.storageCounters(crucible) shouldBe 1
            }

            val dragon = game.findCardsInHand(1, "Crucible Test Dragon").single()
            game.execute(CastSpell(game.player1Id, dragon, paymentStrategy = PaymentStrategy.FromPool))
                .error shouldBe null
            game.resolveStack()
            game.findPermanent("Crucible Test Dragon") shouldNotBe null
        }

        test("the mana can't pay for a non-Dragon spell") {
            val game = board().withCardInHand(1, "Crucible Test Goblin").build()
            game.cashOut(stored = 2, x = 2, color = Color.RED)

            val goblin = game.findCardsInHand(1, "Crucible Test Goblin").single()
            game.execute(CastSpell(game.player1Id, goblin, paymentStrategy = PaymentStrategy.FromPool))
                .error shouldNotBe null
            game.pool().restrictedMana.size shouldBe 2
        }

        test("the mana pays for an ability of a Dragon on the battlefield") {
            val game = board().withCardOnBattlefield(1, "Shivan Dragon").build()
            game.cashOut(stored = 1, x = 1, color = Color.RED)

            val shivan = game.findPermanent("Shivan Dragon")!!
            val firebreathing = cardRegistry.requireCard("Shivan Dragon").activatedAbilities.single().id
            game.execute(
                ActivateAbility(game.player1Id, shivan, firebreathing, paymentStrategy = PaymentStrategy.FromPool)
            ).error shouldBe null
            game.pool().restrictedMana.size shouldBe 0
        }

        test("the mana can't pay for an ability of a Dragon card that isn't on the battlefield") {
            val game = board().withCardInHand(1, "Crucible Test Cycling Dragon").build()
            game.cashOut(stored = 2, x = 2, color = Color.RED)

            val inHand = game.findCardsInHand(1, "Crucible Test Cycling Dragon").single()
            game.execute(CycleCard(game.player1Id, inHand, paymentStrategy = PaymentStrategy.FromPool))
                .error shouldNotBe null
            game.isInHand(1, "Crucible Test Cycling Dragon") shouldBe true
            game.pool().restrictedMana.size shouldBe 2
        }
    }
}
