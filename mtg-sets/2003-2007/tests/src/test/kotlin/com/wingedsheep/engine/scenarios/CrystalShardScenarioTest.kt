package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.support.answerForcedPayments
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.mrd.cards.CrystalShard
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import com.wingedsheep.engine.core.Outcome
import io.kotest.matchers.shouldNotBe

/**
 * Crystal Shard (MRD #159) — "{3}, {T} or {U}, {T}: Return target creature to its owner's hand
 * unless its controller pays {1}."
 *
 * The gate is *inverted*: paying is what stops the bounce, so the bounce lives in the gate's
 * `otherwise` branch. Everything that could plausibly go wrong is a wiring question about *who* the
 * gate talks to — the shard's controller is not the creature's controller — so these tests pin the
 * three answers: the opponent is the one prompted, the opponent is the one charged, and a paying
 * opponent keeps their creature. The fourth test pins the affordability short-circuit: `Gate.MayPay`
 * must not offer an impossible "yes" to a tapped-out player.
 */
class CrystalShardScenarioTest : FunSpec({

    val genericAbility = CrystalShard.activatedAbilities.first().id
    val blueAbility = CrystalShard.activatedAbilities[1].id

    fun driver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + CrystalShard)
        d.initMirrorMatch(deck = Deck.of("Island" to 40), skipMulligans = true, startingPlayer = 0)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return d
    }

    /** The shard on the battlefield, untapped and ready (activation costs {T}, not summoning sick). */
    fun GameTestDriver.shard(): EntityId = putPermanentOnBattlefield(player1, "Crystal Shard")

    test("the creature's controller — not the shard's — is the one asked to pay") {
        val d = driver()
        val shard = d.shard()
        val bear = d.putCreatureOnBattlefield(d.player2, "Grizzly Bears")
        d.giveColorlessMana(d.player1, 3)
        d.giveColorlessMana(d.player2, 1)

        d.submit(
            ActivateAbility(d.player1, shard, genericAbility, targets = listOf(ChosenTarget.Permanent(bear)))
        ).outcome shouldBe Outcome.Done
        d.bothPass()

        withClue("'its controller pays' points at player2, who controls the bear") {
            d.pendingDecision?.playerId shouldBe d.player2
        }
    }

    test("paying {1} keeps the creature on the battlefield and empties that player's pool") {
        val d = driver()
        val shard = d.shard()
        val bear = d.putCreatureOnBattlefield(d.player2, "Grizzly Bears")
        d.giveColorlessMana(d.player1, 3)
        d.giveColorlessMana(d.player2, 1)

        d.submit(
            ActivateAbility(d.player1, shard, genericAbility, targets = listOf(ChosenTarget.Permanent(bear)))
        ).outcome shouldBe Outcome.Done
        d.bothPass()
        d.submitYesNo(d.player2, true)

        withClue("the bounce is the gate's `otherwise` — a paid {1} skips it") {
            d.state.getBattlefield().contains(bear) shouldBe true
        }
        d.getHand(d.player2).none { d.getCardName(it) == "Grizzly Bears" } shouldBe true
    }

    test("declining bounces the creature to its owner's hand") {
        val d = driver()
        val shard = d.shard()
        val bear = d.putCreatureOnBattlefield(d.player2, "Grizzly Bears")
        d.giveColorlessMana(d.player1, 3)
        d.giveColorlessMana(d.player2, 1)

        d.submit(
            ActivateAbility(d.player1, shard, genericAbility, targets = listOf(ChosenTarget.Permanent(bear)))
        ).outcome shouldBe Outcome.Done
        d.bothPass()
        d.submitYesNo(d.player2, false)

        d.state.getBattlefield().contains(bear) shouldBe false
        withClue("it goes to its *owner's* hand — player2 both owns and controls it here") {
            d.getHand(d.player2).any { d.getCardName(it) == "Grizzly Bears" } shouldBe true
        }
    }

    test("a controller with no mana can only refuse — the bounce happens") {
        val d = driver()
        val shard = d.shard()
        val bear = d.putCreatureOnBattlefield(d.player2, "Grizzly Bears")
        d.giveColorlessMana(d.player1, 3)
        // player2 has an empty pool and no lands to tap.

        d.submit(
            ActivateAbility(d.player1, shard, genericAbility, targets = listOf(ChosenTarget.Permanent(bear)))
        ).outcome shouldBe Outcome.Done
        d.bothPass()

        // The hand could hold a Spirit Guide, so Gate.MayPay asks, but never offers an impossible yes.
        d.answerForcedPayments()
        d.pendingDecision shouldBe null
        d.state.getBattlefield().contains(bear) shouldBe false
    }

    test("the {U} half of the printed ability does the same thing for one blue mana") {
        val d = driver()
        val shard = d.shard()
        val bear = d.putCreatureOnBattlefield(d.player2, "Grizzly Bears")
        d.giveMana(d.player1, Color.BLUE, 1)

        d.submit(
            ActivateAbility(d.player1, shard, blueAbility, targets = listOf(ChosenTarget.Permanent(bear)))
        ).outcome shouldBe Outcome.Done
        d.bothPass()
        d.answerForcedPayments()

        d.state.getBattlefield().contains(bear) shouldBe false
    }

    test("only one of the two costs can be paid per untap — both include the shard's {T}") {
        val d = driver()
        val shard = d.shard()
        val bear = d.putCreatureOnBattlefield(d.player2, "Grizzly Bears")
        val other = d.putCreatureOnBattlefield(d.player2, "Centaur Courser")
        d.giveColorlessMana(d.player1, 3)
        d.giveMana(d.player1, Color.BLUE, 1)

        d.submit(
            ActivateAbility(d.player1, shard, genericAbility, targets = listOf(ChosenTarget.Permanent(bear)))
        ).outcome shouldBe Outcome.Done

        withClue("the shard is now tapped, so the {U} half has no {T} left to pay") {
            d.submit(
                ActivateAbility(d.player1, shard, blueAbility, targets = listOf(ChosenTarget.Permanent(other)))
            ).outcome shouldNotBe Outcome.Done
        }
    }
})
