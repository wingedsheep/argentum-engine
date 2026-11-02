package com.wingedsheep.ai.engine

import com.wingedsheep.ai.engine.knowledge.TargetPolarity
import com.wingedsheep.ai.engine.knowledge.TargetPolarityAnalyzer
import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.TargetsResponse
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.ecl.cards.SyggWanderwineWisdom
import com.wingedsheep.mtg.sets.definitions.fra.cards.EmergencyPhytomedic
import com.wingedsheep.mtg.sets.definitions.fra.cards.HexhavenDuelingArena
import com.wingedsheep.mtg.sets.definitions.m10.cards.SignInBlood
import com.wingedsheep.mtg.sets.definitions.trk.cards.LwaxanaTroiCharmingGuest
import com.wingedsheep.mtg.sets.definitions.vis.cards.Inspiration
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Target slots whose effect the AI used to leave unread ([TargetPolarity.UNKNOWN]) and so aimed
 * with the removal ranking — at the opponent's best creature — from the 2026-10-31 AI-vs-humans
 * review: a granted "draw a card" trigger, "becomes prepared", "target player draws". And the
 * UNKNOWN default itself: the candidates a simulation compares always include our side.
 */
class UnreadTargetPolarityTest : FunSpec({

    val cards = TestCards.all + listOf(
        SyggWanderwineWisdom, HexhavenDuelingArena, EmergencyPhytomedic, Inspiration, SignInBlood,
    )
    val registry = CardRegistry().apply { register(cards) }
    val live = AiProfile.LIVE

    fun driver(): GameTestDriver = GameTestDriver().apply {
        registerCards(cards)
        initMirrorMatch(deck = Deck.of("Island" to 40), skipMulligans = true, startingPlayer = 0)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    context("polarity is read off the effect") {
        test("a granted card-draw trigger is a gift to the creature's controller") {
            val etb = SyggWanderwineWisdom.script.triggeredAbilities.first()
            TargetPolarityAnalyzer.forRequirements(etb.effect, listOfNotNull(etb.targetRequirement)) shouldBe
                listOf(TargetPolarity.BENEFICIAL)
        }
        test("becoming prepared is a gift") {
            val prepare = HexhavenDuelingArena.script.activatedAbilities.drop(1)
            prepare.map { TargetPolarityAnalyzer.forAbility(HexhavenDuelingArena, it) } shouldBe
                listOf(listOf(TargetPolarity.BENEFICIAL), listOf(TargetPolarity.BENEFICIAL))
        }
        test("a granted trigger doubler is a gift to the creature's controller") {
            val ability = LwaxanaTroiCharmingGuest.script.activatedAbilities.single()
            TargetPolarityAnalyzer.forAbility(LwaxanaTroiCharmingGuest, ability) shouldBe
                listOf(TargetPolarity.BENEFICIAL)
        }
        test("target player draws is a gift; draws and loses life is either way") {
            TargetPolarityAnalyzer.forSpell(Inspiration) shouldBe listOf(TargetPolarity.BENEFICIAL)
            TargetPolarityAnalyzer.forSpell(SignInBlood) shouldBe listOf(TargetPolarity.UNKNOWN)
        }
    }

    // Game 6d7c6893 turn 16: Sygg's enter trigger gave the opponent's Eddymurk Crab "whenever this
    // creature deals combat damage to a player, draw a card", with four creatures of our own.
    test("Sygg's granted draw trigger goes on our own creature, not theirs") {
        val d = driver()
        d.putCreatureOnBattlefield(d.player1, "Grizzly Bears")
        d.putCreatureOnBattlefield(d.player2, "Force of Nature")
        repeat(2) { d.putLandOnBattlefield(d.player1, "Island") }
        val sygg = d.putCardInHand(d.player1, "Sygg, Wanderwine Wisdom")
        d.castSpell(d.player1, sygg)
        d.bothPass()
        val decision = d.pendingDecision.shouldBeInstanceOf<ChooseTargetsDecision>()
        val response = AIPlayer.create(registry, d.player1, live).respondToDecision(d.state, decision)
        val target = response.shouldBeInstanceOf<TargetsResponse>().selectedTargets.values.flatten().single()
        d.state.projectedState.getController(target) shouldBe d.player1
    }

    // Game 290fcb53 turns 12, 14 and 17: Hexhaven Dueling Arena's "{4}, {T}: Target creature becomes
    // prepared" activated three times on the opponent's creatures, none of which has a prepare spell.
    test("Hexhaven Dueling Arena prepares our own creature, not theirs") {
        val d = driver()
        val phytomedic = d.putCreatureOnBattlefield(d.player1, "Emergency Phytomedic")
        d.putCreatureOnBattlefield(d.player2, "Force of Nature")
        val arena = d.putLandOnBattlefield(d.player1, "Hexhaven Dueling Arena")
        repeat(4) { d.putLandOnBattlefield(d.player1, "Island") }
        val prepareAny = HexhavenDuelingArena.script.activatedAbilities.last().id
        val activation = d.legalActions(d.player1).single {
            val a = it.action as? ActivateAbility
            a?.sourceId == arena && a.abilityId == prepareAny
        }
        val chosen = AIPlayer.create(registry, d.player1, live).chooseFrom(d.state, listOf(activation)).action
        val target = chosen.shouldBeInstanceOf<ActivateAbility>().targets.single()
        (target as ChosenTarget.Permanent).entityId shouldBe phytomedic
    }

    context("an unread slot's simulated candidates include both sides") {
        test("the best of each side is tried even when the cut would keep only theirs") {
            val d = driver()
            val theirs = List(3) { d.putCreatureOnBattlefield(d.player2, "Grizzly Bears") }
            val ours = d.putCreatureOnBattlefield(d.player1, "Grizzly Bears")
            val ranked = theirs + ours
            fun cut(limit: Int, polarity: TargetPolarity?): List<EntityId> =
                TargetSelection.simulationCandidates(d.state, ranked, limit, d.player1, polarity)

            cut(2, TargetPolarity.UNKNOWN) shouldBe listOf(theirs[0], ours)
            cut(1, null) shouldBe listOf(theirs[0], ours)
            cut(8, TargetPolarity.UNKNOWN) shouldBe listOf(theirs[0], ours, theirs[1], theirs[2])
            // A read slot keeps the plain cut: its pool is already the right side.
            cut(2, TargetPolarity.HARMFUL) shouldBe theirs.take(2)
        }
    }
})
