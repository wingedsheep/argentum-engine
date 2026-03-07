package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.*
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.CardScript
import com.wingedsheep.sdk.model.CreatureStats
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GameEvent
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.costs.PayCost
import com.wingedsheep.sdk.scripting.effects.PayOrSufferEffect
import com.wingedsheep.sdk.scripting.effects.SacrificeSelfEffect
import com.wingedsheep.sdk.scripting.TriggeredAbility
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Tests for PayCost.Exile — "Sacrifice [this] unless you exile [N cards] from [zone]".
 *
 * Cards that use this pattern:
 * - Force of Will ("exile a blue card from your hand")
 * - Snapback ("exile a blue card from your hand")
 * - Delve ("exile cards from your graveyard")
 */
class ExileUnlessExileTest : FunSpec({

    // Test card: creature that must exile a card from graveyard on ETB or be sacrificed
    val GraveFed = CardDefinition(
        name = "Grave Fed",
        manaCost = ManaCost.parse("{1}{B}"),
        typeLine = TypeLine.creature(setOf(Subtype("Zombie"))),
        oracleText = "When Grave Fed enters the battlefield, sacrifice it unless you exile a card from your graveyard.",
        creatureStats = CreatureStats(3, 2),
        script = CardScript.creature(
            TriggeredAbility.create(
                trigger = GameEvent.ZoneChangeEvent(to = Zone.BATTLEFIELD),
                binding = TriggerBinding.SELF,
                effect = PayOrSufferEffect(
                    cost = PayCost.Exile(
                        filter = GameObjectFilter.Any,
                        zone = Zone.GRAVEYARD,
                        count = 1
                    ),
                    suffer = SacrificeSelfEffect
                )
            )
        )
    )

    // Test card: creature that must exile a blue card from hand on ETB or be sacrificed
    val HandExiler = CardDefinition(
        name = "Hand Exiler",
        manaCost = ManaCost.parse("{U}"),
        typeLine = TypeLine.creature(setOf(Subtype("Wizard"))),
        oracleText = "When Hand Exiler enters the battlefield, sacrifice it unless you exile a blue card from your hand.",
        creatureStats = CreatureStats(2, 1),
        script = CardScript.creature(
            TriggeredAbility.create(
                trigger = GameEvent.ZoneChangeEvent(to = Zone.BATTLEFIELD),
                binding = TriggerBinding.SELF,
                effect = PayOrSufferEffect(
                    cost = PayCost.Exile(
                        filter = GameObjectFilter.Any.withColor(Color.BLUE),
                        zone = Zone.HAND,
                        count = 1
                    ),
                    suffer = SacrificeSelfEffect
                )
            )
        )
    )

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(GraveFed)
        driver.registerCard(HandExiler)
        return driver
    }

    test("ETB prompts to exile a card from graveyard when player has one") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Swamp" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put a card in graveyard
        val graveyardCard = driver.putCardInGraveyard(activePlayer, "Swamp")

        // Give the player the creature and mana
        val graveFed = driver.putCardInHand(activePlayer, "Grave Fed")
        driver.giveMana(activePlayer, Color.BLACK, 2)

        // Cast and resolve
        driver.castSpell(activePlayer, graveFed)
        driver.bothPass() // Resolve creature

        // Resolve ETB trigger
        driver.stackSize shouldBe 1
        driver.bothPass()

        // Should have a pending decision to select a card from graveyard
        val decision = driver.pendingDecision
        decision shouldNotBe null
        decision.shouldBeInstanceOf<SelectCardsDecision>()
        (decision as SelectCardsDecision).options shouldContain graveyardCard
    }

    test("creature stays if player exiles a card from graveyard") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Swamp" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put a card in graveyard
        val graveyardCard = driver.putCardInGraveyard(activePlayer, "Swamp")

        // Give the player the creature and mana
        val graveFed = driver.putCardInHand(activePlayer, "Grave Fed")
        driver.giveMana(activePlayer, Color.BLACK, 2)

        // Cast and resolve
        driver.castSpell(activePlayer, graveFed)
        driver.bothPass()
        driver.stackSize shouldBe 1
        driver.bothPass()

        // Select the graveyard card to exile
        driver.submitCardSelection(activePlayer, listOf(graveyardCard))

        // Creature should still be on the battlefield
        driver.findPermanent(activePlayer, "Grave Fed") shouldNotBe null

        // Graveyard card should be in exile
        driver.getExileCardNames(activePlayer) shouldContain "Swamp"
    }

    test("creature is sacrificed if player declines to exile") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Swamp" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put a card in graveyard
        driver.putCardInGraveyard(activePlayer, "Swamp")

        // Give the player the creature and mana
        val graveFed = driver.putCardInHand(activePlayer, "Grave Fed")
        driver.giveMana(activePlayer, Color.BLACK, 2)

        // Cast and resolve
        driver.castSpell(activePlayer, graveFed)
        driver.bothPass()
        driver.stackSize shouldBe 1
        driver.bothPass()

        // Decline to pay (select 0 cards)
        driver.submitCardSelection(activePlayer, emptyList())

        // Creature should be sacrificed
        driver.findPermanent(activePlayer, "Grave Fed") shouldBe null
        driver.getGraveyardCardNames(activePlayer) shouldContain "Grave Fed"
    }

    test("creature auto-sacrificed when graveyard is empty") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Swamp" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // No cards in graveyard — cost can't be paid

        val graveFed = driver.putCardInHand(activePlayer, "Grave Fed")
        driver.giveMana(activePlayer, Color.BLACK, 2)

        // Cast and resolve
        driver.castSpell(activePlayer, graveFed)
        driver.bothPass()
        driver.stackSize shouldBe 1
        driver.bothPass()

        // No decision — auto-sacrifice because graveyard is empty
        driver.pendingDecision shouldBe null
        driver.findPermanent(activePlayer, "Grave Fed") shouldBe null
        driver.getGraveyardCardNames(activePlayer) shouldContain "Grave Fed"
    }

    test("exile from hand — creature stays when player exiles a blue card") {
        val driver = createDriver()
        // Use a second copy of Hand Exiler as the blue card to exile
        driver.initMirrorMatch(
            deck = Deck.of("Island" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put a blue card in hand to exile as cost (Hand Exiler is blue)
        val blueCard = driver.putCardInHand(activePlayer, "Hand Exiler")

        // Give the creature and mana
        val handExiler = driver.putCardInHand(activePlayer, "Hand Exiler")
        driver.giveMana(activePlayer, Color.BLUE, 1)

        // Cast and resolve
        driver.castSpell(activePlayer, handExiler)
        driver.bothPass()
        driver.stackSize shouldBe 1
        driver.bothPass()

        // Select the blue card to exile from hand
        driver.submitCardSelection(activePlayer, listOf(blueCard))

        // Creature should stay
        driver.findPermanent(activePlayer, "Hand Exiler") shouldNotBe null

        // The other Hand Exiler should be in exile
        driver.getExileCardNames(activePlayer) shouldContain "Hand Exiler"
    }
})
