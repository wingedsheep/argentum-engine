package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ForetellCard
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.OwnerComponent
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.ManaExpiry
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class SuChiCaveGuardScenarioTest : FunSpec({
    fun driver() = GameTestDriver().apply {
        registerCards(TestCards.all)
        initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }
    fun GameTestDriver.pool(player: EntityId) = state.getEntity(player)!!.get<ManaPoolComponent>()!!
    fun GameTestDriver.destroyGuard(player: EntityId) {
        val guard = putCreatureOnBattlefield(player, "Su-Chi Cave Guard")
        val shatter = putCardInHand(player, "Shatter")
        giveMana(player, Color.RED, 1)
        giveColorlessMana(player, 1)
        castSpell(player, shatter, listOf(guard)).error shouldBe null
        bothPass() // Shatter resolves; the dies ability uses the stack, not mana-ability timing.
        stackSize shouldBe 1
        pool(player).total shouldBe 0
        bothPass()
        pool(player).total shouldBe 8
    }
    test("dies trigger waits on the stack and its eight mana survives combat") {
        val d = driver()
        val player = d.activePlayer!!
        d.destroyGuard(player)
        d.giveColorlessMana(player, 3)
        d.passPriorityUntil(Step.POSTCOMBAT_MAIN)
        d.pool(player).total shouldBe 8
        d.pool(player).restrictedMana.all { it.color == null && it.expiry == ManaExpiry.KEPT_UNTIL_END_OF_TURN } shouldBe true
        d.pool(d.getOpponent(player)).total shouldBe 0
        val client = com.wingedsheep.engine.view.ClientStateTransformer(
            d.cardRegistry, predicateEvaluator = com.wingedsheep.engine.handlers.PredicateEvaluator(d.cardRegistry)
        ).transform(d.state, player)
        val visibleMana = client.players.single { it.playerId == player }.manaPool!!
        visibleMana.total shouldBe 8
        visibleMana.restrictedMana.all {
            it.color == null && it.restrictionDescription == "This mana lasts until end of turn."
        } shouldBe true
    }
    test("retained mana casts a spell after combat and the unspent part stays") {
        val d = driver()
        val player = d.activePlayer!!
        d.destroyGuard(player)
        d.passPriorityUntil(Step.POSTCOMBAT_MAIN)
        val ring = d.putCardInHand(player, "Sol Ring")
        d.castSpell(player, ring).error shouldBe null
        d.bothPass()
        d.pool(player).total shouldBe 7
        d.passPriorityUntil(Step.END)
        d.pool(player).total shouldBe 7
    }
    test("mana lasts through the end step and disappears by the next upkeep") {
        val d = driver()
        val player = d.activePlayer!!
        d.destroyGuard(player)
        d.passPriorityUntil(Step.END)
        d.pool(player).total shouldBe 8
        d.passPriorityUntil(Step.UPKEEP)
        d.pool(player).isEmpty shouldBe true
    }
    test("retained mana pays a counterspell tax and the remainder survives combat") {
        val d = driver()
        val player = d.activePlayer!!
        val opponent = d.getOpponent(player)
        d.destroyGuard(player)
        val ring = d.putCardInHand(player, "Sol Ring")
        d.castSpell(player, ring).error shouldBe null
        d.pool(player).total shouldBe 7
        val ringOnStack = d.state.stack.single()
        d.passPriority(player).error shouldBe null
        val snare = d.putCardInHand(opponent, "Geistlight Snare")
        d.giveMana(opponent, Color.BLUE, 3)
        d.castSpellWithTargets(opponent, snare, listOf(ChosenTarget.Spell(ringOnStack))).error shouldBe null
        d.bothPass()
        d.submitYesNo(player, true).error shouldBe null
        d.pool(player).total shouldBe 4
        d.bothPass()
        d.findPermanent(player, "Sol Ring") shouldBe ring
        d.passPriorityUntil(Step.POSTCOMBAT_MAIN)
        d.pool(player).total shouldBe 4
        d.pool(player).restrictedMana.all { it.expiry == ManaExpiry.KEPT_UNTIL_END_OF_TURN } shouldBe true
    }
    test("retained mana pays the foretell special action and the remainder survives combat") {
        val d = driver()
        val player = d.activePlayer!!
        d.destroyGuard(player)
        val effigy = d.putCardInHand(player, "Scorn Effigy")
        d.submit(ForetellCard(player, effigy)).error shouldBe null
        d.getExile(player).contains(effigy) shouldBe true
        d.pool(player).total shouldBe 6
        d.passPriorityUntil(Step.POSTCOMBAT_MAIN)
        d.pool(player).total shouldBe 6
        d.pool(player).restrictedMana.all { it.expiry == ManaExpiry.KEPT_UNTIL_END_OF_TURN } shouldBe true
    }
    test("two deaths accumulate independently") {
        val d = driver()
        val player = d.activePlayer!!
        d.destroyGuard(player)
        val guard = d.putCreatureOnBattlefield(player, "Su-Chi Cave Guard")
        val shatter = d.putCardInHand(player, "Shatter")
        d.giveMana(player, Color.RED, 1)
        d.castSpell(player, shatter, listOf(guard)).error shouldBe null
        d.bothPass()
        d.bothPass()
        d.pool(player).total shouldBe 15 // one retained mana paid Shatter's generic cost
        d.passPriorityUntil(Step.POSTCOMBAT_MAIN)
        d.pool(player).total shouldBe 15
    }
    test("a stolen guard gives its last controller the mana rather than its owner") {
        val d = driver()
        val player = d.activePlayer!!
        val owner = d.getOpponent(player)
        val guard = d.putCreatureOnBattlefield(player, "Su-Chi Cave Guard")
        d.replaceState(d.state.updateEntity(guard) {
            it.with(OwnerComponent(owner)).with(it.get<CardComponent>()!!.copy(ownerId = owner))
        })
        val shatter = d.putCardInHand(player, "Shatter")
        d.giveMana(player, Color.RED, 1)
        d.giveColorlessMana(player, 1)
        d.castSpell(player, shatter, listOf(guard)).error shouldBe null
        d.bothPass()
        d.bothPass()
        d.pool(player).total shouldBe 8
        d.pool(owner).total shouldBe 0
        d.state.getGraveyard(owner).contains(guard) shouldBe true
    }
    test("vigilance allows attacking without tapping") {
        val d = driver()
        val player = d.activePlayer!!
        val guard = d.putCreatureOnBattlefield(player, "Su-Chi Cave Guard")
        d.removeSummoningSickness(guard)
        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(player, listOf(guard), d.getOpponent(player)).error shouldBe null
        d.isTapped(guard) shouldBe false
    }
})
