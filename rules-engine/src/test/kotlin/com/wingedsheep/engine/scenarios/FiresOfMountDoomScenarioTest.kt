package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.TargetsResponse
import com.wingedsheep.engine.state.ComponentContainer
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.battlefield.AttachmentsComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.OwnerComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.ktk.cards.HeartPiercerBow
import com.wingedsheep.mtg.sets.definitions.ltr.cards.FiresOfMountDoom
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Fires of Mount Doom {2}{R} — Legendary Enchantment
 *
 * ETB: 2 damage to target creature an opponent controls, then destroy all Equipment
 * attached to that creature.
 * {2}{R}: Exile the top card of your library; you may play that card this turn. When you
 * play a card this way, Fires of Mount Doom deals 2 damage to each player.
 */
class FiresOfMountDoomScenarioTest : FunSpec({

    val tester = CardDefinition.creature(
        name = "Test Brute",
        manaCost = ManaCost.parse("{1}{G}"),
        subtypes = setOf(Subtype("Beast")),
        power = 3,
        toughness = 3,
        oracleText = ""
    )

    // The card we impulse-exile and replay: a simple creature castable for {R}.
    val burnFodder = CardDefinition.creature(
        name = "Test Fodder",
        manaCost = ManaCost.parse("{R}"),
        subtypes = setOf(Subtype("Goblin")),
        power = 1,
        toughness = 1,
        oracleText = ""
    )

    fun GameTestDriver.attachEquipment(
        playerId: EntityId,
        cardDef: CardDefinition,
        targetCreatureId: EntityId
    ): EntityId {
        val equipmentId = EntityId.generate()
        val cardComponent = CardComponent(
            cardDefinitionId = cardDef.name,
            name = cardDef.name,
            manaCost = cardDef.manaCost,
            typeLine = cardDef.typeLine,
            oracleText = cardDef.oracleText,
            baseStats = cardDef.creatureStats,
            baseKeywords = cardDef.keywords,
            baseFlags = cardDef.flags,
            colors = cardDef.colors,
            ownerId = playerId,
            spellEffect = cardDef.spellEffect
        )
        val container = ComponentContainer.of(
            cardComponent,
            OwnerComponent(playerId),
            ControllerComponent(playerId),
            AttachedToComponent(targetCreatureId)
        )
        var newState = state.withEntity(equipmentId, container)
        newState = newState.addToZone(ZoneKey(playerId, Zone.BATTLEFIELD), equipmentId)
        val existing = newState.getEntity(targetCreatureId)
            ?.get<AttachmentsComponent>()?.attachedIds ?: emptyList()
        newState = newState.updateEntity(targetCreatureId) { c ->
            c.with(AttachmentsComponent(existing + equipmentId))
        }
        replaceState(newState)
        return equipmentId
    }

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(FiresOfMountDoom, HeartPiercerBow, tester, burnFodder))
        return driver
    }

    test("ETB deals 2 to target opponent creature and destroys Equipment attached to it") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 30), startingLife = 20)

        val p1 = driver.activePlayer!!
        val p2 = driver.getOpponent(p1)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Opponent has a 3/3 with an Equipment attached.
        val brute = driver.putCreatureOnBattlefield(p2, "Test Brute")
        val equipment = driver.attachEquipment(p2, HeartPiercerBow, brute)
        driver.state.getEntity(equipment)?.get<AttachedToComponent>() shouldNotBe null

        // Cast Fires of Mount Doom.
        val fires = driver.putCardInHand(p1, "Fires of Mount Doom")
        driver.giveMana(p1, Color.RED, 1)
        driver.giveColorlessMana(p1, 2)
        driver.castSpell(p1, fires)

        // Resolve the enchantment spell, then the ETB trigger goes on the stack and asks for a target.
        driver.passPriority(p1)
        driver.passPriority(p2)

        val targetDecision = driver.state.pendingDecision as? ChooseTargetsDecision
            ?: error("expected ChooseTargetsDecision for the ETB trigger; got ${driver.state.pendingDecision}")
        driver.submitDecision(p1, TargetsResponse(targetDecision.id, mapOf(0 to listOf(brute))))

        // Resolve the ETB triggered ability.
        driver.passPriority(p1)
        driver.passPriority(p2)

        // 2 damage marked on the 3/3, and the Equipment is destroyed.
        driver.getPermanents(p2).contains(equipment) shouldBe false
        driver.getPermanents(p2).contains(brute) shouldBe true
    }

    test("activated ability impulse-exiles the top card and the rider deals 2 to each player when it's played") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 30), startingLife = 20)

        val p1 = driver.activePlayer!!
        val p2 = driver.getOpponent(p1)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Fires already on the battlefield (skip the ETB for this test).
        val fires = driver.putPermanentOnBattlefield(p1, "Fires of Mount Doom")

        // Known top card we can replay.
        driver.putCardOnTopOfLibrary(p1, "Test Fodder")

        // Activate {2}{R}: exile the top card.
        val abilityId = driver.cardRegistry.requireCard("Fires of Mount Doom").activatedAbilities[0].id
        driver.giveMana(p1, Color.RED, 1)
        driver.giveColorlessMana(p1, 2)
        driver.submit(ActivateAbility(playerId = p1, sourceId = fires, abilityId = abilityId)).error shouldBe null

        // Resolve the activated ability — exiles 1 card, grants may-play.
        driver.passPriority(p1)
        driver.passPriority(p2)

        driver.getExileCardNames(p1) shouldBe listOf("Test Fodder")
        // A may-play permission exists for the exiled card, carrying the rider link.
        val exiled = driver.getExile(p1).first()
        driver.state.mayPlayPermissions.any { exiled in it.cardIds && it.riderLinkId != null } shouldBe true

        val p1LifeBefore = driver.getLifeTotal(p1)
        val p2LifeBefore = driver.getLifeTotal(p2)

        // Play the exiled card this turn (cast Test Fodder for {R} from exile).
        driver.giveMana(p1, Color.RED, 1)
        driver.castSpell(p1, exiled).error shouldBe null

        // The rider ("deals 2 damage to each player") is a triggered ability on the stack; resolve it,
        // then resolve the creature spell.
        driver.passPriority(p1)
        driver.passPriority(p2)
        driver.passPriority(p1)
        driver.passPriority(p2)

        // Both players took 2 from the rider.
        driver.getLifeTotal(p1) shouldBe p1LifeBefore - 2
        driver.getLifeTotal(p2) shouldBe p2LifeBefore - 2
        // The exiled card was actually played (now a creature on the battlefield).
        driver.getExileCardNames(p1).contains("Test Fodder") shouldBe false
    }
})
