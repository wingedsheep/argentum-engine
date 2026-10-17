package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.GameEvent
import com.wingedsheep.engine.core.ZoneChangeEvent
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.lea.cards.ConsecrateLand
import com.wingedsheep.mtg.sets.definitions.pip.cards.PrestonGarveyMinuteman
import com.wingedsheep.mtg.sets.tokens.PredefinedTokens
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Preston Garvey, Minuteman {2}{R}{G}{W} — Legendary Creature — Human Soldier 4/4
 *   At the beginning of combat on your turn, create a green Aura enchantment token named Settlement
 *   attached to up to one target land you control. It has enchant land and "Enchanted land has
 *   '{T}: Add one mana of any color.'"
 *   Whenever Preston Garvey attacks, untap each enchanted permanent you control.
 *
 * Pins the Settlement token (green, a non-Role Aura on the chosen land, granting the mana ability),
 * CR 303.4i (no target, or a land the token can't legally enchant, creates no token), that a second
 * Settlement doesn't displace the first (Role replacement is Roles only), and the attack untap.
 */
class PrestonGarveyMinutemanScenarioTest : FunSpec({

    class Table(val d: GameTestDriver, val me: EntityId, val opp: EntityId, val preston: EntityId)

    fun table(): Table {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + PrestonGarveyMinuteman + ConsecrateLand + PredefinedTokens.Settlement)
        d.initMirrorMatch(deck = Deck.of("Forest" to 40))
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val me = d.activePlayer!!
        val preston = d.putCreatureOnBattlefield(me, "Preston Garvey, Minuteman").also { d.removeSummoningSickness(it) }
        return Table(d, me, d.getOpponent(me), preston)
    }

    fun Table.settlements(): List<EntityId> =
        d.state.getBattlefield().filter { d.getCardName(it) == "Settlement" }

    fun Table.hostOf(aura: EntityId): EntityId? = d.state.getEntity(aura)?.get<AttachedToComponent>()?.targetId

    /**
     * Advance to beginning of combat, answer the Settlement trigger's target prompt and resolve it;
     * returns the events the resolution emitted.
     */
    fun Table.beginCombat(targets: List<EntityId>): List<GameEvent> {
        d.passPriorityUntil(Step.BEGIN_COMBAT)
        if (d.pendingDecision != null) d.submitTargetSelection(me, targets).error shouldBe null
        val events = mutableListOf<GameEvent>()
        var guard = 0
        while (guard++ < 5 && d.stackSize > 0) events += d.bothPass().events
        return events
    }

    fun List<GameEvent>.settlementEntered(): Boolean =
        any { it is ZoneChangeEvent && it.entityName == "Settlement" && it.toZone == Zone.BATTLEFIELD }

    test("creates a green Settlement Aura attached to the chosen land, granting a tap-for-any-color ability") {
        val t = table()
        val land = t.d.putLandOnBattlefield(t.me, "Forest")
        val abilitiesBefore = t.d.legalActions(t.me).count { (it.action as? ActivateAbility)?.sourceId == land }
        t.beginCombat(listOf(land)).settlementEntered() shouldBe true

        val settlement = t.settlements().single()
        t.hostOf(settlement) shouldBe land
        val card = t.d.state.getEntity(settlement)!!.get<CardComponent>().shouldNotBeNull()
        withClue("Settlement is green and an Aura, not a Role") {
            card.colors shouldBe setOf(Color.GREEN)
            card.typeLine.isAura shouldBe true
            card.typeLine.isRole shouldBe false
        }
        withClue("the enchanted land gains \"{T}: Add one mana of any color.\"") {
            t.d.legalActions(t.me).count { (it.action as? ActivateAbility)?.sourceId == land } shouldBe abilitiesBefore + 1
        }
    }

    test("no target chosen: no Settlement is created (CR 303.4i)") {
        val t = table()
        t.d.putLandOnBattlefield(t.me, "Forest")
        val events = t.beginCombat(emptyList())
        events.settlementEntered() shouldBe false
        t.settlements() shouldHaveSize 0
    }

    test("a land that can't be enchanted by other Auras is a legal target, but no Settlement is created") {
        val t = table()
        val land = t.d.putLandOnBattlefield(t.me, "Forest")
        val consecrate = t.d.putCardInHand(t.me, "Consecrate Land")
        t.d.giveMana(t.me, Color.WHITE, 1)
        t.d.castSpell(t.me, consecrate, targets = listOf(land)).error shouldBe null
        t.d.bothPass()
        t.hostOf(consecrate) shouldBe land

        val events = t.beginCombat(listOf(land))
        withClue("the token would be attached to a land it can't legally enchant, so it is never created") {
            events.settlementEntered() shouldBe false
            t.settlements() shouldHaveSize 0
        }
        t.hostOf(consecrate) shouldBe land
    }

    test("a second Settlement on the same land doesn't replace the first — only Roles do that") {
        val t = table()
        val land = t.d.putLandOnBattlefield(t.me, "Forest")
        t.beginCombat(listOf(land))
        t.d.passPriorityUntil(Step.BEGIN_COMBAT, activePlayer = t.opp)
        t.d.passPriorityUntil(Step.BEGIN_COMBAT, activePlayer = t.me)
        if (t.d.pendingDecision != null) t.d.submitTargetSelection(t.me, listOf(land)).error shouldBe null
        var guard = 0
        while (guard++ < 5 && t.d.stackSize > 0) t.d.bothPass()

        val settlements = t.settlements()
        settlements shouldHaveSize 2
        settlements.forEach { t.hostOf(it) shouldBe land }
    }

    test("attacking untaps each enchanted permanent you control, and nothing else") {
        val t = table()
        val enchantedLand = t.d.putLandOnBattlefield(t.me, "Forest")
        val plainLand = t.d.putLandOnBattlefield(t.me, "Forest")
        t.beginCombat(listOf(enchantedLand))
        t.d.tapPermanent(enchantedLand)
        t.d.tapPermanent(plainLand)

        t.d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        t.d.declareAttackers(t.me, mapOf(t.preston to t.opp)).error shouldBe null
        var guard = 0
        while (guard++ < 5 && t.d.stackSize > 0) t.d.bothPass()

        withClue("the Settlement-enchanted land untaps") { t.d.isTapped(enchantedLand) shouldBe false }
        withClue("an unenchanted land stays tapped") { t.d.isTapped(plainLand) shouldBe true }
    }
})
