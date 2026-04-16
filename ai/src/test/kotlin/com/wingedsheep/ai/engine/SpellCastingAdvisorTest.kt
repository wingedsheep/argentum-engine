package com.wingedsheep.ai.engine

import com.wingedsheep.ai.engine.advisor.modules.BloomburrowAdvisorModule
import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.mechanics.layers.ActiveFloatingEffect
import com.wingedsheep.engine.mechanics.layers.FloatingEffectData
import com.wingedsheep.engine.mechanics.layers.Layer
import com.wingedsheep.engine.mechanics.layers.SerializableModification
import com.wingedsheep.engine.mechanics.layers.Sublayer
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.mtg.sets.definitions.bloomburrow.BloomburrowSet
import com.wingedsheep.mtg.sets.definitions.portal.PortalSet
import com.wingedsheep.sdk.core.*
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.Duration
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.comparables.shouldBeGreaterThan
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Tests that the AI actually casts removal/bite spells when it has mana and targets,
 * rather than hoarding them forever.
 */
class SpellCastingAdvisorTest : FunSpec({

    val bear = CardDefinition.creature(
        name = "Grizzly Bears",
        manaCost = ManaCost.parse("{1}{G}"),
        subtypes = setOf(Subtype("Bear")),
        power = 2, toughness = 2
    )

    val ogre = CardDefinition.creature(
        name = "Ogre Warrior",
        manaCost = ManaCost.parse("{3}{R}"),
        subtypes = setOf(Subtype("Ogre"), Subtype("Warrior")),
        power = 4, toughness = 4
    )

    val allCards = BloomburrowSet.allCards + BloomburrowSet.basicLands +
        PortalSet.allCards + listOf(bear, ogre)

    fun createRegistryAndDriver(): Pair<CardRegistry, GameTestDriver> {
        val registry = CardRegistry().apply { register(allCards) }
        val driver = GameTestDriver().apply {
            registerCards(allCards)
            initMirrorMatch(
                deck = Deck.of("Forest" to 20, "Mountain" to 20),
                startingLife = 20
            )
        }
        return registry to driver
    }

    fun cardNamesInHand(driver: GameTestDriver, playerId: EntityId): List<String> {
        return driver.getHand(playerId).mapNotNull { entityId ->
            driver.state.getEntity(entityId)?.get<CardComponent>()?.name
        }
    }

    fun runFullTurnCycle(
        driver: GameTestDriver,
        registry: CardRegistry,
        aiPlayerId: EntityId,
        opponentId: EntityId
    ) {
        val ai = AIPlayer.create(registry, aiPlayerId, listOf(BloomburrowAdvisorModule()))
        val opponent = AIPlayer.create(registry, opponentId, listOf(BloomburrowAdvisorModule()))
        val processor = ActionProcessor(registry)

        var state = driver.state
        var safety = 0
        val startTurn = state.turnNumber

        while (state.turnNumber < startTurn + 2 && !state.gameOver && safety < 300) {
            val nextState: GameState? = when (state.priorityPlayerId) {
                aiPlayerId -> ai.playPriorityWindow(state, processor)
                opponentId -> opponent.playPriorityWindow(state, processor)
                else -> {
                    val decision = state.pendingDecision
                    if (decision != null) {
                        val responder = if (decision.playerId == aiPlayerId) ai else opponent
                        val response = responder.respondToDecision(state, decision)
                        val result = processor.process(state, SubmitDecision(decision.playerId, response)).result
                        if (result.error != null) null else result.state
                    } else null
                }
            }
            if (nextState == null) break
            state = nextState
            safety++
        }

        driver.replaceState(state)
    }

    // ═════════════════════════════════════════════════════════════════════
    // Polliwallop
    // ═════════════════════════════════════════════════════════════════════

    test("AI chooses to cast Polliwallop when it has a creature and opponent has a bigger one") {
        val (registry, driver) = createRegistryAndDriver()
        val p1 = driver.player1
        val p2 = driver.player2

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // P1 has a 2/2, P2 has a 4/4 — Polliwallop deals 2×2 = 4, killing the 4/4
        val myBear = driver.putCreatureOnBattlefield(p1, "Grizzly Bears")
        driver.removeSummoningSickness(myBear)
        driver.putCreatureOnBattlefield(p2, "Ogre Warrior")

        driver.putCardInHand(p1, "Polliwallop")
        driver.giveMana(p1, Color.GREEN, 1)
        driver.giveColorlessMana(p1, 3)

        val ai = AIPlayer.create(registry, p1, listOf(BloomburrowAdvisorModule()))
        val action = ai.chooseAction(driver.state)
        (action is CastSpell).shouldBeTrue()
    }

    test("AI casts Polliwallop within a full turn cycle") {
        val (registry, driver) = createRegistryAndDriver()
        val p1 = driver.player1
        val p2 = driver.player2

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val myBear = driver.putCreatureOnBattlefield(p1, "Grizzly Bears")
        driver.removeSummoningSickness(myBear)
        driver.putCreatureOnBattlefield(p2, "Ogre Warrior")

        driver.putCardInHand(p1, "Polliwallop")
        driver.giveMana(p1, Color.GREEN, 1)
        driver.giveColorlessMana(p1, 3)

        runFullTurnCycle(driver, registry, p1, p2)

        cardNamesInHand(driver, p1).contains("Polliwallop") shouldBe false
    }

    // ═════════════════════════════════════════════════════════════════════
    // Dire Downdraft
    // ═════════════════════════════════════════════════════════════════════

    test("AI chooses to cast Dire Downdraft when opponent has a creature") {
        val (registry, driver) = createRegistryAndDriver()
        val p1 = driver.player1
        val p2 = driver.player2

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putCreatureOnBattlefield(p2, "Ogre Warrior")

        driver.putCardInHand(p1, "Dire Downdraft")
        driver.giveMana(p1, Color.BLUE, 1)
        driver.giveColorlessMana(p1, 3)

        val ai = AIPlayer.create(registry, p1, listOf(BloomburrowAdvisorModule()))
        val action = ai.chooseAction(driver.state)
        (action is CastSpell).shouldBeTrue()
    }

    test("AI casts Dire Downdraft within a full turn cycle") {
        val (registry, driver) = createRegistryAndDriver()
        val p1 = driver.player1
        val p2 = driver.player2

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putCreatureOnBattlefield(p2, "Ogre Warrior")

        driver.putCardInHand(p1, "Dire Downdraft")
        driver.giveMana(p1, Color.BLUE, 1)
        driver.giveColorlessMana(p1, 3)

        runFullTurnCycle(driver, registry, p1, p2)

        cardNamesInHand(driver, p1).contains("Dire Downdraft") shouldBe false
    }

    // ═════════════════════════════════════════════════════════════════════
    // Savor — removal targeting: prefer kills over shrinks
    // ═════════════════════════════════════════════════════════════════════

    test("AI targets creature that Savor can kill rather than one it only shrinks") {
        // Savor gives -2/-2 until end of turn.
        // Opponent (P1) has a 2/2 (dies to -2/-2) and a 2/3 (survives as 0/1).
        // AI (P2) should target the 2/2 to actually kill it — shrinking the 2/3
        // is temporary (recovers next turn) and leaves a creature on the board.
        // Set up on P1's turn so P2 (AI) will use removal at instant speed.
        val creature23 = CardDefinition.creature(
            name = "Coral Merfolk",
            manaCost = ManaCost.parse("{1}{U}"),
            subtypes = setOf(Subtype("Merfolk")),
            power = 2, toughness = 3
        )
        val cards = allCards + listOf(creature23)
        val registry = CardRegistry().apply { register(cards) }
        val driver = GameTestDriver().apply {
            registerCards(cards)
            initMirrorMatch(
                deck = Deck.of("Swamp" to 20, "Forest" to 20),
                startingLife = 20
            )
        }
        val p1 = driver.player1
        val p2 = driver.player2

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // P1 (opponent) has both creatures
        driver.putCreatureOnBattlefield(p1, "Grizzly Bears")   // 2/2 — dies to -2/-2
        driver.putCreatureOnBattlefield(p1, "Coral Merfolk")    // 2/3 — survives as 0/1

        // P2 (AI) has Savor in hand + lands to cast it ({1}{B})
        driver.putCardInHand(p2, "Savor")
        driver.putLandOnBattlefield(p2, "Swamp")
        driver.putLandOnBattlefield(p2, "Swamp")

        // Directly test the evaluator: simulate Savor targeting each creature
        // and verify the score for killing the 2/2 is higher.
        val simulator = GameSimulator(registry)
        val evaluator = AIPlayer.defaultEvaluator()

        // Pass priority from P1 to P2 so P2 can cast
        driver.submit(PassPriority(p1))

        // Give P2 mana directly
        driver.giveMana(p2, Color.BLACK, 1)
        driver.giveColorlessMana(p2, 1)

        val savorInHand = driver.getHand(p2).first { entityId ->
            driver.state.getEntity(entityId)?.get<CardComponent>()?.name == "Savor"
        }

        // Find P1's creatures
        val bearTarget = driver.state.projectedState.getBattlefieldControlledBy(p1).first {
            driver.state.getEntity(it)?.get<CardComponent>()?.name == "Grizzly Bears"
        }
        val merfolkTarget = driver.state.projectedState.getBattlefieldControlledBy(p1).first {
            driver.state.getEntity(it)?.get<CardComponent>()?.name == "Coral Merfolk"
        }

        fun scoreTarget(targetId: EntityId): Double {
            val result = simulator.simulate(driver.state,
                CastSpell(p2, savorInHand, listOf(ChosenTarget.Permanent(targetId)),
                    paymentStrategy = PaymentStrategy.FromPool))
            return evaluator.evaluate(result.state, result.state.projectedState, p2)
        }

        val bearScore = scoreTarget(bearTarget)
        val merfolkScore = scoreTarget(merfolkTarget)

        io.kotest.assertions.withClue(
            "Targeting 2/2 (kills it) should score higher than targeting 2/3 (only shrinks). " +
            "Bear score: $bearScore, Merfolk score: $merfolkScore"
        ) {
            bearScore shouldBeGreaterThan merfolkScore
        }
    }

    test("AI prefers destroying base-stat creature over temporarily-boosted one") {
        // Two creatures both at 4/4 right now, but:
        // - One is a base 4/4 (permanently strong)
        // - One is a base 2/2 with +2/+2 until end of turn (temporary — reverts to 2/2)
        // When choosing which to destroy, the AI should kill the base 4/4 since
        // the boosted 2/2 shrinks back on its own.
        val cards = allCards + listOf(ogre)
        val registry = CardRegistry().apply { register(cards) }
        val driver = GameTestDriver().apply {
            registerCards(cards)
            initMirrorMatch(
                deck = Deck.of("Swamp" to 20, "Forest" to 20),
                startingLife = 20
            )
        }
        val p1 = driver.player1
        val p2 = driver.player2

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Opponent has a base 4/4 and a 2/2
        driver.putCreatureOnBattlefield(p2, "Ogre Warrior") // base 4/4
        val boostedBear = driver.putCreatureOnBattlefield(p2, "Grizzly Bears") // base 2/2

        // Give the 2/2 a temporary +2/+2 so it looks like a 4/4 right now
        val floatingEffect = ActiveFloatingEffect(
            id = EntityId.generate(),
            effect = FloatingEffectData(
                layer = Layer.POWER_TOUGHNESS,
                sublayer = Sublayer.MODIFICATIONS,
                modification = SerializableModification.ModifyPowerToughness(2, 2),
                affectedEntities = setOf(boostedBear)
            ),
            duration = Duration.EndOfTurn,
            sourceId = null,
            controllerId = p2,
            timestamp = driver.state.timestamp
        )
        driver.replaceState(driver.state.copy(floatingEffects = driver.state.floatingEffects + floatingEffect))

        // Verify the boost worked: bear should now be projected as 4/4
        val projectedPower = driver.state.projectedState.getPower(boostedBear)
        projectedPower shouldBe 4

        // Give AI Sonar Strike (deals 4 to target creature, costs {3}{W}) + lands
        driver.putCardInHand(p1, "Sonar Strike")
        driver.putLandOnBattlefield(p1, "Plains")
        driver.putLandOnBattlefield(p1, "Plains")
        driver.putLandOnBattlefield(p1, "Plains")
        driver.putLandOnBattlefield(p1, "Plains")

        runFullTurnCycle(driver, registry, p1, p2)

        // Sonar Strike should have been cast
        cardNamesInHand(driver, p1).contains("Sonar Strike") shouldBe false

        // The base 4/4 should be dead — AI should prefer killing the permanently strong creature
        val p2Graveyard = driver.state.getZone(p2, Zone.GRAVEYARD)
        val graveyardNames = p2Graveyard.mapNotNull { entityId ->
            driver.state.getEntity(entityId)?.get<CardComponent>()?.name
        }
        graveyardNames shouldContain "Ogre Warrior"
    }
})
