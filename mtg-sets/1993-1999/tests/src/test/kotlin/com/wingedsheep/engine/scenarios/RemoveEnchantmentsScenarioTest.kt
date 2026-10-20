package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.battlefield.AttachmentsComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.shouldBe

/**
 * Remove Enchantments [LEG 33] — {W} Instant
 *
 * "Return to your hand all enchantments you both own and control, all Auras you own attached to
 * permanents you control, and all Auras you own attached to attacking creatures your opponents
 * control. Then destroy all other enchantments you control, all other Auras attached to permanents
 * you control, and all other Auras attached to attacking creatures your opponents control."
 *
 * Every group turns on a different owner/controller split, so the board below puts one enchantment
 * in each cell — including the ones the spell must leave alone — and casts it mid-combat while an
 * opposing creature is attacking.
 */
class RemoveEnchantmentsScenarioTest : FunSpec({

    fun GameTestDriver.attach(auraId: EntityId, hostId: EntityId) {
        addComponent(auraId, AttachedToComponent(hostId))
        val existing = state.getEntity(hostId)?.get<AttachmentsComponent>()?.attachedIds ?: emptyList()
        addComponent(hostId, AttachmentsComponent(existing + auraId))
    }

    fun GameTestDriver.aura(owner: EntityId, controller: EntityId, host: EntityId): EntityId {
        val id = putPermanentOnBattlefield(owner, "Fishliver Oil")
        if (controller != owner) addComponent(id, ControllerComponent(controller))
        attach(id, host)
        return id
    }

    test("returns what you own in each group and destroys the rest of each group") {
        val d = GameTestDriver()
        d.registerCards(TestCards.all)
        d.initMirrorMatch(deck = Deck.of("Plains" to 40), skipMulligans = true)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val opp = d.activePlayer!!
        val me = d.getOpponent(opp)

        val myBear = d.putCreatureOnBattlefield(me, "Grizzly Bears")
        val oppAttacker = d.putCreatureOnBattlefield(opp, "Grizzly Bears")
        d.removeSummoningSickness(oppAttacker)
        val oppIdle = d.putCreatureOnBattlefield(opp, "Savannah Lions")

        // Returned to my hand.
        val myAnthem = d.putPermanentOnBattlefield(me, "Glorious Anthem")
        val myAuraOnMyBearOppControls = d.aura(owner = me, controller = opp, host = myBear)
        val myAuraOnAttackerOppControls = d.aura(owner = me, controller = opp, host = oppAttacker)

        // Destroyed.
        val oppAuraOnMyBear = d.aura(owner = opp, controller = opp, host = myBear)
        val oppAuraOnAttacker = d.aura(owner = opp, controller = opp, host = oppAttacker)
        val oppAnthemIControl = d.putPermanentOnBattlefield(opp, "Glorious Anthem")
        d.addComponent(oppAnthemIControl, ControllerComponent(me))

        // Untouched.
        val myAuraOnIdleOppControls = d.aura(owner = me, controller = opp, host = oppIdle)
        val oppAuraOnIdle = d.aura(owner = opp, controller = opp, host = oppIdle)
        val oppAnthem = d.putPermanentOnBattlefield(opp, "Glorious Anthem")

        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(opp, listOf(oppAttacker), me).error shouldBe null
        d.passPriority(opp)

        val spell = d.putCardInHand(me, "Remove Enchantments")
        d.giveMana(me, Color.WHITE, 1)
        d.castSpell(me, spell).error shouldBe null
        d.bothPass()

        withClue("enchantments I own, in each of the three groups, return to my hand") {
            d.state.getHand(me) shouldContainAll listOf(
                myAnthem, myAuraOnMyBearOppControls, myAuraOnAttackerOppControls
            )
        }
        withClue("the other enchantments in those groups are destroyed") {
            d.state.getGraveyard(opp) shouldContainAll listOf(
                oppAuraOnMyBear, oppAuraOnAttacker, oppAnthemIControl
            )
        }
        withClue("enchantments outside every group stay on the battlefield") {
            val battlefield = d.state.getBattlefield()
            battlefield shouldContain myAuraOnIdleOppControls
            battlefield shouldContain oppAuraOnIdle
            battlefield shouldContain oppAnthem
        }
        withClue("the creatures themselves are untouched") {
            d.state.getBattlefield() shouldContainAll listOf(myBear, oppAttacker, oppIdle)
        }
    }
})
