package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CardsSelectedResponse
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.TargetsResponse
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.neo.cards.ScrapWelder
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Scrap Welder — "{T}, Sacrifice an artifact with mana value X: Return target artifact card with
 * mana value less than X from your graveyard to the battlefield. It gains haste until end of turn."
 *
 * The sacrifice is a single-permanent `VariablePermanents` cost (`maxCount = 1`): the artifact
 * picked announces X (CR 601.2b), and the graveyard target must have mana value strictly less than
 * it — equal is not enough (the card's ruling).
 */
class ScrapWelderScenarioTest : FunSpec({

    val mv1 = card("Test MV1 Artifact") { manaCost = "{1}"; typeLine = "Artifact" }
    val mv2 = card("Test MV2 Artifact") { manaCost = "{2}"; typeLine = "Artifact Creature — Construct"; power = 2; toughness = 2 }
    val mv3 = card("Test MV3 Artifact") { manaCost = "{3}"; typeLine = "Artifact" }

    val abilityId = ScrapWelder.activatedAbilities.single().id

    fun setup(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + listOf(ScrapWelder, mv1, mv2, mv3))
        d.initMirrorMatch(deck = Deck.of("Mountain" to 40), skipMulligans = true)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return d
    }

    fun GameTestDriver.welder(player: com.wingedsheep.sdk.model.EntityId) =
        putCreatureOnBattlefield(player, "Scrap Welder").also { removeSummoningSickness(it) }

    test("only an artifact that leaves a lesser-mana-value target is offered, then the target is strictly below X") {
        val d = setup()
        val you = d.activePlayer!!
        val welder = d.welder(you)
        val small = d.putPermanentOnBattlefield(you, "Test MV1 Artifact")
        val big = d.putPermanentOnBattlefield(you, "Test MV3 Artifact")
        val gy1 = d.putCardInGraveyard(you, "Test MV1 Artifact")
        val gy2 = d.putCardInGraveyard(you, "Test MV2 Artifact")
        val gy3 = d.putCardInGraveyard(you, "Test MV3 Artifact")

        d.submit(ActivateAbility(playerId = you, sourceId = welder, abilityId = abilityId))
            .outcome.shouldBeInstanceOf<Outcome.Paused>()

        // Sacrificing the MV1 artifact would need a graveyard artifact of mana value 0 — there is
        // none, so it isn't offered. Exactly one artifact may be chosen.
        val sacrifice = d.pendingDecision
        sacrifice.shouldBeInstanceOf<SelectCardsDecision>()
        sacrifice.options shouldBe listOf(big)
        sacrifice.minSelections shouldBe 1
        sacrifice.maxSelections shouldBe 1
        d.submitDecision(you, CardsSelectedResponse(sacrifice.id, listOf(big)))

        // X = 3: mana value 1 and 2 are legal, mana value 3 (equal to X) is not.
        val target = d.pendingDecision
        target.shouldBeInstanceOf<ChooseTargetsDecision>()
        target.legalTargets[0]!!.toSet() shouldBe setOf(gy1, gy2)
        d.submitDecision(you, TargetsResponse(target.id, mapOf(0 to listOf(gy2))))

        d.state.getZone(ZoneKey(you, Zone.GRAVEYARD)).contains(big) shouldBe true
        d.bothPass()

        d.state.getZone(ZoneKey(you, Zone.BATTLEFIELD)).contains(gy2) shouldBe true
        d.state.getZone(ZoneKey(you, Zone.GRAVEYARD)).contains(gy3) shouldBe true
        d.state.getZone(ZoneKey(you, Zone.BATTLEFIELD)).contains(small) shouldBe true
        d.state.projectedState.hasKeyword(gy2, Keyword.HASTE) shouldBe true
    }

    test("a target with mana value equal to X is rejected") {
        val d = setup()
        val you = d.activePlayer!!
        val welder = d.welder(you)
        val sac = d.putPermanentOnBattlefield(you, "Test MV2 Artifact")
        val gy2 = d.putCardInGraveyard(you, "Test MV2 Artifact")

        val result = d.submit(
            ActivateAbility(
                playerId = you, sourceId = welder, abilityId = abilityId,
                targets = listOf(ChosenTarget.Card(gy2, you, Zone.GRAVEYARD)),
                costPayment = AdditionalCostPayment(variableCostPermanents = listOf(sac)),
            )
        )
        result.error!! shouldContain "mana value less than X"
        d.state.getZone(ZoneKey(you, Zone.BATTLEFIELD)).contains(sac) shouldBe true
        d.isTapped(welder) shouldBe false
    }

    test("sacrificing two artifacts to sum X is rejected — the cost is one artifact") {
        val d = setup()
        val you = d.activePlayer!!
        val welder = d.welder(you)
        val a = d.putPermanentOnBattlefield(you, "Test MV1 Artifact")
        val b = d.putPermanentOnBattlefield(you, "Test MV2 Artifact")
        val gy2 = d.putCardInGraveyard(you, "Test MV2 Artifact")

        val result = d.submit(
            ActivateAbility(
                playerId = you, sourceId = welder, abilityId = abilityId,
                targets = listOf(ChosenTarget.Card(gy2, you, Zone.GRAVEYARD)),
                costPayment = AdditionalCostPayment(variableCostPermanents = listOf(a, b)),
            )
        )
        result.error!! shouldContain "at most 1"
        d.isTapped(welder) shouldBe false
        d.state.getZone(ZoneKey(you, Zone.BATTLEFIELD)).contains(a) shouldBe true
        d.state.getZone(ZoneKey(you, Zone.BATTLEFIELD)).contains(b) shouldBe true
        d.state.getZone(ZoneKey(you, Zone.GRAVEYARD)).contains(gy2) shouldBe true
    }

    test("with no artifact whose mana value exceeds a graveyard artifact's, the ability isn't offered") {
        val d = setup()
        val you = d.activePlayer!!
        val welder = d.welder(you)
        val sac = d.putPermanentOnBattlefield(you, "Test MV2 Artifact")
        d.putCardInGraveyard(you, "Test MV3 Artifact")

        d.legalActions(you).none { (it.action as? ActivateAbility)?.sourceId == welder } shouldBe true
        d.submit(ActivateAbility(playerId = you, sourceId = welder, abilityId = abilityId))
            .error!! shouldContain "leaves a legal target"
        d.state.getZone(ZoneKey(you, Zone.BATTLEFIELD)).contains(sac) shouldBe true
        d.state.getZone(ZoneKey(you, Zone.BATTLEFIELD)).contains(welder) shouldBe true
    }
})
