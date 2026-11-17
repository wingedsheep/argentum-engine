package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CardsSelectedResponse
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.frf.cards.TasigurTheGoldenFang
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Tasigur, the Golden Fang (FRF #87) — {5}{B} 4/5 Legendary Human Shaman. "Delve.
 * {2}{G/U}{G/U}: Mill two cards, then return a nonland card of an opponent's choice from your
 * graveyard to your hand."
 *
 * Covers what the script alone can't show: the selection is made by the opponent, it is gathered
 * after the mill (so a milled card is eligible alongside older graveyard cards), lands are excluded,
 * and with no nonland card in the graveyard the ability resolves returning nothing.
 */
class TasigurTheGoldenFangScenarioTest : FunSpec({

    data class Setup(val d: GameTestDriver, val you: EntityId, val opponent: EntityId, val tasigur: EntityId)

    fun setUp(): Setup {
        val d = GameTestDriver().apply {
            registerCards(TestCards.all + TasigurTheGoldenFang)
            initMirrorMatch(deck = Deck.of("Swamp" to 40), skipMulligans = true)
        }
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val you = d.activePlayer!!
        val tasigur = d.putCreatureOnBattlefield(you, "Tasigur, the Golden Fang")
        return Setup(d, you, d.getOpponent(you), tasigur)
    }

    fun Setup.activate() {
        d.giveMana(you, Color.GREEN, 2)
        d.giveColorlessMana(you, 2)
        d.submitSuccess(ActivateAbility(you, tasigur, TasigurTheGoldenFang.activatedAbilities.first().id))
        d.bothPass()
    }

    test("mills two, then the opponent picks which nonland card returns — milled cards included") {
        val s = setUp()
        val (d, you, opponent) = s
        val swamp = d.putCardInGraveyard(you, "Swamp")
        val bears = d.putCardInGraveyard(you, "Grizzly Bears")
        val forest = d.putCardOnTopOfLibrary(you, "Forest")
        val bolt = d.putCardOnTopOfLibrary(you, "Lightning Bolt")

        s.activate()

        val decision = d.pendingDecision
        decision.shouldBeInstanceOf<SelectCardsDecision>()
        withClue("the opponent makes the choice") { decision.playerId shouldBe opponent }
        withClue("only nonland cards, the freshly milled Bolt among them") {
            decision.options shouldContainExactlyInAnyOrder listOf(bears, bolt)
        }
        d.submitDecision(opponent, CardsSelectedResponse(decision.id, listOf(bears)))

        d.getHand(you) shouldContain bears
        d.getGraveyard(you) shouldContainExactlyInAnyOrder listOf(swamp, forest, bolt)
    }

    test("with no nonland card in the graveyard, nothing is returned") {
        val s = setUp()
        val (d, you, _) = s
        val swamp = d.putCardInGraveyard(you, "Swamp")
        val forestA = d.putCardOnTopOfLibrary(you, "Forest")
        val forestB = d.putCardOnTopOfLibrary(you, "Forest")
        val handBefore = d.getHand(you)

        s.activate()

        d.pendingDecision shouldBe null
        d.getHand(you) shouldBe handBefore
        d.getGraveyard(you) shouldContainExactlyInAnyOrder listOf(swamp, forestA, forestB)
    }
})
