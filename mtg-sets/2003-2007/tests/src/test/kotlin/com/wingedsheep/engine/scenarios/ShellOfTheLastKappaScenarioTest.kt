package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.chk.cards.ShellOfTheLastKappa
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Shell of the Last Kappa — {3} Legendary Artifact (Champions of Kamigawa #269)
 *
 * "{3}, {T}: Exile target instant or sorcery spell that targets you. (The spell has no effect.)
 * {3}, {T}, Sacrifice Shell of the Last Kappa: You may cast a spell from among cards exiled with
 * Shell of the Last Kappa without paying its mana cost."
 *
 * The opponent (active player) aims Lightning Bolt at us or at our creature. The first ability may
 * only target the Bolt aimed at us (`targetsPlayer(Player.You)`); the second ability, after the
 * Shell is sacrificed, recasts the exiled Bolt for free under our control.
 */
class ShellOfTheLastKappaScenarioTest : FunSpec({

    val exileAbility = ShellOfTheLastKappa.activatedAbilities[0].id
    val castAbility = ShellOfTheLastKappa.activatedAbilities[1].id

    data class Board(val driver: GameTestDriver, val me: EntityId, val opp: EntityId, val shell: EntityId)

    fun board(): Board {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + ShellOfTheLastKappa)
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20, startingPlayer = 1)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val opp = driver.activePlayer!!
        val me = driver.getOpponent(opp)
        val shell = driver.putPermanentOnBattlefield(me, "Shell of the Last Kappa")
        return Board(driver, me, opp, shell)
    }

    /** The opponent casts Lightning Bolt at [target] and passes priority to us. */
    fun Board.oppBolts(target: EntityId): EntityId {
        val bolt = driver.putCardInHand(opp, "Lightning Bolt")
        driver.giveMana(opp, Color.RED, 1)
        driver.castSpell(opp, bolt, listOf(target)).outcome shouldBe Outcome.Done
        driver.passPriority(opp)
        return bolt
    }

    fun Board.exileWithShell(spell: EntityId) = driver.run {
        giveColorlessMana(me, 3)
        submit(ActivateAbility(me, shell, exileAbility, targets = listOf(ChosenTarget.Spell(spell))))
    }

    test("exiles an instant that targets you; the spell has no effect") {
        val b = board()
        val bolt = b.oppBolts(b.me)

        b.exileWithShell(bolt).outcome shouldBe Outcome.Done
        b.driver.bothPass()

        withClue("the Bolt was exiled, not resolved") {
            b.driver.getExile(b.opp).contains(bolt) shouldBe true
            b.driver.getLifeTotal(b.me) shouldBe 20
        }
    }

    test("can't target a spell that targets only your creature") {
        val b = board()
        val bears = b.driver.putCreatureOnBattlefield(b.me, "Grizzly Bears")
        val bolt = b.oppBolts(bears)

        withClue("a spell that doesn't target you is not a legal target") {
            b.exileWithShell(bolt).outcome shouldNotBe Outcome.Done
        }
    }

    test("can't target a spell that targets another player") {
        val b = board()
        val bolt = b.oppBolts(b.opp)

        withClue("'you' is read relative to the Shell's controller, not the spell's") {
            b.exileWithShell(bolt).outcome shouldNotBe Outcome.Done
        }
    }

    test("sacrifice: cast the exiled spell without paying its mana cost") {
        val b = board()
        val bolt = b.oppBolts(b.me)
        b.exileWithShell(bolt).outcome shouldBe Outcome.Done
        b.driver.bothPass()

        b.driver.untapPermanent(b.shell)
        b.driver.giveColorlessMana(b.me, 3)
        // Active player holds priority after the ability resolved; hand it to us.
        if (b.driver.state.priorityPlayerId == b.opp) b.driver.passPriority(b.opp)
        b.driver.submit(ActivateAbility(b.me, b.shell, castAbility)).outcome shouldBe Outcome.Done
        b.driver.bothPass()

        withClue("the Shell was sacrificed as a cost") {
            b.driver.assertInGraveyard(b.me, "Shell of the Last Kappa")
        }
        b.driver.submitCardSelection(b.me, listOf(bolt))
        b.driver.submitTargetSelection(b.me, listOf(b.opp)).outcome shouldBe Outcome.Done
        b.driver.bothPass()

        withClue("we cast the opponent's Bolt for free and it hit them") {
            b.driver.getLifeTotal(b.opp) shouldBe 17
            b.driver.getExile(b.opp).contains(bolt) shouldBe false
        }
    }

    test("sacrifice: choosing nothing declines, and the card stays exiled") {
        val b = board()
        val bolt = b.oppBolts(b.me)
        b.exileWithShell(bolt).outcome shouldBe Outcome.Done
        b.driver.bothPass()

        b.driver.untapPermanent(b.shell)
        b.driver.giveColorlessMana(b.me, 3)
        if (b.driver.state.priorityPlayerId == b.opp) b.driver.passPriority(b.opp)
        b.driver.submit(ActivateAbility(b.me, b.shell, castAbility)).outcome shouldBe Outcome.Done
        b.driver.bothPass()
        b.driver.submitCardSelection(b.me, emptyList()).outcome shouldBe Outcome.Done

        b.driver.getExile(b.opp).contains(bolt) shouldBe true
        b.driver.getLifeTotal(b.opp) shouldBe 20
    }
})
