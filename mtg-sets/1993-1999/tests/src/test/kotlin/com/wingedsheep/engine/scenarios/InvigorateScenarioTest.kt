package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.AlternativeCostType
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.legalactions.LegalAction
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.lrw.cards.ThornOfAmethyst
import com.wingedsheep.mtg.sets.definitions.ltr.cards.GrimaWormtongue
import com.wingedsheep.mtg.sets.definitions.mmq.cards.Invigorate
import com.wingedsheep.mtg.sets.definitions.scg.cards.SulfuricVortex
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain

/**
 * Invigorate {2}{G} — Instant.
 *
 * "If you control a Forest, rather than pay this spell's mana cost, you may have an opponent gain
 *  3 life. Target creature gets +4/+4 until end of turn."
 *
 * Pins the opponent-gains-life alternative cost: the opponent (not the caster) gains the life, the
 * caster names which opponent in multiplayer, and a player who can't gain life can't be named while
 * one whose gain is only replaced (Sulfuric Vortex) can (ruling 2020-08-07).
 */
class InvigorateScenarioTest : FunSpec({

    fun altCasts(driver: GameTestDriver, player: EntityId, cardId: EntityId): List<LegalAction> =
        driver.legalActions(player).filter { legal ->
            val action = legal.action
            action is CastSpell && action.cardId == cardId && action.useAlternativeCost &&
                action.alternativeCostType == AlternativeCostType.SELF_ALTERNATIVE
        }

    fun altCast(player: EntityId, card: EntityId, target: EntityId, recipient: EntityId? = null) = CastSpell(
        playerId = player,
        cardId = card,
        targets = listOf(ChosenTarget.Permanent(target)),
        useAlternativeCost = true,
        alternativeCostType = AlternativeCostType.SELF_ALTERNATIVE,
        additionalCostPayment = recipient?.let { AdditionalCostPayment(lifeGainRecipient = it) },
    )

    fun twoPlayer(): Triple<GameTestDriver, EntityId, EntityId> {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(Invigorate, GrimaWormtongue, SulfuricVortex))
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val you = driver.activePlayer!!
        return Triple(driver, you, driver.getOpponent(you))
    }

    test("with a Forest, the opponent gains 3 life instead of mana being paid and the creature gets +4/+4") {
        val (driver, you, opponent) = twoPlayer()
        driver.putLandOnBattlefield(you, "Forest")
        val bear = driver.putCreatureOnBattlefield(you, "Grizzly Bears")
        val invigorate = driver.putCardInHand(you, "Invigorate")

        val offered = altCasts(driver, you, invigorate)
        offered shouldHaveSize 1
        offered.single().description shouldContain "have an opponent gain 3 life"

        driver.submit(altCast(you, invigorate, bear)).error shouldBe null
        driver.getLifeTotal(opponent) shouldBe 23
        driver.getLifeTotal(you) shouldBe 20
        driver.bothPass()

        driver.state.projectedState.getPower(bear) shouldBe 6
        driver.state.projectedState.getToughness(bear) shouldBe 6
        // The Forest stays untapped: no mana was paid.
        driver.state.getBattlefield().count { driver.isTapped(it) } shouldBe 0
    }

    test("without a Forest the alternative is not offered") {
        val (driver, you, _) = twoPlayer()
        val bear = driver.putCreatureOnBattlefield(you, "Grizzly Bears")
        val invigorate = driver.putCardInHand(you, "Invigorate")

        altCasts(driver, you, invigorate) shouldHaveSize 0
        driver.submit(altCast(you, invigorate, bear)).error shouldNotBe null
    }

    test("an opponent who can't gain life can't be made to gain it to pay") {
        val (driver, you, opponent) = twoPlayer()
        driver.putLandOnBattlefield(you, "Forest")
        driver.putCreatureOnBattlefield(you, "Gríma Wormtongue")
        val bear = driver.putCreatureOnBattlefield(you, "Grizzly Bears")
        val invigorate = driver.putCardInHand(you, "Invigorate")

        altCasts(driver, you, invigorate) shouldHaveSize 0
        driver.submit(altCast(you, invigorate, bear, recipient = opponent)).error shouldNotBe null
        driver.getLifeTotal(opponent) shouldBe 20
    }

    test("under Sulfuric Vortex the cost is still payable and the gain is replaced with nothing") {
        val (driver, you, opponent) = twoPlayer()
        driver.putLandOnBattlefield(you, "Forest")
        driver.putPermanentOnBattlefield(opponent, "Sulfuric Vortex")
        val bear = driver.putCreatureOnBattlefield(you, "Grizzly Bears")
        val invigorate = driver.putCardInHand(you, "Invigorate")

        altCasts(driver, you, invigorate) shouldHaveSize 1
        driver.submit(altCast(you, invigorate, bear)).error shouldBe null
        driver.getLifeTotal(opponent) shouldBe 20
        driver.bothPass()
        driver.state.projectedState.getPower(bear) shouldBe 6
    }

    test("in multiplayer the caster names the opponent: one cast per opponent, only that one gains") {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(Invigorate))
        val players = driver.initMultiplayer(List(3) { Deck.of("Forest" to 40) }, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val (you, p2, p3) = players
        driver.putLandOnBattlefield(you, "Forest")
        val bear = driver.putCreatureOnBattlefield(you, "Grizzly Bears")
        val invigorate = driver.putCardInHand(you, "Invigorate")

        val offered = altCasts(driver, you, invigorate)
        offered.map { (it.action as CastSpell).additionalCostPayment?.lifeGainRecipient } shouldContainExactlyInAnyOrder listOf(p2, p3)

        // A cast that names no opponent is rejected when there is more than one to choose from.
        driver.submit(altCast(you, invigorate, bear)).error shouldNotBe null

        driver.submit(altCast(you, invigorate, bear, recipient = p3)).error shouldBe null
        driver.getLifeTotal(p3) shouldBe 23
        driver.getLifeTotal(p2) shouldBe 20
        driver.getLifeTotal(you) shouldBe 20
    }

    test("naming yourself, or a player who isn't an opponent, is rejected") {
        val (driver, you, opponent) = twoPlayer()
        driver.putLandOnBattlefield(you, "Forest")
        val bear = driver.putCreatureOnBattlefield(you, "Grizzly Bears")
        val invigorate = driver.putCardInHand(you, "Invigorate")

        driver.submit(altCast(you, invigorate, bear, recipient = you)).error shouldNotBe null
        driver.submit(altCast(you, invigorate, bear, recipient = bear)).error shouldNotBe null
        driver.getLifeTotal(you) shouldBe 20
        driver.getLifeTotal(opponent) shouldBe 20
    }

    test("a taxed alternative cost still names each opponent on its cast") {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(Invigorate, ThornOfAmethyst))
        val players = driver.initMultiplayer(List(3) { Deck.of("Forest" to 40) }, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val (you, _, _) = players
        driver.putLandOnBattlefield(you, "Forest")
        driver.putPermanentOnBattlefield(you, "Thorn of Amethyst")
        driver.putCreatureOnBattlefield(you, "Grizzly Bears")
        val invigorate = driver.putCardInHand(you, "Invigorate")

        val descriptions = altCasts(driver, you, invigorate).map { it.description }
        descriptions shouldHaveSize 2
        descriptions.toSet() shouldHaveSize 2
        descriptions.forEach { it shouldContain "gain 3 life" }
    }
})
