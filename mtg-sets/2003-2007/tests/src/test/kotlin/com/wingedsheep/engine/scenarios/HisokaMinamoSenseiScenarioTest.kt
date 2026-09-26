package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.chk.cards.HisokaMinamoSensei
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Hisoka, Minamo Sensei (CHK #66) — "{2}{U}, Discard a card: Counter target spell if it has the
 * same mana value as the discarded card."
 *
 * The discard is an activation cost, so the ability has to remember which card it was: the
 * comparison at resolution reads that card's mana value from the graveyard. The victim is the
 * active player casting Centaur Courser (mana value 3); Hisoka's controller responds.
 */
class HisokaMinamoSenseiScenarioTest : FunSpec({

    val abilityId = HisokaMinamoSensei.activatedAbilities.first().id

    fun driver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + HisokaMinamoSensei)
        d.initMirrorMatch(deck = Deck.of("Island" to 40), skipMulligans = true, startingPlayer = 0)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return d
    }

    /** Player 1 casts Centaur Courser; player 2 answers with Hisoka, discarding [discardName]. */
    fun GameTestDriver.hisokaVersusCourser(discardName: String): Pair<EntityId, EntityId> {
        val victim = player1
        val caster = player2
        val hisoka = putCreatureOnBattlefield(caster, "Hisoka, Minamo Sensei")
        giveMana(victim, Color.GREEN, 3)
        val courser = putCardInHand(victim, "Centaur Courser")
        castSpell(victim, courser).outcome shouldBe Outcome.Done
        passPriority(victim)

        giveMana(caster, Color.BLUE, 3)
        val discarded = putCardInHand(caster, discardName)
        val activation = submit(
            ActivateAbility(
                playerId = caster,
                sourceId = hisoka,
                abilityId = abilityId,
                targets = listOf(ChosenTarget.Spell(courser)),
                costPayment = AdditionalCostPayment(discardedCards = listOf(discarded)),
            )
        )
        withClue(activation.error ?: "activating Hisoka failed") { activation.outcome shouldBe Outcome.Done }
        withClue("the discard is paid on activation") { getGraveyard(caster).contains(discarded) shouldBe true }
        bothPass()
        return courser to discarded
    }

    test("counters the spell when the discarded card has the same mana value") {
        val d = driver()
        d.hisokaVersusCourser(discardName = "Palladium Myr")

        d.getGraveyardCardNames(d.player1).contains("Centaur Courser") shouldBe true
        d.findPermanent(d.player1, "Centaur Courser") shouldBe null
    }

    test("does not counter the spell when the mana values differ, but the discard stays paid") {
        val d = driver()
        val (_, discarded) = d.hisokaVersusCourser(discardName = "Savannah Lions")

        // The ability resolved and did nothing; the Courser is still on the stack until it resolves.
        d.state.stack.isEmpty() shouldBe false
        d.bothPass()
        d.findPermanent(d.player1, "Centaur Courser") shouldNotBe null
        d.getGraveyard(d.player2).contains(discarded) shouldBe true
    }

    test("a land card has mana value 0 and does not match a three-drop") {
        val d = driver()
        d.hisokaVersusCourser(discardName = "Island")

        d.bothPass()
        d.findPermanent(d.player1, "Centaur Courser") shouldNotBe null
    }
})
