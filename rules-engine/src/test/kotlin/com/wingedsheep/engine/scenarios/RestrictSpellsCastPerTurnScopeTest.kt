package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.legalactions.LegalActionEnumerator
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.RestrictSpellsCastPerTurn
import com.wingedsheep.sdk.scripting.references.Player
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * [RestrictSpellsCastPerTurn]'s `affected` who-axis, read relative to the source's controller:
 * `You` caps only the controller, `EachOpponent` only the others, `Each` everyone, and
 * `EnchantedPlayer` whoever the Aura is attached to, whatever its controller. Each case puts one spell on the tally of both
 * players and asks who may still cast a second.
 */
class RestrictSpellsCastPerTurnScopeTest : FunSpec({
    fun cap(name: String, affected: Player) = card(name) {
        typeLine = "Enchantment"
        staticAbility { ability = RestrictSpellsCastPerTurn(maxPerTurn = 1, affected = affected) }
    }
    val youCap = cap("You Cap", Player.You)
    val opponentCap = cap("Opponent Cap", Player.EachOpponent)
    val eachCap = cap("Each Cap", Player.Each)
    val enchantedCap = cap("Enchanted Cap", Player.EnchantedPlayer)

    fun driver() = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(youCap, opponentCap, eachCap, enchantedCap))
        initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    /** Who of (me, opponent) can still cast a Lightning Bolt with one spell already cast this turn. */
    fun GameTestDriver.whoCanCastASecond(): Pair<Boolean, Boolean> {
        val me = activePlayer!!
        val opp = getOpponent(me)
        replaceState(state.copy(playerSpellsCastThisTurn = mapOf(me to 1, opp to 1)))
        fun can(p: EntityId): Boolean {
            val bolt = putCardInHand(p, "Lightning Bolt")
            giveMana(p, Color.RED, 1)
            return LegalActionEnumerator.create(cardRegistry).enumerate(state, p)
                .any { (it.action as? CastSpell)?.cardId == bolt }
        }
        return can(me) to can(opp)
    }

    test("You caps only the controller") {
        val d = driver()
        d.putPermanentOnBattlefield(d.activePlayer!!, youCap.name)
        d.whoCanCastASecond() shouldBe (false to true)
    }

    test("EachOpponent caps only the controller's opponents") {
        val d = driver()
        d.putPermanentOnBattlefield(d.activePlayer!!, opponentCap.name)
        d.whoCanCastASecond() shouldBe (true to false)
    }

    test("Each caps every player") {
        val d = driver()
        d.putPermanentOnBattlefield(d.activePlayer!!, eachCap.name)
        d.whoCanCastASecond() shouldBe (false to false)
    }

    test("EnchantedPlayer caps the player the Aura is attached to, not its controller") {
        val d = driver()
        val me = d.activePlayer!!
        val opp = d.getOpponent(me)

        val onOpp = d.putPermanentOnBattlefield(me, enchantedCap.name)
        d.replaceState(d.state.updateEntity(onOpp) { it.with(AttachedToComponent(opp)) })
        withClue("my Aura on the opponent caps the opponent") { d.whoCanCastASecond() shouldBe (true to false) }
    }

    test("EnchantedPlayer on its own controller caps the controller") {
        val d = driver()
        val me = d.activePlayer!!
        val onMe = d.putPermanentOnBattlefield(me, enchantedCap.name)
        d.replaceState(d.state.updateEntity(onMe) { it.with(AttachedToComponent(me)) })
        d.whoCanCastASecond() shouldBe (false to true)
    }

    test("EnchantedPlayer attached to nothing caps nobody") {
        val d = driver()
        d.putPermanentOnBattlefield(d.activePlayer!!, enchantedCap.name)
        d.whoCanCastASecond() shouldBe (true to true)
    }
})
