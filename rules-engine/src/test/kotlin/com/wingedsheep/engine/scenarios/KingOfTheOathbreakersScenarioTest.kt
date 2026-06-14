package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.PhasedOutComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.ltr.cards.KingOfTheOathbreakers
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * King of the Oathbreakers (Gap 32 — phasing, CR 702.26).
 *
 * Flying
 * Whenever King of the Oathbreakers or another Spirit you control becomes the target of a
 * spell, it phases out.
 * Whenever King of the Oathbreakers or another Spirit you control phases in, create a tapped
 * 1/1 white Spirit creature token with flying.
 *
 * Verifies the actual oracle behaviour:
 *  - targeting King with a spell phases it out (it leaves the battlefield view, can't be hit, the
 *    targeting spell fizzles for lack of a legal target),
 *  - King phases back in on its controller's next untap step,
 *  - the phase-in fires the "create a tapped 1/1 white flying Spirit token" trigger.
 */
class KingOfTheOathbreakersScenarioTest : FunSpec({

    /**
     * Advance to [player]'s next upkeep. The untap step (where phasing-in happens) gives no
     * priority and is not observable as a stopping point, so we stop at the upkeep that
     * immediately follows it — by then phase-in has already run.
     */
    fun GameTestDriver.passUntilUpkeepOf(player: EntityId, maxTurns: Int = 8) {
        repeat(maxTurns) {
            passPriorityUntil(Step.UPKEEP, maxPasses = 200)
            if (activePlayer == player) return
            // Move off this upkeep so the next passPriorityUntil(UPKEEP) lands on a new turn.
            passPriorityUntil(Step.PRECOMBAT_MAIN, maxPasses = 200)
        }
        throw AssertionError("Never reached $player's upkeep")
    }

    test("targeting a Spirit you control phases it out, the spell fizzles, then it phases back in and makes a token") {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(KingOfTheOathbreakers))
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40))
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val active = driver.activePlayer!!
        val opponent = driver.getOpponent(active)

        val king = driver.putCreatureOnBattlefield(active, "King of the Oathbreakers")

        // Cast Giant Growth (a spell) targeting our own King — "becomes the target of a spell".
        val growth = driver.putCardInHand(active, "Giant Growth")
        driver.giveMana(active, Color.GREEN, 1)
        driver.castSpell(active, growth, targets = listOf(king))
        // Resolve the phase-out trigger, then the (now-targetless) Giant Growth.
        driver.bothPass()
        driver.bothPass()

        // King phased out: it's gone from the battlefield view but still physically present
        // with a PhasedOutComponent recording its controller.
        driver.state.getBattlefield().contains(king) shouldBe false
        driver.state.getEntity(king)?.has<PhasedOutComponent>() shouldBe true
        driver.state.getEntity(king)?.get<PhasedOutComponent>()?.phasedOutByController shouldBe active

        // No token yet — nothing has phased in. A created Spirit token is a TokenComponent
        // permanent with the Spirit subtype controlled by the active player (the printed token's
        // name is "Spirit Token", so match on subtype rather than the raw name).
        fun spiritTokens() = driver.state.getBattlefield().filter { id ->
            val c = driver.state.getEntity(id) ?: return@filter false
            c.has<TokenComponent>() &&
                c.get<CardComponent>()?.typeLine?.hasSubtype(Subtype.SPIRIT) == true &&
                c.get<ControllerComponent>()?.playerId == active
        }
        spiritTokens().size shouldBe 0

        // Advance to the controller's next turn: King phases in during their untap step, then
        // the phase-in trigger resolves during upkeep.
        driver.passUntilUpkeepOf(active)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN, maxPasses = 200)

        // King is back on the battlefield (same object) and no longer phased out.
        driver.state.getBattlefield().contains(king) shouldBe true
        driver.state.getEntity(king)?.has<PhasedOutComponent>() shouldBe false

        // The phase-in trigger created a tapped 1/1 white Spirit token with flying.
        val tokens = spiritTokens()
        tokens.size shouldBe 1
        val tokenId = tokens.first()
        driver.state.getEntity(tokenId)?.has<TappedComponent>() shouldBe true
        val projected = driver.state.projectedState
        projected.getPower(tokenId) shouldBe 1
        projected.getToughness(tokenId) shouldBe 1
        driver.state.getEntity(tokenId)?.get<CardComponent>()?.colors shouldBe setOf(Color.WHITE)
    }
})
