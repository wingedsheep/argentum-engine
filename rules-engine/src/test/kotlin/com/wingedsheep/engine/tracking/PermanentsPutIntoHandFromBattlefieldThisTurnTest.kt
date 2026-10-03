package com.wingedsheep.engine.tracking

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.state.components.player.PermanentsPutIntoHandFromBattlefieldThisTurnComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Engine coverage for `TurnTracker.PERMANENTS_PUT_INTO_HAND_FROM_BATTLEFIELD` — "if a permanent was
 * put into your hand from the battlefield this turn" (Barrin, Tolarian Archmage). Keyed on the
 * owner, tokens count, other battlefield exits don't, and the tally resets at end of turn.
 */
class PermanentsPutIntoHandFromBattlefieldThisTurnTest : FunSpec({

    val bouncer = card("Self Bounce Probe") {
        manaCost = "{0}"
        typeLine = "Artifact"
        activatedAbility {
            cost = Costs.Free
            effect = Effects.ReturnToHand(EffectTarget.Self)
        }
    }

    val destroyer = card("Self Destroy Probe") {
        manaCost = "{0}"
        typeLine = "Artifact"
        activatedAbility {
            cost = Costs.Free
            effect = Effects.Destroy(EffectTarget.Self)
        }
    }

    val gate = card("Bounced Permanent Gate Probe") {
        manaCost = "{0}"
        typeLine = "Artifact"
        activatedAbility {
            cost = Costs.Free
            restrictions = listOf(
                ActivationRestriction.OnlyIfCondition(Conditions.PermanentPutIntoYourHandFromBattlefieldThisTurn)
            )
            effect = Effects.GainLife(1)
        }
    }

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(bouncer, destroyer, gate))
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40))
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun GameTestDriver.count(player: EntityId) =
        state.getEntity(player)?.get<PermanentsPutIntoHandFromBattlefieldThisTurnComponent>()?.count ?: 0

    fun GameTestDriver.activate(player: EntityId, permanent: EntityId, name: String) =
        submit(ActivateAbility(player, permanent, cardRegistry.requireCard(name).activatedAbilities[0].id))

    fun GameTestDriver.gateIsLegal(player: EntityId, gateId: EntityId) =
        legalActions(player).any { it.affordable && (it.action as? ActivateAbility)?.sourceId == gateId }

    test("a permanent bounced to your hand opens the gate; other battlefield exits don't") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val gateId = driver.putPermanentOnBattlefield(me, "Bounced Permanent Gate Probe")
        val destroyerId = driver.putPermanentOnBattlefield(me, "Self Destroy Probe")
        val bouncerId = driver.putPermanentOnBattlefield(me, "Self Bounce Probe")

        driver.activate(me, destroyerId, "Self Destroy Probe").error shouldBe null
        driver.bothPass()
        withClue("going to the graveyard isn't going to hand") {
            driver.count(me) shouldBe 0
            driver.gateIsLegal(me, gateId) shouldBe false
        }

        driver.activate(me, bouncerId, "Self Bounce Probe").error shouldBe null
        driver.bothPass()
        driver.count(me) shouldBe 1
        driver.gateIsLegal(me, gateId) shouldBe true
    }

    test("the tally is keyed on the owner, counts tokens, and resets at end of turn") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)
        val opponentGate = driver.putPermanentOnBattlefield(opponent, "Bounced Permanent Gate Probe")
        val tokenId = driver.putPermanentOnBattlefield(me, "Self Bounce Probe")
        driver.addComponent(tokenId, TokenComponent)

        driver.activate(me, tokenId, "Self Bounce Probe").error shouldBe null
        driver.bothPass()
        withClue("a bounced token is put into its owner's hand before it ceases to exist") {
            driver.count(me) shouldBe 1
        }
        withClue("the opponent's hand received nothing") {
            driver.count(opponent) shouldBe 0
            driver.gateIsLegal(opponent, opponentGate) shouldBe false
        }

        driver.passPriorityUntil(Step.UPKEEP)
        withClue("a new turn starts from zero") { driver.count(me) shouldBe 0 }
    }
})
