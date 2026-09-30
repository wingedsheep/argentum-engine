package com.wingedsheep.engine.mechanics.layers

import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.CardType
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantCardType
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * The cross-zone half of [GrantCardType] — Encroaching Mycosynth's "Nonland permanents you control
 * are artifacts in addition to their other types. The same is true for permanent spells you control
 * and nonland permanent cards you own that aren't on the battlefield."
 *
 * Pins: the battlefield half (nonland, yours only), the stack half (your permanent spells, not your
 * instants, not an opponent's spells), the owned-cards half across hand/graveyard/library/exile
 * (nonland permanent cards only, owner only), the flags defaulting off, the grant ending with its
 * source, and the two downstream readers that see a spell off the battlefield — cast triggers and
 * cast history.
 */
class CrossZoneGrantCardTypeTest : FunSpec({

    val mycosynth = card("Test Mycosynth") {
        manaCost = "{3}{U}"
        typeLine = "Artifact"
        staticAbility {
            ability = GrantCardType(
                cardType = "ARTIFACT",
                filter = GroupFilter(GameObjectFilter.NonlandPermanent.youControl()),
                includeControlledSpells = true,
                includeOwnedCardsOutsideBattlefield = true
            )
        }
    }

    val battlefieldOnly = card("Test Battlefield Artificer") {
        manaCost = "{3}{U}"
        typeLine = "Enchantment"
        staticAbility {
            ability = GrantCardType(
                cardType = "ARTIFACT",
                filter = GroupFilter(GameObjectFilter.NonlandPermanent.youControl())
            )
        }
    }

    val artifactWatcher = card("Test Artifact Watcher") {
        manaCost = "{1}"
        typeLine = "Enchantment"
        triggeredAbility {
            trigger = Triggers.you.casts(GameObjectFilter.Artifact)
            effect = Effects.GainLife(1)
        }
    }

    val predicates = PredicateEvaluator(cardRegistry = null)

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(mycosynth, battlefieldOnly, artifactWatcher))
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun GameTestDriver.isArtifact(id: EntityId): Boolean =
        predicates.matches(state, state.projectedState, id, GameObjectFilter.Artifact, PredicateContext(controllerId = player1))

    test("nonland permanents you control become artifacts; lands and an opponent's permanents don't") {
        val driver = newDriver()
        val me = driver.player1
        driver.putPermanentOnBattlefield(me, "Test Mycosynth")
        val mine = driver.putCreatureOnBattlefield(me, "Centaur Courser")
        val land = driver.putLandOnBattlefield(me, "Forest")
        val theirs = driver.putCreatureOnBattlefield(driver.player2, "Centaur Courser")

        driver.isArtifact(mine) shouldBe true
        driver.state.projectedState.isCreature(mine) shouldBe true
        driver.isArtifact(land) shouldBe false
        driver.isArtifact(theirs) shouldBe false
    }

    test("nonland permanent cards you own outside the battlefield are artifacts, in every such zone") {
        val driver = newDriver()
        val me = driver.player1
        driver.putPermanentOnBattlefield(me, "Test Mycosynth")

        val inHand = driver.putCardInHand(me, "Centaur Courser")
        val inGraveyard = driver.putCardInGraveyard(me, "Test Enchantment")
        val onLibrary = driver.putCardOnTopOfLibrary(me, "Centaur Courser")
        val inExile = driver.putCardInExile(me, "Centaur Courser")
        val instant = driver.putCardInHand(me, "Lightning Bolt")
        val landCard = driver.putCardInHand(me, "Forest")
        val theirCard = driver.putCardInHand(driver.player2, "Centaur Courser")

        driver.isArtifact(inHand) shouldBe true
        driver.isArtifact(inGraveyard) shouldBe true
        driver.isArtifact(onLibrary) shouldBe true
        driver.isArtifact(inExile) shouldBe true
        driver.isArtifact(instant) shouldBe false
        driver.isArtifact(landCard) shouldBe false
        driver.isArtifact(theirCard) shouldBe false
    }

    test("your permanent spells are artifact spells; your instants and an opponent's spells aren't") {
        val driver = newDriver()
        val me = driver.player1
        driver.putPermanentOnBattlefield(me, "Test Mycosynth")

        val courser = driver.putCardInHand(me, "Centaur Courser")
        driver.giveMana(me, Color.GREEN, 4)
        driver.castSpell(me, courser)
        driver.isArtifact(courser) shouldBe true

        val bolt = driver.putCardInHand(me, "Lightning Bolt")
        driver.giveMana(me, Color.RED, 1)
        driver.castSpell(me, bolt, listOf(driver.player2))
        driver.isArtifact(bolt) shouldBe false
    }

    test("an opponent's permanent spell on the stack is not an artifact") {
        val driver = newDriver()
        driver.putPermanentOnBattlefield(driver.player2, "Test Mycosynth")
        val courser = driver.putCardInHand(driver.player1, "Centaur Courser")
        driver.giveMana(driver.player1, Color.GREEN, 4)
        driver.castSpell(driver.player1, courser)
        driver.isArtifact(courser) shouldBe false
    }

    test("without the cross-zone flags the grant stays on the battlefield") {
        val driver = newDriver()
        val me = driver.player1
        driver.putPermanentOnBattlefield(me, "Test Battlefield Artificer")
        val mine = driver.putCreatureOnBattlefield(me, "Centaur Courser")
        val inHand = driver.putCardInHand(me, "Centaur Courser")

        driver.isArtifact(mine) shouldBe true
        driver.isArtifact(inHand) shouldBe false
    }

    test("the grant ends when its source leaves the battlefield") {
        val driver = newDriver()
        val me = driver.player1
        val source = driver.putPermanentOnBattlefield(me, "Test Mycosynth")
        val inHand = driver.putCardInHand(me, "Centaur Courser")
        driver.isArtifact(inHand) shouldBe true

        driver.moveToGraveyard(source)
        driver.isArtifact(inHand) shouldBe false
    }

    test("\"whenever you cast an artifact spell\" sees a granted artifact spell") {
        val driver = newDriver()
        val me = driver.player1
        driver.putPermanentOnBattlefield(me, "Test Mycosynth")
        driver.putPermanentOnBattlefield(me, "Test Artifact Watcher")
        val life = driver.getLifeTotal(me)

        val courser = driver.putCardInHand(me, "Centaur Courser")
        driver.giveMana(me, Color.GREEN, 4)
        driver.castSpell(me, courser)
        driver.bothPass()

        driver.getLifeTotal(me) shouldBe life + 1
    }

    test("cast history records the spell as the artifact it was cast as") {
        val driver = newDriver()
        val me = driver.player1
        driver.putPermanentOnBattlefield(me, "Test Mycosynth")
        val courser = driver.putCardInHand(me, "Centaur Courser")
        driver.giveMana(me, Color.GREEN, 4)
        driver.castSpell(me, courser)

        val record = driver.state.spellsCastThisTurnByPlayer[me]!!.single()
        (CardType.ARTIFACT in record.typeLine.cardTypes) shouldBe true
        (CardType.CREATURE in record.typeLine.cardTypes) shouldBe true
        predicates.matchesFilter(record, GameObjectFilter.Artifact) shouldBe true
    }
})
