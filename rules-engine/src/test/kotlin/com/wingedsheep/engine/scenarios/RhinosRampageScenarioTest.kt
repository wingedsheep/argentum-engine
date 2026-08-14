package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.spm.cards.RhinosRampage
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Rhino's Rampage — {R/G} sorcery
 *  "Target creature you control gets +1/+0 until end of turn. It fights target creature an opponent
 *   controls. When excess damage is dealt to the creature an opponent controls this way, destroy up
 *   to one target noncreature artifact with mana value 3 or less."
 *
 * The reflexive artifact destruction is gated on the fight's *excess* damage (CR 120.4a), which the
 * fight captures into the `excess` pipeline number. Verified:
 *  - excess dealt → the controller may destroy an eligible artifact; the mv ≤ 3 filter excludes
 *    larger artifacts (the +1/+0 pump is what pushes a 2-power creature over lethal, so it is what
 *    produces the excess);
 *  - no excess (exactly-lethal / non-lethal fight) → the reflexive never fires, no decision, nothing
 *    destroyed;
 *  - "up to one" — even when excess is dealt, the controller may decline and destroy nothing.
 */
class RhinosRampageScenarioTest : FunSpec({

    // 2/4 attacker: base power 2 is exactly lethal to a 2-toughness creature; the +1/+0 pump takes
    // it to 3 power, producing 1 excess. Toughness 4 lets it survive the return blow.
    val myFighter = card("Test Rampage Fighter") {
        manaCost = "{2}{G}"
        typeLine = "Creature — Beast"
        power = 2
        toughness = 4
    }
    // 2/2 opponent: pumped fighter deals 3 → lethal 2 → 1 excess.
    val theirSmall = card("Test Rampage Small") {
        manaCost = "{1}{R}"
        typeLine = "Creature — Beast"
        power = 2
        toughness = 2
    }
    // 3/3 opponent: pumped fighter deals exactly 3 → lethal, 0 excess.
    val theirTough = card("Test Rampage Tough") {
        manaCost = "{2}{R}"
        typeLine = "Creature — Beast"
        power = 3
        toughness = 3
    }
    // Noncreature artifact, mana value 2 — eligible for destruction.
    val smallArtifact = card("Test Rampage Trinket") {
        manaCost = "{2}"
        typeLine = "Artifact"
    }
    // Noncreature artifact, mana value 4 — excluded by "mana value 3 or less".
    val bigArtifact = card("Test Rampage Engine") {
        manaCost = "{4}"
        typeLine = "Artifact"
    }

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(
            TestCards.all + listOf(
                RhinosRampage, myFighter, theirSmall, theirTough, smallArtifact, bigArtifact
            )
        )
        return driver
    }

    /** Pass priority until either a decision is raised or the stack empties. */
    fun GameTestDriver.advanceToDecisionOrEmpty() {
        var guard = 0
        while (pendingDecision == null && stackSize > 0 && guard < 50) {
            bothPass()
            guard++
        }
    }

    fun GameTestDriver.onBattlefield(id: EntityId): Boolean =
        state.zones.any { (key, entities) -> key.zoneType == Zone.BATTLEFIELD && id in entities }

    fun GameTestDriver.inGraveyard(playerId: EntityId, id: EntityId): Boolean =
        state.zones[ZoneKey(playerId, Zone.GRAVEYARD)]?.contains(id) == true

    test("excess fight damage lets you destroy an eligible artifact; the mv-3 filter excludes larger ones") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40))
        val active = driver.activePlayer!!
        val opponent = driver.getOpponent(active)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val mine = driver.putCreatureOnBattlefield(active, "Test Rampage Fighter")   // 2/4
        val theirs = driver.putCreatureOnBattlefield(opponent, "Test Rampage Small") // 2/2
        val trinket = driver.putPermanentOnBattlefield(opponent, "Test Rampage Trinket") // mv 2
        val engine = driver.putPermanentOnBattlefield(opponent, "Test Rampage Engine")   // mv 4

        val rampage = driver.putCardInHand(active, "Rhino's Rampage")
        driver.giveMana(active, Color.RED, 1) // {R/G}
        driver.castSpell(active, rampage, targets = listOf(mine, theirs)).isSuccess shouldBe true

        // Pump (+1/+0 → 3 power) then fight: 3 damage to a 2/2 → lethal 2 → 1 excess → reflexive fires.
        driver.advanceToDecisionOrEmpty()
        driver.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
        driver.submitYesNo(active, true)
        driver.advanceToDecisionOrEmpty()

        // Only the mv-2 artifact is a legal target (auto-selected) and is destroyed; mv-4 survives.
        driver.onBattlefield(trinket) shouldBe false
        driver.inGraveyard(opponent, trinket) shouldBe true
        driver.onBattlefield(engine) shouldBe true
    }

    test("a fight that deals no excess never fires the reflexive and destroys nothing") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40))
        val active = driver.activePlayer!!
        val opponent = driver.getOpponent(active)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val mine = driver.putCreatureOnBattlefield(active, "Test Rampage Fighter")   // 2/4
        val theirs = driver.putCreatureOnBattlefield(opponent, "Test Rampage Tough") // 3/3
        val trinket = driver.putPermanentOnBattlefield(opponent, "Test Rampage Trinket") // mv 2

        val rampage = driver.putCardInHand(active, "Rhino's Rampage")
        driver.giveMana(active, Color.RED, 1)
        driver.castSpell(active, rampage, targets = listOf(mine, theirs)).isSuccess shouldBe true

        // Pumped to 3 power, 3 damage to a 3-toughness creature is exactly lethal → 0 excess.
        driver.advanceToDecisionOrEmpty()

        // No may-decision was raised and the artifact is untouched.
        driver.pendingDecision shouldBe null
        driver.onBattlefield(trinket) shouldBe true
    }

    test("'up to one' — the controller may decline to destroy even when excess is dealt") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40))
        val active = driver.activePlayer!!
        val opponent = driver.getOpponent(active)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val mine = driver.putCreatureOnBattlefield(active, "Test Rampage Fighter")   // 2/4
        val theirs = driver.putCreatureOnBattlefield(opponent, "Test Rampage Small") // 2/2
        val trinket = driver.putPermanentOnBattlefield(opponent, "Test Rampage Trinket") // mv 2

        val rampage = driver.putCardInHand(active, "Rhino's Rampage")
        driver.giveMana(active, Color.RED, 1)
        driver.castSpell(active, rampage, targets = listOf(mine, theirs)).isSuccess shouldBe true

        // 1 excess → the reflexive fires and offers the destroy, but the controller declines.
        driver.advanceToDecisionOrEmpty()
        driver.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
        driver.submitYesNo(active, false)
        driver.advanceToDecisionOrEmpty()

        driver.onBattlefield(trinket) shouldBe true
    }
})
