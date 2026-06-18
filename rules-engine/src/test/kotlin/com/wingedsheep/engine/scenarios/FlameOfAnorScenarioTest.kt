package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.ltr.cards.FlameOfAnor
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Flame of Anor — "Choose one. If you control a Wizard as you cast this spell, you may
 * choose two instead." Pins the conditional modal count: the dynamic cap is 2 when you
 * control a Wizard at cast time, otherwise 1, with a mandatory floor of one mode.
 *
 * Modes: 0 = target player draws two cards, 1 = destroy target artifact,
 *        2 = Flame of Anor deals 5 damage to target creature.
 */
class FlameOfAnorScenarioTest : FunSpec({

    val WizardCreature: CardDefinition = CardDefinition.creature(
        name = "Test Wizard",
        manaCost = ManaCost.parse("{U}"),
        subtypes = setOf(Subtype("Human"), Subtype("Wizard")),
        power = 1,
        toughness = 1
    )

    fun driver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all)
        d.registerCard(FlameOfAnor)
        d.registerCard(WizardCreature)
        return d
    }

    fun castSetup(d: GameTestDriver): Pair<com.wingedsheep.sdk.model.EntityId, com.wingedsheep.sdk.model.EntityId> {
        d.initMirrorMatch(deck = Deck.of("Island" to 20, "Mountain" to 20), startingLife = 20)
        val p1 = d.activePlayer!!
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        d.giveColorlessMana(p1, 1)
        d.giveMana(p1, Color.BLUE, 1)
        d.giveMana(p1, Color.RED, 1)
        val opponent = d.getOpponent(p1)
        return p1 to opponent
    }

    test("no Wizard — choosing two modes is illegal (effective max is one)") {
        val d = driver()
        val (p1, p2) = castSetup(d)

        // Opponent has a creature so the damage mode has a legal target.
        d.putCreatureOnBattlefield(p2, "Goblin Guide")
        val goblin = d.getCreatures(p2).first()

        val spell = d.putCardInHand(p1, "Flame of Anor")
        val result = d.submit(CastSpell(
            playerId = p1,
            cardId = spell,
            targets = listOf(ChosenTarget.Player(p1), ChosenTarget.Permanent(goblin)),
            chosenModes = listOf(0, 2),
            modeTargetsOrdered = listOf(
                listOf(ChosenTarget.Player(p1)),
                listOf(ChosenTarget.Permanent(goblin))
            )
        ))

        result.isSuccess shouldBe false
    }

    test("no Wizard — choosing exactly one mode resolves (deal 5 to a creature)") {
        val d = driver()
        val (p1, p2) = castSetup(d)

        d.putCreatureOnBattlefield(p2, "Centaur Courser") // 3/3
        val centaur = d.getCreatures(p2).first()

        val spell = d.putCardInHand(p1, "Flame of Anor")
        val result = d.submit(CastSpell(
            playerId = p1,
            cardId = spell,
            targets = listOf(ChosenTarget.Permanent(centaur)),
            chosenModes = listOf(2),
            modeTargetsOrdered = listOf(listOf(ChosenTarget.Permanent(centaur)))
        ))
        if (!result.isSuccess) throw AssertionError("cast failed: ${result.error}")

        d.bothPass()
        // Centaur (3/3) took 5 damage → destroyed.
        d.findPermanent(p2, "Centaur Courser").shouldBeNull()
    }

    test("control a Wizard — may choose two: draw two AND deal 5 to a creature") {
        val d = driver()
        val (p1, p2) = castSetup(d)

        d.putCreatureOnBattlefield(p1, "Test Wizard")
        d.putCreatureOnBattlefield(p2, "Centaur Courser")
        val centaur = d.getCreatures(p2).first { d.state.getEntity(it)?.get<CardComponent>()?.name == "Centaur Courser" }

        val handBefore = d.getHand(p1).size

        val spell = d.putCardInHand(p1, "Flame of Anor")
        val result = d.submit(CastSpell(
            playerId = p1,
            cardId = spell,
            targets = listOf(ChosenTarget.Player(p1), ChosenTarget.Permanent(centaur)),
            chosenModes = listOf(0, 2),
            modeTargetsOrdered = listOf(
                listOf(ChosenTarget.Player(p1)),
                listOf(ChosenTarget.Permanent(centaur))
            )
        ))
        if (!result.isSuccess) throw AssertionError("cast failed: ${result.error}")

        d.bothPass()

        // Mode 0: P1 drew two cards. Hand delta from handBefore (captured before adding
        // Flame): +1 (put Flame in hand) - 1 (cast Flame) + 2 (draw) = +2.
        d.getHand(p1).size shouldBe (handBefore + 2)
        // Mode 2: Centaur (3/3) took 5 damage → destroyed.
        d.findPermanent(p2, "Centaur Courser").shouldBeNull()
    }

    test("control a Wizard — cast-time picker offers a second mode pick") {
        val d = driver()
        val (p1, _) = castSetup(d)
        d.putCreatureOnBattlefield(p1, "Test Wizard")

        val spell = d.putCardInHand(p1, "Flame of Anor")
        d.submit(CastSpell(playerId = p1, cardId = spell))

        // First mode pick — minChooseCount = 1, so "Done" must NOT be offered yet.
        val first = d.pendingDecision.shouldBeInstanceOf<ChooseOptionDecision>()
        first.options shouldNotContain "Done"
        first.options shouldContain "Target player draws two cards"

        // Pick mode 0; with a Wizard the effective chooseCount is 2, so a second pick
        // (with "Done" now available) must be presented rather than going straight to targets.
        d.submitDecision(p1, OptionChosenResponse(first.id, 0))
        val second = d.pendingDecision.shouldBeInstanceOf<ChooseOptionDecision>()
        second.options shouldContain "Done"
    }

    test("no Wizard — cast-time picker resolves after a single mandatory pick") {
        val d = driver()
        val (p1, _) = castSetup(d)

        val spell = d.putCardInHand(p1, "Flame of Anor")
        d.submit(CastSpell(playerId = p1, cardId = spell))

        // With no Wizard the effective chooseCount is 1; the first decision is the only
        // mode pick and must not offer "Done".
        val first = d.pendingDecision.shouldBeInstanceOf<ChooseOptionDecision>()
        first.options shouldNotContain "Done"

        // Picking the draw mode (mode 0) transitions straight to target selection — no
        // second mode pick is offered.
        d.submitDecision(p1, OptionChosenResponse(first.id, 0))
        val next = d.pendingDecision
        next.shouldNotBeNull()
        // The next decision is not another mode-selection ChooseOptionDecision with a
        // "Done" option (it is the player-target selection for the draw mode).
        if (next is ChooseOptionDecision) {
            next.options shouldNotContain "Done"
        }
    }
})
