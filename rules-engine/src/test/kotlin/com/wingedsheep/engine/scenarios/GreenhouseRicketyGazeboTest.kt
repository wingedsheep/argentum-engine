package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.UnlockRoomDoor
import com.wingedsheep.engine.handlers.ConditionEvaluator
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.legalactions.utils.CastPermissionUtils
import com.wingedsheep.engine.mechanics.mana.ManaSolver
import com.wingedsheep.engine.state.components.identity.RoomComponent
import com.wingedsheep.engine.state.components.identity.RoomFaceId
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.dsk.cards.GreenhouseRicketyGazebo
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardLayout
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * End-to-end scenarios for `Greenhouse // Rickety Gazebo` (DSK 181), a mono-green Room, plus the
 * underlying room-face static-ability projection (CR 709.5): a Room face's static abilities
 * function only while that door is unlocked.
 *
 * Greenhouse {2}{G}: "Lands you control have '{T}: Add one mana of any color.'" — a *static*
 * ability printed on a Room face. Rickety Gazebo {3}{G}: "When you unlock this door, mill four
 * cards, then return up to two permanent cards from among them to your hand."
 */
class GreenhouseRicketyGazeboTest : FunSpec({

    // An inline Room whose front face carries a *continuous-projection* static (an anthem). No real
    // DSK Room face produces a ContinuousEffectSourceComponent, so this proves that path + the
    // re-bake on unlock. Anthem Parlor "Creatures you control get +1/+1"; Quiet Study is vanilla.
    val anthemRoom = card("Anthem Parlor // Quiet Study") {
        layout = CardLayout.SPLIT
        colorIdentity = "W"
        face("Anthem Parlor") {
            manaCost = "{W}"
            typeLine = "Enchantment — Room"
            oracleText = "Creatures you control get +1/+1."
            staticAbility {
                ability = ModifyStats(1, 1, GroupFilter(GameObjectFilter.Creature.youControl()))
            }
        }
        face("Quiet Study") {
            manaCost = "{1}{W}"
            typeLine = "Enchantment — Room"
            oracleText = ""
        }
    }

    fun driver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all)
        d.registerCard(GreenhouseRicketyGazebo)
        d.registerCard(anthemRoom)
        d.initMirrorMatch(
            deck = Deck.of(
                "Forest" to 20,
                "Grizzly Bears" to 10,
                "Plains" to 10,
            ),
            skipMulligans = true,
        )
        return d
    }

    test("Greenhouse unlocked grants lands '{T}: Add one mana of any color'") {
        val d = driver()
        val p1 = d.activePlayer!!
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // p1's only colored land is a Forest (a green source).
        val forest = d.putLandOnBattlefield(p1, "Forest")

        // Cast Greenhouse (face 0, {2}{G}); it enters unlocked. The Greenhouse face is a pure
        // static, so there is no door-unlock trigger to resolve.
        val roomId = d.putCardInHand(p1, GreenhouseRicketyGazebo.name)
        d.giveMana(p1, Color.GREEN, 3)
        d.submitSuccess(CastSpell(p1, roomId, faceIndex = 0))
        d.bothPass()

        val room = d.state.getEntity(roomId)?.get<RoomComponent>()
        room shouldNotBe null
        room!!.unlocked shouldBe setOf(RoomFaceId("Greenhouse"))

        // Site 3 (auto-payer): the Forest can now pay an off-color cost via the granted ability.
        val manaSolver = ManaSolver(d.cardRegistry)
        manaSolver.canPay(d.state, p1, ManaCost.parse("{U}")).shouldBeTrue()

        // Site 2 (clickable ability): the Forest is offered the granted mana ability.
        val cpu = CastPermissionUtils(d.cardRegistry, PredicateEvaluator(), ConditionEvaluator())
        cpu.getStaticGrantedActivatedAbilities(forest, d.state)
            .any { it.isManaAbility }
            .shouldBeTrue()
    }

    test("Greenhouse locked does not grant the mana ability; unlocking it turns the grant on") {
        val d = driver()
        val p1 = d.activePlayer!!
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        d.putLandOnBattlefield(p1, "Forest") // sole colored source: green only

        // Cast Rickety Gazebo (face 1, {3}{G}); Greenhouse stays LOCKED.
        val roomId = d.putCardInHand(p1, GreenhouseRicketyGazebo.name)
        d.giveMana(p1, Color.GREEN, 4)
        d.submitSuccess(CastSpell(p1, roomId, faceIndex = 1))
        d.bothPass() // resolve Room; door-unlock trigger goes on the stack
        d.bothPass() // resolve trigger → mill 4, then a "return up to two" selection pauses
        d.submitCardSelection(p1, emptyList()) // return nothing

        d.state.getEntity(roomId)!!.get<RoomComponent>()!!.unlocked shouldBe
            setOf(RoomFaceId("Rickety Gazebo"))

        val manaSolver = ManaSolver(d.cardRegistry)
        // Greenhouse locked: no any-color grant, so the lone Forest can't pay {U} …
        manaSolver.canPay(d.state, p1, ManaCost.parse("{U}")).shouldBeFalse()
        // … but it still makes its own green.
        manaSolver.canPay(d.state, p1, ManaCost.parse("{G}")).shouldBeTrue()

        // Unlock Greenhouse via the sorcery-speed special action ({2}{G}); the grant turns on.
        d.giveMana(p1, Color.GREEN, 3)
        d.submitSuccess(UnlockRoomDoor(p1, roomId, RoomFaceId("Greenhouse")))
        d.state.getEntity(roomId)!!.get<RoomComponent>()!!.unlocked shouldBe
            setOf(RoomFaceId("Rickety Gazebo"), RoomFaceId("Greenhouse"))
        manaSolver.canPay(d.state, p1, ManaCost.parse("{U}")).shouldBeTrue()
    }

    test("Rickety Gazebo: unlocking the door mills four and returns up to two permanent cards") {
        val d = driver()
        val p1 = d.activePlayer!!
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Stack the top four of the library: two nonpermanents (Lightning Bolt) and two permanents
        // (Grizzly Bears). putCardOnTopOfLibrary puts each new card on top, so the last call is the
        // topmost — order within the top four doesn't matter for "mill four".
        val bolt1 = d.putCardOnTopOfLibrary(p1, "Lightning Bolt")
        val bolt2 = d.putCardOnTopOfLibrary(p1, "Lightning Bolt")
        val bear1 = d.putCardOnTopOfLibrary(p1, "Grizzly Bears")
        val bear2 = d.putCardOnTopOfLibrary(p1, "Grizzly Bears")

        // Cast Rickety Gazebo (face 1) → enters unlocked → "when you unlock this door" trigger.
        val roomId = d.putCardInHand(p1, GreenhouseRicketyGazebo.name)
        d.giveMana(p1, Color.GREEN, 4)
        d.submitSuccess(CastSpell(p1, roomId, faceIndex = 1))
        d.bothPass() // resolve Room; trigger goes on the stack
        d.bothPass() // resolve trigger → mill 4 → "return up to two permanent cards" selection

        // Return the two creature (permanent) cards; leave the two Lightning Bolts in the graveyard.
        d.submitCardSelection(p1, listOf(bear1, bear2))

        val hand = d.getHand(p1)
        (bear1 in hand).shouldBeTrue()
        (bear2 in hand).shouldBeTrue()
        val graveyard = d.getGraveyard(p1)
        (bolt1 in graveyard).shouldBeTrue()
        (bolt2 in graveyard).shouldBeTrue()
        // The returned permanents are no longer in the graveyard.
        (bear1 in graveyard).shouldBeFalse()
        (bear2 in graveyard).shouldBeFalse()
    }

    test("a continuous static on a Room face projects only while that door is unlocked (re-baked on unlock)") {
        val d = driver()
        val p1 = d.activePlayer!!
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val bears = d.putPermanentOnBattlefield(p1, "Grizzly Bears") // 2/2

        // Cast Quiet Study (face 1) → Anthem Parlor (the anthem face) stays LOCKED.
        val roomId = d.putCardInHand(p1, anthemRoom.name)
        d.giveMana(p1, Color.WHITE, 2)
        d.submitSuccess(CastSpell(p1, roomId, faceIndex = 1))
        d.bothPass()

        // Anthem locked → no buff.
        d.state.projectedState.getPower(bears) shouldBe 2

        // Unlock Anthem Parlor ({W}); the continuous-effect component is re-baked and the anthem
        // begins functioning (CR 709.5).
        d.giveMana(p1, Color.WHITE, 1)
        d.submitSuccess(UnlockRoomDoor(p1, roomId, RoomFaceId("Anthem Parlor")))
        d.state.projectedState.getPower(bears) shouldBe 3
    }
})
