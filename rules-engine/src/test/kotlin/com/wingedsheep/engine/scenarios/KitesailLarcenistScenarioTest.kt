package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.legalactions.EnumerationMode
import com.wingedsheep.engine.legalactions.LegalActionEnumerator
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.lci.cards.KitesailLarcenist
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe

/**
 * Kitesail Larcenist (LCI) — {2}{U} Human Pirate 2/3, Flying, ward {1}.
 *
 *   When this creature enters, for each player, choose up to one other target artifact or creature
 *   that player controls. For as long as this creature remains on the battlefield, the chosen
 *   permanents become Treasure artifacts with "{T}, Sacrifice this artifact: Add one mana of any
 *   color" and lose all other abilities.
 *
 * Exercises the two-player rendering of "for each player, choose up to one ... target" (two
 * optional target slots — one you control, one an opponent controls), the source-keyed
 * [com.wingedsheep.sdk.scripting.Duration.WhileSourceOnBattlefield] transform via a floating
 * [com.wingedsheep.sdk.scripting.effects.BecomeArtifactEffect], and the revert when Kitesail
 * leaves the battlefield.
 */
class KitesailLarcenistScenarioTest : FunSpec({

    val projector = StateProjector()

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + KitesailLarcenist)
        return driver
    }

    /** Cast Kitesail from [you]'s hand (mana pre-supplied) and resolve it so the ETB fires. */
    fun GameTestDriver.castKitesail(you: com.wingedsheep.sdk.model.EntityId) {
        val kitesail = putCardInHand(you, "Kitesail Larcenist")
        giveMana(you, Color.BLUE, 1)
        giveColorlessMana(you, 2)
        castSpell(you, kitesail)
        bothPass() // resolve the creature spell -> Kitesail enters -> ETB trigger asks for targets
    }

    test("casting Kitesail turns a chosen permanent per player into a Treasure that loses all abilities") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), startingLife = 20)
        val you = driver.activePlayer!!
        val opponent = driver.getOpponent(you)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val myCourser = driver.putCreatureOnBattlefield(you, "Centaur Courser")
        val theirCourser = driver.putCreatureOnBattlefield(opponent, "Centaur Courser")

        driver.castKitesail(you)

        // "for each player, choose up to one ... target": slot 0 = a permanent you control,
        // slot 1 = a permanent an opponent controls.
        (driver.pendingDecision is ChooseTargetsDecision) shouldBe true
        driver.submitMultiTargetSelection(
            you,
            mapOf(0 to listOf(myCourser), 1 to listOf(theirCourser))
        )
        driver.bothPass()

        val projected = projector.project(driver.state)
        for (courser in listOf(myCourser, theirCourser)) {
            projected.getTypes(courser) shouldContain "ARTIFACT"
            projected.getTypes(courser) shouldNotContain "CREATURE"
            projected.hasSubtype(courser, "Treasure") shouldBe true
            projected.hasSubtype(courser, "Centaur") shouldBe false
            projected.hasLostAllAbilities(courser) shouldBe true
        }

        // Your transformed Treasure offers exactly one activated ability — the granted
        // "{T}, Sacrifice this artifact: Add one mana of any color" (it survives the ability wipe).
        val actions = LegalActionEnumerator.create(driver.cardRegistry)
            .enumerate(driver.state, you, EnumerationMode.FULL)
        actions.mapNotNull { it.action as? ActivateAbility }
            .count { it.sourceId == myCourser } shouldBe 1
    }

    test("the transform reverts when Kitesail leaves the battlefield") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), startingLife = 20)
        val you = driver.activePlayer!!
        val opponent = driver.getOpponent(you)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val myCourser = driver.putCreatureOnBattlefield(you, "Centaur Courser")
        val theirCourser = driver.putCreatureOnBattlefield(opponent, "Centaur Courser")

        driver.castKitesail(you)
        driver.submitMultiTargetSelection(
            you,
            mapOf(0 to listOf(myCourser), 1 to listOf(theirCourser))
        )
        driver.bothPass()
        projector.project(driver.state).hasSubtype(myCourser, "Treasure") shouldBe true

        // Kitesail leaves -> the "for as long as this creature remains" duration ends.
        val kitesail = driver.findPermanent(you, "Kitesail Larcenist")!!
        driver.moveToGraveyard(kitesail)

        val projected = projector.project(driver.state)
        for (courser in listOf(myCourser, theirCourser)) {
            projected.getTypes(courser) shouldContain "CREATURE"
            projected.getTypes(courser) shouldNotContain "ARTIFACT"
            projected.hasSubtype(courser, "Treasure") shouldBe false
            projected.hasSubtype(courser, "Centaur") shouldBe true
            projected.hasLostAllAbilities(courser) shouldBe false
        }
    }

    test("the granted Treasure mana ability goes away when Kitesail dies") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), startingLife = 20)
        val you = driver.activePlayer!!
        val opponent = driver.getOpponent(you)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val myCourser = driver.putCreatureOnBattlefield(you, "Centaur Courser")
        val theirCourser = driver.putCreatureOnBattlefield(opponent, "Centaur Courser")

        driver.castKitesail(you)
        driver.submitMultiTargetSelection(
            you,
            mapOf(0 to listOf(myCourser), 1 to listOf(theirCourser))
        )
        driver.bothPass()
        driver.state.grantedActivatedAbilities.map { it.entityId }
            .toSet() shouldBe setOf(myCourser, theirCourser)

        // Kill Kitesail with a real removal spell so state-based actions actually run — a raw
        // zone move would revert the floating type effects (they are re-gated on every projection)
        // while leaving the durable grant behind, which is exactly the bug this covers.
        // Kitesail's own controller casts the Bolt, so its ward {1} doesn't trigger.
        val kitesail = driver.findPermanent(you, "Kitesail Larcenist")!!
        val bolt = driver.putCardInHand(you, "Lightning Bolt")
        driver.giveMana(you, Color.RED, 1)
        driver.castSpell(you, bolt, listOf(kitesail))
        driver.bothPass()
        driver.findPermanent(you, "Kitesail Larcenist") shouldBe null

        // The "for as long as this creature remains on the battlefield" grant ends with it: neither
        // chosen permanent may still sacrifice itself for mana.
        driver.state.grantedActivatedAbilities.shouldBeEmpty()
        val enumerator = LegalActionEnumerator.create(driver.cardRegistry)
        for ((player, courser) in listOf(you to myCourser, opponent to theirCourser)) {
            enumerator.enumerate(driver.state, player, EnumerationMode.FULL)
                .mapNotNull { it.action as? ActivateAbility }
                .count { it.sourceId == courser } shouldBe 0
        }
    }

    test("killing Kitesail in response to its own ETB grants nothing at all") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), startingLife = 20)
        val you = driver.activePlayer!!
        val opponent = driver.getOpponent(you)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val myCourser = driver.putCreatureOnBattlefield(you, "Centaur Courser")
        val theirCourser = driver.putCreatureOnBattlefield(opponent, "Centaur Courser")

        driver.castKitesail(you)
        driver.submitMultiTargetSelection(
            you,
            mapOf(0 to listOf(myCourser), 1 to listOf(theirCourser))
        )

        // Kill Kitesail with its ETB trigger still on the stack — the other half of CR 611.2b:
        // "if that duration ends before the moment the effect would first be applied ... the
        // effect does nothing". The trigger still resolves (its targets are legal), so the
        // executor still records the transform and the grant, both keyed to a source that is
        // already gone. Bolt cast by Kitesail's own controller, so ward {1} stays out of the way.
        val kitesail = driver.findPermanent(you, "Kitesail Larcenist")!!
        val bolt = driver.putCardInHand(you, "Lightning Bolt")
        driver.giveMana(you, Color.RED, 1)
        driver.castSpell(you, bolt, listOf(kitesail))
        driver.bothPass()
        driver.findPermanent(you, "Kitesail Larcenist") shouldBe null
        driver.bothPass()

        // Nothing started: no lingering grant, and neither permanent was ever a Treasure.
        driver.state.grantedActivatedAbilities.shouldBeEmpty()
        val projected = projector.project(driver.state)
        for (courser in listOf(myCourser, theirCourser)) {
            projected.getTypes(courser) shouldContain "CREATURE"
            projected.hasSubtype(courser, "Treasure") shouldBe false
            projected.hasLostAllAbilities(courser) shouldBe false
        }
        val enumerator = LegalActionEnumerator.create(driver.cardRegistry)
        for ((player, courser) in listOf(you to myCourser, opponent to theirCourser)) {
            enumerator.enumerate(driver.state, player, EnumerationMode.FULL)
                .mapNotNull { it.action as? ActivateAbility }
                .count { it.sourceId == courser } shouldBe 0
        }
    }

    test("'up to one' lets the controller choose no permanent for either player") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), startingLife = 20)
        val you = driver.activePlayer!!
        val opponent = driver.getOpponent(you)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val myCourser = driver.putCreatureOnBattlefield(you, "Centaur Courser")
        val theirCourser = driver.putCreatureOnBattlefield(opponent, "Centaur Courser")

        driver.castKitesail(you)
        // Decline both optional slots — no targets chosen for any player.
        driver.submitMultiTargetSelection(you, mapOf(0 to emptyList(), 1 to emptyList()))
        driver.bothPass()

        val projected = projector.project(driver.state)
        for (courser in listOf(myCourser, theirCourser)) {
            projected.getTypes(courser) shouldContain "CREATURE"
            projected.hasSubtype(courser, "Treasure") shouldBe false
            projected.hasLostAllAbilities(courser) shouldBe false
        }
    }
})
