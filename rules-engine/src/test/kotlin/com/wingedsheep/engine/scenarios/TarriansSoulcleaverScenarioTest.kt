package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.battlefield.AttachmentsComponent
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.lci.cards.SynapseNecromage
import com.wingedsheep.mtg.sets.definitions.lci.cards.TarriansSoulcleaver
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Tarrian's Soulcleaver (LCI #264) — {1} Legendary Artifact — Equipment.
 *
 * Equipped creature has vigilance.
 * Whenever another artifact or creature is put into a graveyard from the battlefield,
 * put a +1/+1 counter on equipped creature.
 * Equip {2}
 *
 * Tests:
 *  1. Equipped creature has vigilance (static GrantKeyword).
 *  2. Another creature dying (any controller) puts a +1/+1 counter on the equipped creature.
 *  3. An artifact dying puts a +1/+1 counter on the equipped creature (artifact branch of the OR).
 *  4. When the Soulcleaver is unattached, the death trigger resolves as a harmless no-op.
 *  5. A TOKEN dying puts a +1/+1 counter on the equipped creature — regression for the
 *     TriggerMatcher LKI gap: the union filter collapses to CardPredicate.Or, and the token
 *     entity is swept by CR 704.5d before trigger matching, so the composite must be evaluated
 *     against the event's last-known type line, not the live (gone) CardComponent.
 */
class TarriansSoulcleaverScenarioTest : FunSpec({

    val projector = StateProjector()

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(TarriansSoulcleaver, SynapseNecromage))
        return driver
    }

    fun plusOneCounters(driver: GameTestDriver, entityId: EntityId): Int =
        driver.state.getEntity(entityId)?.get<CountersComponent>()
            ?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    fun GameTestDriver.putEquipmentAttached(
        playerId: EntityId,
        cardName: String,
        targetCreatureId: EntityId,
    ): EntityId {
        val equipmentId = putPermanentOnBattlefield(playerId, cardName)
        var newState = state.updateEntity(equipmentId) { c -> c.with(AttachedToComponent(targetCreatureId)) }
        val existing = newState.getEntity(targetCreatureId)
            ?.get<AttachmentsComponent>()?.attachedIds ?: emptyList()
        newState = newState.updateEntity(targetCreatureId) { c ->
            c.with(AttachmentsComponent(existing + equipmentId))
        }
        replaceState(newState)
        return equipmentId
    }

    // Player 1 may not be active at game start (random turn order) — advance until it is.
    fun GameTestDriver.advanceToPlayer1Main() {
        passPriorityUntil(Step.PRECOMBAT_MAIN)
        var safety = 0
        while (activePlayer != player1 && safety < 50) {
            bothPass()
            passPriorityUntil(Step.PRECOMBAT_MAIN)
            safety++
        }
    }

    test("equipped creature has vigilance") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), startingLife = 20)

        val bear = driver.putCreatureOnBattlefield(driver.player1, "Grizzly Bears")
        driver.putEquipmentAttached(driver.player1, "Tarrian's Soulcleaver", bear)

        val projected = projector.project(driver.state)
        projected.hasKeyword(bear, Keyword.VIGILANCE) shouldBe true
    }

    test("another creature dying (any controller) puts a +1/+1 counter on the equipped creature") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), startingLife = 20)

        val bear = driver.putCreatureOnBattlefield(driver.player1, "Grizzly Bears")
        driver.putEquipmentAttached(driver.player1, "Tarrian's Soulcleaver", bear)
        // A different creature the opponent controls — "another artifact or creature", any controller.
        val victim = driver.putCreatureOnBattlefield(driver.player2, "Grizzly Bears")

        driver.advanceToPlayer1Main()
        plusOneCounters(driver, bear) shouldBe 0

        // Destroy the victim for real so its dies (battlefield -> graveyard) trigger fires.
        val doomBlade = driver.putCardInHand(driver.player1, "Doom Blade")
        driver.giveMana(driver.player1, Color.BLACK, 2)
        driver.castSpell(driver.player1, doomBlade, targets = listOf(victim)).isSuccess shouldBe true
        driver.bothPass() // resolve Doom Blade -> victim dies, queuing the Soulcleaver trigger
        driver.state.getBattlefield().contains(victim) shouldBe false
        driver.bothPass() // resolve the Soulcleaver "another ... is put into a graveyard" trigger

        plusOneCounters(driver, bear) shouldBe 1
    }

    test("an artifact dying puts a +1/+1 counter on the equipped creature") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), startingLife = 20)

        val bear = driver.putCreatureOnBattlefield(driver.player1, "Grizzly Bears")
        driver.putEquipmentAttached(driver.player1, "Tarrian's Soulcleaver", bear)
        // A colorless artifact creature — matches the "artifact" branch of the trigger filter.
        val artifact = driver.putCreatureOnBattlefield(driver.player1, "Artifact Creature")

        driver.advanceToPlayer1Main()

        val doomBlade = driver.putCardInHand(driver.player1, "Doom Blade")
        driver.giveMana(driver.player1, Color.BLACK, 2)
        driver.castSpell(driver.player1, doomBlade, targets = listOf(artifact)).isSuccess shouldBe true
        driver.bothPass() // resolve Doom Blade -> artifact dies
        driver.state.getBattlefield().contains(artifact) shouldBe false
        driver.bothPass() // resolve the Soulcleaver trigger

        plusOneCounters(driver, bear) shouldBe 1
    }

    test("unattached Soulcleaver: a creature dying resolves as a no-op") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), startingLife = 20)

        val bear = driver.putCreatureOnBattlefield(driver.player1, "Grizzly Bears")
        // Soulcleaver on the battlefield but attached to nothing.
        driver.putPermanentOnBattlefield(driver.player1, "Tarrian's Soulcleaver")
        val victim = driver.putCreatureOnBattlefield(driver.player2, "Grizzly Bears")

        driver.advanceToPlayer1Main()

        val doomBlade = driver.putCardInHand(driver.player1, "Doom Blade")
        driver.giveMana(driver.player1, Color.BLACK, 2)
        driver.castSpell(driver.player1, doomBlade, targets = listOf(victim)).isSuccess shouldBe true
        driver.bothPass() // resolve Doom Blade
        driver.bothPass() // resolve (or fizzle) the Soulcleaver trigger with no equipped creature

        // No creature to receive the counter — the bear stays at zero and nothing crashes.
        plusOneCounters(driver, bear) shouldBe 0
    }

    test("a token dying puts a +1/+1 counter on the equipped creature (LKI regression)") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), startingLife = 20)

        val bear = driver.putCreatureOnBattlefield(driver.player1, "Grizzly Bears")
        driver.putEquipmentAttached(driver.player1, "Tarrian's Soulcleaver", bear)
        // Opponent's Synapse Necromage: killing it creates two Fungus tokens through the real
        // token-creation path, so the tokens carry a genuine TokenComponent.
        val necromage = driver.putCreatureOnBattlefield(driver.player2, "Synapse Necromage")

        driver.advanceToPlayer1Main()

        driver.giveMana(driver.player1, Color.RED, 1)
        val bolt1 = driver.putCardInHand(driver.player1, "Lightning Bolt")
        driver.castSpell(driver.player1, bolt1, targets = listOf(necromage)).isSuccess shouldBe true
        driver.bothPass() // resolve the bolt -> Necromage dies, queuing both players' triggers
        driver.bothPass() // resolve one trigger (APNAP)
        driver.bothPass() // resolve the other trigger

        // The Necromage (a real card) dying already grew the bear by one and left two tokens.
        plusOneCounters(driver, bear) shouldBe 1
        val fungusTokens = driver.getCreatures(driver.player2).filter { driver.getCardName(it) == "Fungus Token" }
        fungusTokens.size shouldBe 2

        // Kill a token: it dies AND is swept from the game by 704.5d in the same SBA pass, so
        // the Soulcleaver's `Artifact or Creature` union must match on last-known info.
        driver.giveMana(driver.player1, Color.RED, 1)
        val bolt2 = driver.putCardInHand(driver.player1, "Lightning Bolt")
        driver.castSpell(driver.player1, bolt2, targets = listOf(fungusTokens.first())).isSuccess shouldBe true
        driver.bothPass() // resolve the bolt -> token dies and is swept
        driver.bothPass() // resolve the Soulcleaver trigger

        plusOneCounters(driver, bear) shouldBe 2
    }
})
