package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.gtc.cards.LazavDimirMastermind
import com.wingedsheep.mtg.sets.definitions.lea.cards.Clone
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Lazav, Dimir Mastermind — "Whenever a creature card is put into an opponent's graveyard from
 * anywhere, you may have Lazav become a copy of that card, except its name is Lazav, Dimir
 * Mastermind, it's legendary in addition to its other types, and it has hexproof and this ability."
 *
 * Pins the exceptions (name, legendary, hexproof, and the ability itself — so a second card makes
 * it copy again), the "you may", that your own cards don't count, and the Clone ruling: the copy
 * reads the card in the graveyard, so a dead Clone makes Lazav a 0/0 Clone that dies.
 */
class LazavDimirMastermindScenarioTest : FunSpec({

    fun createDriver(startingPlayer: Int = 0): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(LazavDimirMastermind, Clone))
        driver.initMirrorMatch(Deck.of("Mountain" to 40), skipMulligans = true, startingPlayer = startingPlayer)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun card(d: GameTestDriver, id: EntityId) = d.state.getEntity(id)!!.get<CardComponent>()!!

    fun boltAndResolve(d: GameTestDriver, caster: EntityId, target: EntityId) {
        val bolt = d.putCardInHand(caster, "Lightning Bolt")
        d.giveMana(caster, Color.RED, 1)
        d.castSpell(caster, bolt, targets = listOf(target)).error shouldBe null
        d.bothPass()
    }

    /** Bolt an opponent's creature, then let Lazav's trigger resolve up to its "you may". */
    fun boltAndReachMay(d: GameTestDriver, caster: EntityId, target: EntityId) {
        boltAndResolve(d, caster, target)
        d.bothPass()
        d.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
    }

    test("an opponent's creature dying: Lazav becomes a copy, keeping its name, legendary, hexproof and the ability") {
        val d = createDriver()
        val me = d.player1
        val lazav = d.putCreatureOnBattlefield(me, "Lazav, Dimir Mastermind")
        val lions = d.putCreatureOnBattlefield(d.player2, "Savannah Lions")

        boltAndReachMay(d, me, lions)
        d.submitYesNo(me, true).error shouldBe null

        val copied = card(d, lazav)
        withClue("a copy of Savannah Lions, except for the printed exceptions") {
            copied.name shouldBe "Lazav, Dimir Mastermind"
            copied.typeLine.isLegendary shouldBe true
            copied.typeLine.subtypes.map { it.value } shouldContain "Cat"
            d.state.projectedState.getPower(lazav) shouldBe 1
            d.state.projectedState.getToughness(lazav) shouldBe 1
            d.state.projectedState.hasKeyword(lazav, Keyword.HEXPROOF) shouldBe true
        }

        // "and this ability": a second card makes it copy again.
        val courser = d.putCreatureOnBattlefield(d.player2, "Centaur Courser")
        boltAndReachMay(d, me, courser)
        d.submitYesNo(me, true).error shouldBe null
        withClue("the retained ability copied Centaur Courser next") {
            card(d, lazav).name shouldBe "Lazav, Dimir Mastermind"
            d.state.projectedState.getPower(lazav) shouldBe 3
            d.state.projectedState.getToughness(lazav) shouldBe 3
            d.state.projectedState.hasKeyword(lazav, Keyword.HEXPROOF) shouldBe true
        }
    }

    test("declining leaves Lazav as it is; your own creature card doesn't trigger it") {
        val d = createDriver()
        val me = d.player1
        val lazav = d.putCreatureOnBattlefield(me, "Lazav, Dimir Mastermind")
        val theirs = d.putCreatureOnBattlefield(d.player2, "Savannah Lions")
        boltAndReachMay(d, me, theirs)
        d.submitYesNo(me, false).error shouldBe null
        d.state.projectedState.getPower(lazav) shouldBe 3

        val mine = d.putCreatureOnBattlefield(me, "Savannah Lions")
        boltAndResolve(d, me, mine)
        withClue("my own graveyard isn't an opponent's") {
            d.state.stack.isEmpty() shouldBe true
            d.pendingDecision shouldBe null
        }
    }

    test("a dead Clone copying a creature makes Lazav a 0/0 Clone, not whatever the Clone was copying") {
        val d = createDriver(startingPlayer = 1)
        val opponent = d.player2
        val me = d.player1
        val lazav = d.putCreatureOnBattlefield(me, "Lazav, Dimir Mastermind")
        val lions = d.putCreatureOnBattlefield(opponent, "Savannah Lions")

        val clone = d.putCardInHand(opponent, "Clone")
        d.giveMana(opponent, Color.BLUE, 4)
        d.castSpell(opponent, clone).error shouldBe null
        d.bothPass()
        d.submitCardSelection(opponent, listOf(lions)).error shouldBe null
        card(d, clone).name shouldBe "Savannah Lions"

        boltAndReachMay(d, opponent, clone)
        d.submitYesNo(me, true).error shouldBe null

        withClue("Lazav became the 0/0 Clone card and died to the toughness check") {
            d.state.getBattlefield().contains(lazav) shouldBe false
            d.getGraveyard(me) shouldContain lazav
        }
    }
})
