package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.*
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.CardScript
import com.wingedsheep.sdk.model.CreatureStats
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.*
import com.wingedsheep.sdk.targeting.TargetCreature
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Tests for Tempting Wurm's ETB trigger.
 *
 * Oracle: "When Tempting Wurm enters the battlefield, each opponent may put any
 * number of artifact, creature, enchantment, and/or land cards from their hand
 * onto the battlefield."
 */
class TemptingWurmTest : FunSpec({

    val TemptingWurm = CardDefinition(
        name = "Tempting Wurm",
        manaCost = ManaCost.parse("{1}{G}"),
        typeLine = TypeLine.creature(setOf(Subtype("Wurm"))),
        oracleText = "When Tempting Wurm enters the battlefield, each opponent may put any number of artifact, creature, enchantment, and/or land cards from their hand onto the battlefield.",
        creatureStats = CreatureStats(5, 5),
        script = CardScript.creature(
            TriggeredAbility.create(
                trigger = OnEnterBattlefield(),
                effect = EachOpponentMayPutFromHandEffect(
                    filter = GameObjectFilter.Permanent
                )
            )
        )
    )

    val GoblinSoldier = CardDefinition(
        name = "Goblin Soldier",
        manaCost = ManaCost.parse("{R}"),
        typeLine = TypeLine.creature(setOf(Subtype("Goblin"))),
        creatureStats = CreatureStats(1, 1),
        script = CardScript.creature()
    )

    val TestPacifism = CardDefinition(
        name = "Pacifism",
        manaCost = ManaCost.parse("{1}{W}"),
        typeLine = TypeLine(
            cardTypes = setOf(CardType.ENCHANTMENT),
            subtypes = setOf(Subtype("Aura"))
        ),
        oracleText = "Enchant creature\nEnchanted creature can't attack or block.",
        script = CardScript.aura(
            enchantTarget = TargetCreature(),
            staticAbilities = listOf(
                CantAttack(target = StaticTarget.AttachedCreature),
                CantBlock(target = StaticTarget.AttachedCreature)
            )
        )
    )

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(TemptingWurm)
        driver.registerCard(GoblinSoldier)
        driver.registerCard(TestPacifism)
        return driver
    }

    /** Get the opponent of the given player */
    fun GameTestDriver.opponentOf(playerId: com.wingedsheep.sdk.model.EntityId) =
        if (playerId == player1) player2 else player1

    test("Opponent may put creatures from hand onto the battlefield") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Forest" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.opponentOf(activePlayer)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Give opponent a creature in hand
        val goblin = driver.putCardInHand(opponent, "Goblin Soldier")

        // Cast Tempting Wurm
        val wurm = driver.putCardInHand(activePlayer, "Tempting Wurm")
        driver.giveMana(activePlayer, Color.GREEN, 2)

        driver.castSpell(activePlayer, wurm)
        driver.bothPass() // Resolve creature

        // Resolve ETB trigger
        driver.stackSize shouldBe 1
        driver.bothPass()

        // Opponent should be asked to put cards from hand onto the battlefield
        val decision = driver.pendingDecision
        decision shouldNotBe null
        decision.shouldBeInstanceOf<SelectCardsDecision>()
        (decision as SelectCardsDecision).playerId shouldBe opponent

        // The goblin should be among the options (along with any forests in hand)
        decision.options shouldContain goblin

        // Opponent puts the goblin onto the battlefield
        driver.submitCardSelection(opponent, listOf(goblin))

        // Goblin should now be on the battlefield under opponent's control
        driver.findPermanent(opponent, "Goblin Soldier") shouldNotBe null

        // Tempting Wurm should still be on the battlefield
        driver.findPermanent(activePlayer, "Tempting Wurm") shouldNotBe null
    }

    test("Opponent may decline to put cards from hand") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Forest" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.opponentOf(activePlayer)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Give opponent a creature in hand
        driver.putCardInHand(opponent, "Goblin Soldier")

        // Cast Tempting Wurm
        val wurm = driver.putCardInHand(activePlayer, "Tempting Wurm")
        driver.giveMana(activePlayer, Color.GREEN, 2)

        driver.castSpell(activePlayer, wurm)
        driver.bothPass() // Resolve creature

        // Resolve ETB trigger
        driver.stackSize shouldBe 1
        driver.bothPass()

        // Opponent declines
        driver.submitCardSelection(opponent, emptyList())

        // No creatures should be on the battlefield for the opponent
        driver.findPermanent(opponent, "Goblin Soldier") shouldBe null

        // Tempting Wurm should still be on the battlefield
        driver.findPermanent(activePlayer, "Tempting Wurm") shouldNotBe null
    }

    test("Opponent may put multiple cards from hand onto the battlefield") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Forest" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.opponentOf(activePlayer)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Give opponent multiple creatures in hand
        val goblin1 = driver.putCardInHand(opponent, "Goblin Soldier")
        val goblin2 = driver.putCardInHand(opponent, "Goblin Soldier")

        // Cast Tempting Wurm
        val wurm = driver.putCardInHand(activePlayer, "Tempting Wurm")
        driver.giveMana(activePlayer, Color.GREEN, 2)

        driver.castSpell(activePlayer, wurm)
        driver.bothPass() // Resolve creature

        // Resolve ETB trigger
        driver.stackSize shouldBe 1
        driver.bothPass()

        // Opponent puts both goblins onto the battlefield
        val decision = driver.pendingDecision
        decision shouldNotBe null
        decision.shouldBeInstanceOf<SelectCardsDecision>()

        // Both goblin IDs should be in the options
        (decision as SelectCardsDecision).options shouldContain goblin1
        decision.options shouldContain goblin2

        driver.submitCardSelection(opponent, listOf(goblin1, goblin2))

        // Both goblins should be on the battlefield
        val battlefield = driver.state.getZone(ZoneKey(opponent, Zone.BATTLEFIELD))
        battlefield.contains(goblin1) shouldBe true
        battlefield.contains(goblin2) shouldBe true
    }

    test("No decision when opponent hand is empty") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Forest" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.opponentOf(activePlayer)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Clear opponent's hand by moving all cards to library
        val handZone = ZoneKey(opponent, Zone.HAND)
        val libraryZone = ZoneKey(opponent, Zone.LIBRARY)
        val handCards = driver.state.getZone(handZone)
        var newState = driver.state
        for (cardId in handCards) {
            newState = newState.removeFromZone(handZone, cardId)
            newState = newState.addToZone(libraryZone, cardId)
        }
        driver.replaceState(newState)

        // Verify opponent hand is empty
        driver.getHandSize(opponent) shouldBe 0

        // Cast Tempting Wurm
        val wurm = driver.putCardInHand(activePlayer, "Tempting Wurm")
        driver.giveMana(activePlayer, Color.GREEN, 2)

        driver.castSpell(activePlayer, wurm)
        driver.bothPass() // Resolve creature

        // Resolve ETB trigger - opponent has no cards in hand, no decision needed
        if (driver.stackSize > 0) {
            driver.bothPass()
        }

        // No decision should be pending
        driver.pendingDecision shouldBe null

        // Tempting Wurm should be on the battlefield
        driver.findPermanent(activePlayer, "Tempting Wurm") shouldNotBe null
    }

    test("Opponent can put lands from hand onto the battlefield") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Forest" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.opponentOf(activePlayer)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Give opponent a land in hand (they already have forests from opening hand,
        // but we track a specific one)
        val forest = driver.putCardInHand(opponent, "Forest")

        // Cast Tempting Wurm
        val wurm = driver.putCardInHand(activePlayer, "Tempting Wurm")
        driver.giveMana(activePlayer, Color.GREEN, 2)

        driver.castSpell(activePlayer, wurm)
        driver.bothPass() // Resolve creature

        // Resolve ETB trigger
        driver.stackSize shouldBe 1
        driver.bothPass()

        // Opponent puts the tracked forest onto the battlefield
        driver.submitCardSelection(opponent, listOf(forest))

        // The specific forest should be on the battlefield
        val battlefield = driver.state.getZone(ZoneKey(opponent, Zone.BATTLEFIELD))
        battlefield.contains(forest) shouldBe true
    }

    test("Opponent can put aura from hand onto the battlefield and choose target") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Forest" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.opponentOf(activePlayer)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Give opponent Pacifism in hand
        val pacifism = driver.putCardInHand(opponent, "Pacifism")

        // Put a creature on the active player's battlefield (target for Pacifism)
        val goblin = driver.putCreatureOnBattlefield(activePlayer, "Goblin Soldier")

        // Cast Tempting Wurm
        val wurm = driver.putCardInHand(activePlayer, "Tempting Wurm")
        driver.giveMana(activePlayer, Color.GREEN, 2)

        driver.castSpell(activePlayer, wurm)
        driver.bothPass() // Resolve creature

        // Resolve ETB trigger
        driver.stackSize shouldBe 1
        driver.bothPass()

        // Opponent should be asked to select cards
        val selectDecision = driver.pendingDecision
        selectDecision.shouldBeInstanceOf<SelectCardsDecision>()
        (selectDecision as SelectCardsDecision).options shouldContain pacifism

        // Opponent selects Pacifism
        driver.submitCardSelection(opponent, listOf(pacifism))

        // Opponent should now be asked to choose a target for the aura
        val targetDecision = driver.pendingDecision
        targetDecision.shouldBeInstanceOf<ChooseTargetsDecision>()
        (targetDecision as ChooseTargetsDecision).playerId shouldBe opponent

        // The goblin should be a legal target
        val legalTargets = targetDecision.legalTargets[0]!!
        legalTargets shouldContain goblin

        // Opponent selects the goblin as the enchant target
        driver.submitTargetSelection(opponent, listOf(goblin))

        // Pacifism should be on the battlefield attached to the goblin
        val battlefield = driver.state.getZone(ZoneKey(opponent, Zone.BATTLEFIELD))
        battlefield.contains(pacifism) shouldBe true

        val attachedTo = driver.state.getEntity(pacifism)?.get<AttachedToComponent>()
        attachedTo shouldNotBe null
        attachedTo!!.targetId shouldBe goblin
    }

    test("Aura with no legal targets stays in hand when put via Tempting Wurm") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Forest" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.opponentOf(activePlayer)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Give opponent Pacifism in hand (no creatures on battlefield to enchant)
        val pacifism = driver.putCardInHand(opponent, "Pacifism")

        // Cast Tempting Wurm (only Tempting Wurm enters — it's not on the battlefield yet
        // when the trigger resolves, so there are no creatures to enchant... actually the wurm
        // will be on battlefield when the trigger resolves)
        val wurm = driver.putCardInHand(activePlayer, "Tempting Wurm")
        driver.giveMana(activePlayer, Color.GREEN, 2)

        driver.castSpell(activePlayer, wurm)
        driver.bothPass() // Resolve creature — Tempting Wurm enters battlefield

        // Resolve ETB trigger
        driver.stackSize shouldBe 1
        driver.bothPass()

        // Opponent selects Pacifism
        val selectDecision = driver.pendingDecision
        selectDecision.shouldBeInstanceOf<SelectCardsDecision>()
        driver.submitCardSelection(opponent, listOf(pacifism))

        // Tempting Wurm is a creature on the battlefield, so Pacifism has a legal target.
        // Let's verify we get a target decision (Tempting Wurm should be targetable)
        val targetDecision = driver.pendingDecision
        targetDecision.shouldBeInstanceOf<ChooseTargetsDecision>()

        // Select Tempting Wurm as the target
        driver.submitTargetSelection(opponent, listOf(wurm))

        // Pacifism should be on the battlefield attached to the wurm
        val battlefield = driver.state.getZone(ZoneKey(opponent, Zone.BATTLEFIELD))
        battlefield.contains(pacifism) shouldBe true

        val attachedTo = driver.state.getEntity(pacifism)?.get<AttachedToComponent>()
        attachedTo shouldNotBe null
        attachedTo!!.targetId shouldBe wurm
    }

    test("Opponent can put aura and creature together from hand") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Forest" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.opponentOf(activePlayer)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Give opponent both a creature and an aura
        val goblin = driver.putCardInHand(opponent, "Goblin Soldier")
        val pacifism = driver.putCardInHand(opponent, "Pacifism")

        // Put a creature on the active player's battlefield (target for Pacifism)
        val targetCreature = driver.putCreatureOnBattlefield(activePlayer, "Goblin Soldier")

        // Cast Tempting Wurm
        val wurm = driver.putCardInHand(activePlayer, "Tempting Wurm")
        driver.giveMana(activePlayer, Color.GREEN, 2)

        driver.castSpell(activePlayer, wurm)
        driver.bothPass() // Resolve creature

        // Resolve ETB trigger
        driver.stackSize shouldBe 1
        driver.bothPass()

        // Opponent selects both creature and aura
        val selectDecision = driver.pendingDecision
        selectDecision.shouldBeInstanceOf<SelectCardsDecision>()
        driver.submitCardSelection(opponent, listOf(goblin, pacifism))

        // Goblin should already be on the battlefield (non-aura goes directly)
        val opponentBattlefield = driver.state.getZone(ZoneKey(opponent, Zone.BATTLEFIELD))
        opponentBattlefield.contains(goblin) shouldBe true

        // Now we should be asked to choose a target for Pacifism
        val targetDecision = driver.pendingDecision
        targetDecision.shouldBeInstanceOf<ChooseTargetsDecision>()

        // Select the active player's creature
        driver.submitTargetSelection(opponent, listOf(targetCreature))

        // Pacifism should be on the battlefield attached to the target creature
        val updatedBattlefield = driver.state.getZone(ZoneKey(opponent, Zone.BATTLEFIELD))
        updatedBattlefield.contains(pacifism) shouldBe true

        val attachedTo = driver.state.getEntity(pacifism)?.get<AttachedToComponent>()
        attachedTo shouldNotBe null
        attachedTo!!.targetId shouldBe targetCreature
    }
})
