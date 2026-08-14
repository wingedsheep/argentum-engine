package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.state.components.identity.FaceDownModeComponent
import com.wingedsheep.engine.state.components.identity.MorphDataComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.mkm.cards.HideInPlainSight
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.effects.FaceDownMode
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Hide in Plain Sight — {3}{G} sorcery: "Look at the top five cards of your library, cloak two of
 * them, and put the rest on the bottom of your library in a random order."
 *
 * Two of the five become face-down 2/2s with ward {2} (CR 701.58a), each turnable face up for its
 * own mana cost if it happens to be a creature card. Cloaking a non-creature card is not a failure
 * mode — it's a 2/2 body that simply never flips.
 */
class HideInPlainSightScenarioTest : FunSpec({

    val bear = card("Plain Test Bear") {
        manaCost = "{1}{G}"
        typeLine = "Creature — Bear"
        power = 2
        toughness = 2
    }

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(HideInPlainSight, bear))
        return driver
    }

    fun GameTestDriver.faceDownPermanents(playerId: com.wingedsheep.sdk.model.EntityId) =
        getPermanents(playerId).filter { state.getEntity(it)?.has<FaceDownComponent>() == true }

    fun GameTestDriver.resolveSpell(playerId: com.wingedsheep.sdk.model.EntityId) {
        bothPass()
        pendingDecision.shouldNotBeNull()
        autoResolveDecision()
        repeat(3) { if (state.priorityPlayerId != null && !isPaused) bothPass() }
    }

    test("cloaks two of the top five as 2/2s with ward, bottoming the other three") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40))
        val player = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        repeat(5) { driver.putCardOnTopOfLibrary(player, "Plain Test Bear") }
        val librarySizeBefore = driver.state.getLibrary(player).size

        val spell = driver.putCardInHand(player, "Hide in Plain Sight")
        driver.giveMana(player, Color.GREEN, 4)
        driver.castSpell(player, spell).error shouldBe null
        driver.resolveSpell(player)

        val cloaked = driver.faceDownPermanents(player)
        cloaked.size shouldBe 2
        cloaked.forEach { id ->
            driver.state.getEntity(id)?.get<FaceDownModeComponent>()?.mode shouldBe FaceDownMode.CLOAK
            driver.state.projectedState.getPower(id) shouldBe 2
            driver.state.projectedState.getToughness(id) shouldBe 2
            driver.state.projectedState.hasKeyword(id, Keyword.WARD) shouldBe true
        }

        // All five left the library; three went back to the bottom.
        driver.state.getLibrary(player).size shouldBe librarySizeBefore - 2
        driver.getGraveyardCardNames(player).contains("Hide in Plain Sight") shouldBe true
    }

    test("a cloaked creature card can be turned face up for its own mana cost") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40))
        val player = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        repeat(5) { driver.putCardOnTopOfLibrary(player, "Plain Test Bear") }
        val spell = driver.putCardInHand(player, "Hide in Plain Sight")
        driver.giveMana(player, Color.GREEN, 4)
        driver.castSpell(player, spell).error shouldBe null
        driver.resolveSpell(player)

        val cloaked = driver.faceDownPermanents(player).first()
        val data = driver.state.getEntity(cloaked)?.get<MorphDataComponent>()
        data.shouldNotBeNull()
        data.procedures.single().mechanic shouldBe FaceDownMode.CLOAK
        data.procedures.single().cost.description shouldBe "{1}{G}" // Plain Test Bear's mana cost
    }

    test("it is a sorcery — no face-down cast option and no instant-speed play") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40))
        val player = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val opponent = driver.getOpponent(player)

        driver.putCardInHand(opponent, "Hide in Plain Sight")
        repeat(4) { driver.putLandOnBattlefield(opponent, "Forest") }

        // It is not the opponent's turn, so the sorcery isn't castable at all.
        driver.legalActions(opponent).none {
            it.description.contains("Hide in Plain Sight")
        } shouldBe true
    }
})
