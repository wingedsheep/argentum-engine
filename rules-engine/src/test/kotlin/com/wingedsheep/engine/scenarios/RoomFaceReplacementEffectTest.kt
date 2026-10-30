package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.UnlockRoomDoor
import com.wingedsheep.engine.handlers.continuations.entityIdToChosenTarget
import com.wingedsheep.engine.state.components.battlefield.ReplacementEffectSourceComponent
import com.wingedsheep.engine.state.components.identity.RoomComponent
import com.wingedsheep.engine.state.components.identity.RoomFaceId
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardLayout
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyDamageAmount
import com.wingedsheep.sdk.scripting.events.DamageType
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * A replacement effect printed on a Room door exists only while that door is unlocked (CR 709.5):
 * casting the door, unlocking it with the special action, and locking it again each switch the
 * baked replacement component on or off. Proven with a Torture Pit–shaped "+2 noncombat damage to
 * an opponent" door next to a blank door.
 */
class RoomFaceReplacementEffectTest : FunSpec({

    val pitRoom = card("Blank Hall // Test Pit") {
        layout = CardLayout.SPLIT
        face("Blank Hall") {
            manaCost = "{1}{R}"
            typeLine = "Enchantment — Room"
            oracleText = "Blank."
        }
        face("Test Pit") {
            manaCost = "{2}{R}"
            typeLine = "Enchantment — Room"
            oracleText = "If a source you control would deal noncombat damage to an opponent, it deals " +
                "that much damage plus 2 instead."
            replacementEffect(
                ModifyDamageAmount(
                    modifier = 2,
                    appliesTo = EventPattern.DamageEvent(
                        source = GameObjectFilter.Any.youControl(),
                        recipient = Recipient.Opponent,
                        damageType = DamageType.NonCombat,
                    ),
                )
            )
        }
    }

    // "{T}: Lock a door of target Room you control."
    val lockKey = card("Test Pit Lock Key") {
        manaCost = "{1}"
        typeLine = "Artifact"
        activatedAbility {
            cost = Costs.Tap
            target(TargetObject(filter = TargetFilter(GameObjectFilter.Any.withSubtype(Subtype.ROOM).youControl())))
            effect = Effects.LockDoor(EffectTarget.ContextTarget(0))
        }
    }

    fun driver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all)
        d.registerCard(pitRoom)
        d.registerCard(lockKey)
        d.initMirrorMatch(deck = Deck.of("Mountain" to 40), skipMulligans = true)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return d
    }

    fun GameTestDriver.castRoom(p: EntityId, faceIndex: Int): EntityId {
        val roomId = putCardInHand(p, pitRoom.name)
        giveMana(p, Color.RED, 3)
        submitSuccess(CastSpell(p, roomId, faceIndex = faceIndex))
        bothPass()
        return roomId
    }

    /** Bolts [target] and returns how much life it lost. */
    fun GameTestDriver.boltLifeLoss(p: EntityId, target: EntityId): Int {
        val before = getLifeTotal(target)
        val bolt = putCardInHand(p, "Lightning Bolt")
        giveMana(p, Color.RED, 1)
        castSpell(p, bolt, listOf(target))
        bothPass()
        return before - getLifeTotal(target)
    }

    test("a locked door's replacement effect doesn't apply") {
        val d = driver()
        val p = d.activePlayer!!
        val roomId = d.castRoom(p, faceIndex = 0)
        d.state.getEntity(roomId)!!.get<RoomComponent>()!!.unlocked shouldBe setOf(RoomFaceId("Blank Hall"))

        d.boltLifeLoss(p, d.getOpponent(p)) shouldBe 3
    }

    test("casting the replacement door makes it apply to noncombat damage to an opponent only") {
        val d = driver()
        val p = d.activePlayer!!
        d.castRoom(p, faceIndex = 1)

        d.boltLifeLoss(p, d.getOpponent(p)) shouldBe 5
        // Not an opponent: the controller's own face takes the printed 3.
        d.boltLifeLoss(p, p) shouldBe 3
    }

    test("unlocking the door with the special action turns the replacement on") {
        val d = driver()
        val p = d.activePlayer!!
        val roomId = d.castRoom(p, faceIndex = 0)
        // The blank door has no replacement effects, so nothing is baked yet.
        d.state.getEntity(roomId)!!.get<ReplacementEffectSourceComponent>() shouldBe null

        d.giveMana(p, Color.RED, 3)
        d.submitSuccess(UnlockRoomDoor(p, roomId, RoomFaceId("Test Pit")))
        d.boltLifeLoss(p, d.getOpponent(p)) shouldBe 5
    }

    test("locking the door turns the replacement off again") {
        val d = driver()
        val p = d.activePlayer!!
        val roomId = d.castRoom(p, faceIndex = 1)
        d.boltLifeLoss(p, d.getOpponent(p)) shouldBe 5

        val key = d.putPermanentOnBattlefield(p, lockKey.name)
        d.submitSuccess(
            ActivateAbility(
                playerId = p,
                sourceId = key,
                abilityId = lockKey.activatedAbilities.first().id,
                targets = listOf(entityIdToChosenTarget(d.state, roomId)),
            )
        )
        d.bothPass()
        d.state.getEntity(roomId)!!.get<RoomComponent>()!!.unlocked shouldBe emptySet()

        // The locked door was the only one with a replacement: none stays baked, the bolt deals 3.
        d.state.getEntity(roomId)!!.get<ReplacementEffectSourceComponent>() shouldBe null
        d.boltLifeLoss(p, d.getOpponent(p)) shouldBe 3
    }
})
