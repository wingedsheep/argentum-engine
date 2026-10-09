package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.LinkedExileComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.bro.cards.TheTemporalAnchor
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantFlashToSpellType
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.*
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class TheTemporalAnchorScenarioTest : FunSpec({
    val scry = card("Anchor Test Scry") {
        manaCost = "{0}"
        typeLine = "Instant"
        spell { effect = Effects.Scry(2) }
    }
    val remove = card("Anchor Test Removal") {
        manaCost = "{0}"
        typeLine = "Instant"
        spell {
            val artifact = target(TargetFilter.Artifact)
            effect = Effects.Destroy(artifact)
        }
    }
    fun GameTestDriver.removeAnchor(anchor: EntityId) {
        val me = priorityPlayer!!
        castSpell(me, putCardInHand(me, remove.name), listOf(anchor)).outcome shouldBe Outcome.Done
        bothPass()
    }
    val flashCreature = card("Anchor Flash Proof") {
        manaCost = "{1}{G}"
        typeLine = "Creature — Bear"
        power = 2
        toughness = 2
        keywords(Keyword.FLASH)
    }
    val flashGranter = card("Anchor Flash Granter") {
        manaCost = "{0}"
        typeLine = "Enchantment"
        staticAbility { ability = GrantFlashToSpellType(GameObjectFilter.Creature, controllerOnly = true) }
    }
    fun setup() = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(TheTemporalAnchor, scry, remove, flashCreature, flashGranter))
        initMirrorMatch(Deck.of("Island" to 40), startingPlayer = 0)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }
    fun GameTestDriver.chooseBottom(cards: List<EntityId>) {
        val select = pendingDecision as SelectCardsDecision
        submitDecision(select.playerId, CardsSelectedResponse(select.id, cards))
        (pendingDecision as? ReorderLibraryDecision)?.let {
            submitDecision(it.playerId, OrderedResponse(it.id, it.cards))
        }
    }
    fun GameTestDriver.scryBottom(cards: List<EntityId>) {
        val me = priorityPlayer!!
        castSpell(me, putCardInHand(me, scry.name)).outcome shouldBe Outcome.Done
        bothPass()
        chooseBottom(cards)
    }
    fun GameTestDriver.exile(me: EntityId) = state.getZone(ZoneKey(me, Zone.EXILE))
    fun GameTestDriver.canCast(me: EntityId, id: EntityId) =
        legalActions(me).any { it.affordable && (it.action as? CastSpell)?.cardId == id }
    fun GameTestDriver.canPlayLand(me: EntityId, id: EntityId) =
        legalActions(me).any { it.affordable && (it.action as? PlayLand)?.cardId == id }

    test("own upkeep scries two then one batched trigger exiles the chosen bottom cards") {
        val d = setup(); val me = d.activePlayer!!; val opp = d.getOpponent(me)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN, activePlayer = opp)
        val anchor = d.putPermanentOnBattlefield(me, TheTemporalAnchor.name)
        val land = d.putCardOnTopOfLibrary(me, "Forest")
        val spell = d.putCardOnTopOfLibrary(me, "Grizzly Bears")
        d.passPriorityUntil(Step.UPKEEP, activePlayer = me)
        d.stackSize shouldBe 1
        d.bothPass()
        (d.pendingDecision as SelectCardsDecision).options.toSet() shouldBe setOf(land, spell)
        d.chooseBottom(listOf(land, spell))
        d.stackSize shouldBe 1
        d.bothPass()
        d.exile(me).toSet() shouldBe setOf(land, spell)
        d.state.getEntity(anchor)!!.get<LinkedExileComponent>()!!.exiledIds.toSet() shouldBe setOf(land, spell)
        // Sorcery timing still applies during upkeep.
        d.giveMana(me, Color.GREEN, 2)
        d.canCast(me, spell) shouldBe false
        d.canPlayLand(me, land) shouldBe false
        (d.castSpell(me, spell).error != null) shouldBe true
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        // Costs still apply; the mana from upkeep emptied at the step boundary.
        d.canCast(me, spell) shouldBe false
        d.giveMana(me, Color.GREEN, 2)
        d.canCast(me, spell) shouldBe true
        d.canPlayLand(me, land) shouldBe true
        d.playLand(me, land).outcome shouldBe Outcome.Done
        d.castSpell(me, spell).outcome shouldBe Outcome.Done
        d.bothPass()
        d.assertPermanentExists(me, "Grizzly Bears")
    }
    for (granted in listOf(false, true)) {
        test("linked exile honors ${if (granted) "granted" else "printed"} flash during your turn") {
            val d = setup(); val me = d.activePlayer!!
            d.putPermanentOnBattlefield(me, TheTemporalAnchor.name)
            val name = if (granted) "Grizzly Bears" else flashCreature.name
            val creature = d.putCardOnTopOfLibrary(me, name)
            d.scryBottom(listOf(creature)); d.bothPass()
            d.passPriorityUntil(Step.BEGIN_COMBAT)
            d.giveMana(me, Color.GREEN, 2)
            if (granted) {
                d.canCast(me, creature) shouldBe false
                d.putPermanentOnBattlefield(me, flashGranter.name)
            }
            d.canCast(me, creature) shouldBe true
            d.castSpell(me, creature).outcome shouldBe Outcome.Done
            d.bothPass()
            d.assertPermanentExists(me, name)
        }
    }
    test("keeping all cards on top creates no exile trigger") {
        val d = setup(); val me = d.activePlayer!!
        d.putPermanentOnBattlefield(me, TheTemporalAnchor.name)
        d.scryBottom(emptyList())
        d.stackSize shouldBe 0
        d.exile(me).isEmpty() shouldBe true
    }
    test("exile uses the bottom at resolution instead of remembering the scried identities") {
        val d = setup(); val me = d.activePlayer!!
        d.putPermanentOnBattlefield(me, TheTemporalAnchor.name)
        val chosen = d.putCardOnTopOfLibrary(me, "Forest")
        d.scryBottom(listOf(chosen))
        val key = ZoneKey(me, Zone.LIBRARY)
        val before = d.state.getZone(key)
        val nowBottom = before.first()
        // Model a library reorder in response to the trigger.
        d.replaceState(d.state.copy(zones = d.state.zones + (key to (before.drop(1) + nowBottom))))
        d.bothPass()
        d.exile(me) shouldBe listOf(nowBottom)
        d.state.getZone(key).contains(chosen) shouldBe true
    }
    test("scry on the opponent's turn exiles cards but neither spells nor lands may be played then") {
        val d = setup(); val me = d.activePlayer!!; val opp = d.getOpponent(me)
        d.putPermanentOnBattlefield(me, TheTemporalAnchor.name)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN, activePlayer = opp)
        d.passPriority(opp)
        val bolt = d.putCardOnTopOfLibrary(me, "Lightning Bolt")
        val land = d.putCardOnTopOfLibrary(me, "Forest")
        d.scryBottom(listOf(bolt, land)); d.bothPass()
        d.exile(me).toSet() shouldBe setOf(bolt, land)
        if (d.priorityPlayer != me) d.passPriority(d.priorityPlayer!!)
        d.giveMana(me, Color.RED)
        d.canCast(me, bolt) shouldBe false
        d.canPlayLand(me, land) shouldBe false
        (d.castSpell(me, bolt, listOf(opp)).error != null) shouldBe true
    }
    test("removing the Anchor disables permission and its replacement cannot access the old pile") {
        val d = setup(); val me = d.activePlayer!!
        val anchor = d.putPermanentOnBattlefield(me, TheTemporalAnchor.name)
        val spell = d.putCardOnTopOfLibrary(me, "Grizzly Bears")
        d.scryBottom(listOf(spell)); d.bothPass()
        d.giveMana(me, Color.GREEN, 2)
        d.canCast(me, spell) shouldBe true
        d.removeAnchor(anchor)
        d.canCast(me, spell) shouldBe false
        d.putPermanentOnBattlefield(me, TheTemporalAnchor.name)
        d.canCast(me, spell) shouldBe false
        d.exile(me) shouldBe listOf(spell)
    }
    test("source leaving in response does not counter the exile trigger or grant permission") {
        val d = setup(); val me = d.activePlayer!!
        val anchor = d.putPermanentOnBattlefield(me, TheTemporalAnchor.name)
        val land = d.putCardOnTopOfLibrary(me, "Forest")
        d.scryBottom(listOf(land))
        d.removeAnchor(anchor)
        d.bothPass()
        d.exile(me) shouldBe listOf(land)
        d.canPlayLand(me, land) shouldBe false
    }
})
