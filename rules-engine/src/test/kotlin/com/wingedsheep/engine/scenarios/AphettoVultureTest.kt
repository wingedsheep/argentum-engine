package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.*
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.CardScript
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.*
import com.wingedsheep.sdk.scripting.effects.MoveToZoneEffect
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Tests for Aphetto Vulture:
 * {4}{B}{B} Creature — Zombie Bird 3/2
 * Flying
 * When Aphetto Vulture dies, you may put target Zombie card from your graveyard
 * on top of your library.
 */
class AphettoVultureTest : FunSpec({

    val AphettoVulture = CardDefinition.creature(
        name = "Aphetto Vulture",
        manaCost = ManaCost.parse("{4}{B}{B}"),
        subtypes = setOf(Subtype("Zombie"), Subtype("Bird")),
        power = 3,
        toughness = 2,
        oracleText = "Flying\nWhen Aphetto Vulture dies, you may put target Zombie card from your graveyard on top of your library.",
        keywords = setOf(Keyword.FLYING),
        script = CardScript.creature(
            TriggeredAbility.create(
                trigger = GameEvent.ZoneChangeEvent(from = Zone.BATTLEFIELD, to = Zone.GRAVEYARD),
                binding = TriggerBinding.SELF,
                effect = MoveToZoneEffect(
                    target = EffectTarget.ContextTarget(0),
                    destination = Zone.LIBRARY,
                    placement = ZonePlacement.Top
                ),
                optional = true,
                targetRequirement = TargetObject(
                    filter = TargetFilter(
                        GameObjectFilter.Any.withSubtype("Zombie").ownedByYou(),
                        zone = Zone.GRAVEYARD
                    )
                )
            )
        )
    )

    // Also need a Zombie test card for graveyard targeting (Festering Goblin is a Zombie)
    val FesteringGoblin = CardDefinition.creature(
        name = "Festering Goblin",
        manaCost = ManaCost.parse("{B}"),
        subtypes = setOf(Subtype("Zombie"), Subtype("Goblin")),
        power = 1,
        toughness = 1,
        oracleText = "When Festering Goblin dies, target creature gets -1/-1 until end of turn."
    )

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(AphettoVulture, FesteringGoblin))
        return driver
    }

    test("Aphetto Vulture dies - triggers target selection for Zombie in graveyard") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Swamp" to 20, "Mountain" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!

        // Advance to main phase
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put Aphetto Vulture on the battlefield
        val vulture = driver.putCreatureOnBattlefield(activePlayer, "Aphetto Vulture")
        vulture shouldNotBe null

        // Put a Zombie creature in the graveyard as a valid target
        val zombieInGraveyard = driver.putCardInGraveyard(activePlayer, "Festering Goblin")
        driver.getGraveyardCardNames(activePlayer) shouldContain "Festering Goblin"

        // Kill the vulture with Lightning Bolt (3 damage kills 3/2)
        driver.giveMana(activePlayer, Color.RED, 1)
        val bolt = driver.putCardInHand(activePlayer, "Lightning Bolt")
        val castResult = driver.castSpellWithTargets(activePlayer, bolt, listOf(ChosenTarget.Permanent(vulture)))
        castResult.isSuccess shouldBe true

        // Resolve the bolt - vulture takes 3 damage and dies
        driver.bothPass()

        // Vulture should be dead
        driver.findPermanent(activePlayer, "Aphetto Vulture") shouldBe null

        // Death trigger should fire - should have a target selection decision
        driver.isPaused shouldBe true
        driver.pendingDecision.shouldNotBeNull()
        driver.pendingDecision.shouldBeInstanceOf<ChooseTargetsDecision>()

        val targetDecision = driver.pendingDecision as ChooseTargetsDecision
        targetDecision.playerId shouldBe activePlayer

        // Legal targets should include the Zombie in graveyard
        val legalTargets = targetDecision.legalTargets[0] ?: emptyList()
        legalTargets shouldContain zombieInGraveyard
    }

    test("Aphetto Vulture can target itself when it dies") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Swamp" to 20, "Mountain" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!

        // Advance to main phase
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put Aphetto Vulture on the battlefield (it's a Zombie itself)
        val vulture = driver.putCreatureOnBattlefield(activePlayer, "Aphetto Vulture")

        // Kill the vulture
        driver.giveMana(activePlayer, Color.RED, 1)
        val bolt = driver.putCardInHand(activePlayer, "Lightning Bolt")
        val castResult = driver.castSpellWithTargets(activePlayer, bolt, listOf(ChosenTarget.Permanent(vulture)))
        castResult.isSuccess shouldBe true

        // Resolve bolt - vulture dies
        driver.bothPass()

        // Death trigger should fire
        driver.isPaused shouldBe true
        driver.pendingDecision.shouldBeInstanceOf<ChooseTargetsDecision>()

        val targetDecision = driver.pendingDecision as ChooseTargetsDecision

        // Aphetto Vulture itself should be a legal target (it's a Zombie in your graveyard)
        val legalTargets = targetDecision.legalTargets[0] ?: emptyList()
        legalTargets shouldContain vulture
    }

    test("Aphetto Vulture dies - resolving puts Zombie on top of library") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Swamp" to 20, "Mountain" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!

        // Advance to main phase
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put Aphetto Vulture on the battlefield
        val vulture = driver.putCreatureOnBattlefield(activePlayer, "Aphetto Vulture")

        // Put a Zombie in the graveyard
        val zombieInGraveyard = driver.putCardInGraveyard(activePlayer, "Festering Goblin")

        // Kill the vulture
        driver.giveMana(activePlayer, Color.RED, 1)
        val bolt = driver.putCardInHand(activePlayer, "Lightning Bolt")
        driver.castSpellWithTargets(activePlayer, bolt, listOf(ChosenTarget.Permanent(vulture)))

        // Resolve bolt
        driver.bothPass()

        // Select the zombie as target
        driver.pendingDecision.shouldBeInstanceOf<ChooseTargetsDecision>()
        driver.submitTargetSelection(activePlayer, listOf(zombieInGraveyard))

        // Resolve the trigger
        driver.bothPass()

        // The zombie should no longer be in the graveyard
        driver.getGraveyardCardNames(activePlayer) shouldNotContain "Festering Goblin"

        // The zombie should be on top of the library
        val library = driver.state.getLibrary(activePlayer)
        library.size shouldBeGreaterThan 0
        library.first() shouldBe zombieInGraveyard
    }

    test("Aphetto Vulture dies with only non-Zombies in graveyard - still targets itself") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Swamp" to 20, "Mountain" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!

        // Advance to main phase
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put Aphetto Vulture on the battlefield
        val vulture = driver.putCreatureOnBattlefield(activePlayer, "Aphetto Vulture")

        // Put a non-Zombie creature in the graveyard
        driver.putCardInGraveyard(activePlayer, "Grizzly Bears")

        // Kill the vulture
        driver.giveMana(activePlayer, Color.RED, 1)
        val bolt = driver.putCardInHand(activePlayer, "Lightning Bolt")
        driver.castSpellWithTargets(activePlayer, bolt, listOf(ChosenTarget.Permanent(vulture)))

        // Resolve bolt - vulture dies
        driver.bothPass()

        // Vulture itself is a Zombie, so it's now in the graveyard and is a valid target.
        // The trigger should still fire because Aphetto Vulture itself is a Zombie in the graveyard.
        driver.isPaused shouldBe true
        driver.pendingDecision.shouldBeInstanceOf<ChooseTargetsDecision>()

        val targetDecision = driver.pendingDecision as ChooseTargetsDecision
        val legalTargets = targetDecision.legalTargets[0] ?: emptyList()

        // Grizzly Bears should NOT be a legal target (not a Zombie)
        val bearsId = driver.getGraveyard(activePlayer).first { id ->
            driver.getCardName(id) == "Grizzly Bears"
        }
        legalTargets shouldNotContain bearsId

        // But Aphetto Vulture itself should be a legal target
        legalTargets shouldContain vulture
    }

    test("Aphetto Vulture does not target Zombies in opponent's graveyard") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Swamp" to 20, "Mountain" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)

        // Advance to main phase
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put Aphetto Vulture on the battlefield
        val vulture = driver.putCreatureOnBattlefield(activePlayer, "Aphetto Vulture")

        // Put a Zombie in OPPONENT's graveyard (not ours)
        val opponentZombie = driver.putCardInGraveyard(opponent, "Festering Goblin")

        // Kill the vulture
        driver.giveMana(activePlayer, Color.RED, 1)
        val bolt = driver.putCardInHand(activePlayer, "Lightning Bolt")
        driver.castSpellWithTargets(activePlayer, bolt, listOf(ChosenTarget.Permanent(vulture)))

        // Resolve bolt
        driver.bothPass()

        // Trigger should fire (Aphetto Vulture itself is a Zombie in our graveyard)
        driver.isPaused shouldBe true
        driver.pendingDecision.shouldBeInstanceOf<ChooseTargetsDecision>()

        val targetDecision = driver.pendingDecision as ChooseTargetsDecision
        val legalTargets = targetDecision.legalTargets[0] ?: emptyList()

        // Opponent's Zombie should NOT be a legal target
        legalTargets shouldNotContain opponentZombie
    }
})
