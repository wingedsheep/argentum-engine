package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.AlternativeCostType
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.ltr.cards.GrimaWormtongue
import com.wingedsheep.mtg.sets.definitions.nem.cards.ReverentSilence
import com.wingedsheep.mtg.sets.definitions.scg.cards.SulfuricVortex
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Reverent Silence {3}{G} — Sorcery.
 *
 * "If you control a Forest, rather than pay this spell's mana cost, you may have each other player
 *  gain 6 life. Destroy all enchantments."
 *
 * Pins the "each other player" form of the gain-life alternative cost: every other player gains,
 * the caster doesn't, and the cost can't be paid while any of them can't gain life (CR 119.7).
 */
class ReverentSilenceScenarioTest : FunSpec({

    fun setup(): Pair<GameTestDriver, List<EntityId>> {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(ReverentSilence, SulfuricVortex, GrimaWormtongue))
        val players = driver.initMultiplayer(List(3) { Deck.of("Forest" to 40) }, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver to players
    }

    fun altCast(player: EntityId, card: EntityId) = CastSpell(
        playerId = player,
        cardId = card,
        useAlternativeCost = true,
        alternativeCostType = AlternativeCostType.SELF_ALTERNATIVE,
    )

    fun altOffered(driver: GameTestDriver, player: EntityId, card: EntityId) =
        driver.legalActions(player).count { legal ->
            val action = legal.action
            action is CastSpell && action.cardId == card && action.useAlternativeCost &&
                action.alternativeCostType == AlternativeCostType.SELF_ALTERNATIVE
        }

    test("each other player gains 6 life and every enchantment is destroyed") {
        val (driver, players) = setup()
        val (you, p2, p3) = players
        driver.putLandOnBattlefield(you, "Forest")
        val vortex = driver.putPermanentOnBattlefield(p2, "Sulfuric Vortex")
        val silence = driver.putCardInHand(you, "Reverent Silence")

        altOffered(driver, you, silence) shouldBe 1
        driver.submit(altCast(you, silence)).error shouldBe null
        // Sulfuric Vortex replaces the gains with nothing, but the cost was still payable.
        driver.getLifeTotal(p2) shouldBe 20
        driver.getLifeTotal(p3) shouldBe 20
        var guard = 0
        while (driver.stackSize > 0 && guard++ < 10) driver.passPriority(driver.state.priorityPlayerId!!)
        driver.state.getBattlefield().contains(vortex) shouldBe false
    }

    test("without a replacement on the battlefield, the other players gain and the caster doesn't") {
        val (driver, players) = setup()
        val (you, p2, p3) = players
        driver.putLandOnBattlefield(you, "Forest")
        val silence = driver.putCardInHand(you, "Reverent Silence")

        driver.submit(altCast(you, silence)).error shouldBe null
        driver.getLifeTotal(p2) shouldBe 26
        driver.getLifeTotal(p3) shouldBe 26
        driver.getLifeTotal(you) shouldBe 20
    }

    test("while any other player can't gain life the cost can't be paid") {
        val (driver, players) = setup()
        val (you, p2, _) = players
        driver.putLandOnBattlefield(you, "Forest")
        driver.putCreatureOnBattlefield(you, "Gríma Wormtongue")
        val silence = driver.putCardInHand(you, "Reverent Silence")

        altOffered(driver, you, silence) shouldBe 0
        driver.submit(altCast(you, silence)).error shouldNotBe null
        driver.getLifeTotal(p2) shouldBe 20
    }
})
