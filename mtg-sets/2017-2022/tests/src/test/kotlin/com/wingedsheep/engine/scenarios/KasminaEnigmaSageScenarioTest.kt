package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.GrantActivatedAbility
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Kasmina, Enigma Sage (STX #196) — {1}{G}{U} Legendary Planeswalker, loyalty 2:
 *   Each other planeswalker you control has the loyalty abilities of Kasmina.
 *   +2: Scry 1.
 *   −X: Create a 0/0 green and blue Fractal creature token. Put X +1/+1 counters on it.
 *   −8: Search your library for an instant or sorcery card that shares a color with this
 *   planeswalker, exile that card, then shuffle. You may cast that card without paying its mana cost.
 *
 * Pins the granted-loyalty-ability shape: another planeswalker activates Kasmina's abilities with
 * its own loyalty and its own once-per-turn budget, and the −8's "this planeswalker" is the host
 * (white Ajani finds a white instant, not Kasmina's blue one).
 */
class KasminaEnigmaSageScenarioTest : ScenarioTestBase() {
    init {
        val kasminaCard = cardRegistry.getCard("Kasmina, Enigma Sage")!!
        val kasminaMinusX = kasminaCard.script.activatedAbilities.single { it.cost is AbilityCost.LoyaltyX }.id
        val grants = kasminaCard.script.staticAbilities.filterIsInstance<GrantActivatedAbility>()
        val grantedMinusX = grants.single { it.ability.cost is AbilityCost.LoyaltyX }.ability.id
        val grantedMinusEight = grants.single { (it.ability.cost as? AbilityCost.Loyalty)?.change == -8 }.ability.id
        val ajaniPlusOne = cardRegistry.getCard("Ajani Goldmane")!!.script.activatedAbilities
            .single { (it.cost as? AbilityCost.Loyalty)?.change == 1 }.id

        fun base() = scenario().withPlayers()
            .withCardOnBattlefield(1, "Kasmina, Enigma Sage")
            .withCardOnBattlefield(1, "Ajani Goldmane")
            .withCardInLibrary(1, "Island")
            .withCardInLibrary(2, "Island")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)

        fun TestGame.loyalty(id: EntityId): Int =
            state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.LOYALTY) ?: 0

        test("Kasmina's −X creates a Fractal with X +1/+1 counters") {
            val game = base().build()
            val kasmina = game.findPermanent("Kasmina, Enigma Sage")!!

            game.execute(ActivateAbility(game.player1Id, kasmina, kasminaMinusX, xValue = 1)).error shouldBe null
            game.resolveStack()

            val fractal = game.findPermanent("Fractal Token")!!
            game.state.projectedState.getPower(fractal) shouldBe 1
            game.state.projectedState.getToughness(fractal) shouldBe 1
            game.loyalty(kasmina) shouldBe 1
        }

        test("another planeswalker activates Kasmina's −X with its own loyalty, once per turn") {
            val game = base().build()
            val kasmina = game.findPermanent("Kasmina, Enigma Sage")!!
            val ajani = game.findPermanent("Ajani Goldmane")!!

            game.execute(ActivateAbility(game.player1Id, ajani, grantedMinusX, xValue = 3)).error shouldBe null
            game.resolveStack()

            val fractal = game.findPermanent("Fractal Token")!!
            game.state.projectedState.getPower(fractal) shouldBe 3
            withClue("the cost came off Ajani, not Kasmina") {
                game.loyalty(ajani) shouldBe 1
                game.loyalty(kasmina) shouldBe 2
            }
            withClue("Ajani has already used its one loyalty activation this turn") {
                game.execute(ActivateAbility(game.player1Id, ajani, ajaniPlusOne)).error shouldNotBe null
            }
        }

        test("the granted −8 searches for a card sharing a color with the host planeswalker") {
            val game = base()
                .withCardInLibrary(1, "Counterspell")
                .withCardInLibrary(1, "Swords to Plowshares")
                .build()
            val ajani = game.findPermanent("Ajani Goldmane")!!
            game.state = game.state.updateEntity(ajani) {
                it.with(CountersComponent(mapOf(CounterType.LOYALTY to 9)))
            }
            val swords = game.findCardsInLibrary(1, "Swords to Plowshares").single()

            game.execute(ActivateAbility(game.player1Id, ajani, grantedMinusEight)).error shouldBe null
            game.resolveStack()

            val search = game.getPendingDecision()
            withClue("white Ajani finds the white instant, not blue Counterspell: $search") {
                (search as SelectCardsDecision).options shouldBe listOf(swords)
            }
            game.selectCards(listOf(swords)).error shouldBe null
            withClue("casting it is optional") { game.hasPendingDecision() shouldBe true }
            game.answerYesNo(false).error shouldBe null

            game.isInExile(1, "Swords to Plowshares") shouldBe true
            game.loyalty(ajani) shouldBe 1
        }
    }
}
