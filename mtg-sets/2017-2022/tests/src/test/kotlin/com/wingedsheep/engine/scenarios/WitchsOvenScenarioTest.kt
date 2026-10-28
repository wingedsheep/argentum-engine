package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.eld.cards.WitchsOven
import com.wingedsheep.mtg.sets.tokens.PredefinedTokens
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Witch's Oven (ELD #237) — {T}, Sacrifice a creature: Create a Food token. If the sacrificed
 * creature's toughness was 4 or greater, create two Food tokens instead.
 *
 * Pins the cost-linked branch: the toughness is the sacrificed creature's last-known toughness
 * (a pump counts), and 3 versus 4 is the boundary.
 */
class WitchsOvenScenarioTest : FunSpec({
    val threeToughness = card("Oven Test Three") {
        manaCost = "{1}"
        typeLine = "Creature — Human"
        power = 1
        toughness = 3
    }
    val fourToughness = card("Oven Test Four") {
        manaCost = "{1}"
        typeLine = "Creature — Human"
        power = 1
        toughness = 4
    }

    fun driver() = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(WitchsOven, threeToughness, fourToughness) + PredefinedTokens.allTokens)
        initMirrorMatch(deck = Deck.of("Forest" to 40), skipMulligans = true, startingPlayer = 0)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun foods(d: GameTestDriver, player: EntityId): Int =
        d.getPermanents(player).count { d.state.projectedState.hasSubtype(it, "Food") }

    fun bake(d: GameTestDriver, oven: EntityId, victim: EntityId) {
        d.submit(
            ActivateAbility(
                d.player1, oven, WitchsOven.activatedAbilities.single().id,
                costPayment = AdditionalCostPayment(sacrificedPermanents = listOf(victim)),
            )
        ).error shouldBe null
        d.bothPass()
        d.stackSize shouldBe 0
    }

    test("a creature with toughness 3 makes one Food") {
        val d = driver()
        val oven = d.putPermanentOnBattlefield(d.player1, WitchsOven.name)
        val victim = d.putCreatureOnBattlefield(d.player1, threeToughness.name)
        bake(d, oven, victim)
        d.findPermanent(d.player1, threeToughness.name) shouldBe null
        foods(d, d.player1) shouldBe 1
    }

    test("a creature with toughness 4 makes two Foods") {
        val d = driver()
        val oven = d.putPermanentOnBattlefield(d.player1, WitchsOven.name)
        val victim = d.putCreatureOnBattlefield(d.player1, fourToughness.name)
        bake(d, oven, victim)
        foods(d, d.player1) shouldBe 2
    }

    test("the sacrificed creature's last-known toughness counts, pumps included") {
        val d = driver()
        val oven = d.putPermanentOnBattlefield(d.player1, WitchsOven.name)
        val victim = d.putCreatureOnBattlefield(d.player1, threeToughness.name)
        val growth = d.putCardInHand(d.player1, "Giant Growth")
        d.giveMana(d.player1, Color.GREEN, 1)
        d.castSpell(d.player1, growth, listOf(victim)).error shouldBe null
        d.bothPass()
        d.state.projectedState.getToughness(victim) shouldBe 6
        bake(d, oven, victim)
        withClue("toughness 6 at the moment of sacrifice, not the printed 3") {
            foods(d, d.player1) shouldBe 2
        }
    }
})
