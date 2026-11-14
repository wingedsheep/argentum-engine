package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.CopyOfComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.state.components.stack.SpellOnStackComponent
import com.wingedsheep.engine.state.components.stack.TargetsComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.ori.cards.PsychicRebuttal
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Psychic Rebuttal (ORI #67) — "Counter target instant or sorcery spell that targets you.
 * Spell mastery — If there are two or more instant and/or sorcery cards in your graveyard, you may
 * copy the spell countered this way. You may choose new targets for the copy."
 *
 * Player 2 (active) bolts player 1; player 1 answers with Psychic Rebuttal.
 */
class PsychicRebuttalScenarioTest : FunSpec({

    val unstoppableBolt = TestCards.LightningBolt.copy(
        name = "Unstoppable Bolt",
        script = TestCards.LightningBolt.script.copy(cantBeCountered = true)
    )

    fun driver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + PsychicRebuttal + unstoppableBolt)
        d.initMirrorMatch(deck = Deck.of("Island" to 40), skipMulligans = true, startingPlayer = 1)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return d
    }

    fun GameTestDriver.spellMastery() {
        putCardInGraveyard(player1, "Lightning Bolt")
        putCardInGraveyard(player1, "Lightning Bolt")
    }

    /** Player 2 casts [name] at [target] and passes; returns the spell on the stack. */
    fun GameTestDriver.opponentCasts(name: String, target: ChosenTarget): EntityId {
        giveMana(player2, Color.RED, 1)
        val spell = putCardInHand(player2, name)
        castSpellWithTargets(player2, spell, listOf(target)).error shouldBe null
        passPriority(player2)
        return spell
    }

    fun GameTestDriver.rebut(spell: EntityId) {
        giveMana(player1, Color.BLUE, 2)
        val rebuttal = putCardInHand(player1, "Psychic Rebuttal")
        castSpellWithTargets(player1, rebuttal, listOf(ChosenTarget.Spell(spell))).error shouldBe null
        bothPass()
    }

    fun GameTestDriver.copiesOnStack() = state.stack.filter { id ->
        state.getEntity(id)?.let { it.has<SpellOnStackComponent>() && it.has<CopyOfComponent>() } == true
    }

    test("counters a spell that targets you; without spell mastery there is no copy offer") {
        val d = driver()
        val bolt = d.opponentCasts("Lightning Bolt", ChosenTarget.Player(d.player1))
        d.rebut(bolt)

        withClue("no may-copy prompt without spell mastery") { d.pendingDecision shouldBe null }
        d.state.getZone(ZoneKey(d.player2, Zone.GRAVEYARD)) shouldContain bolt
        d.copiesOnStack().size shouldBe 0
        d.getLifeTotal(d.player1) shouldBe 20
    }

    test("with spell mastery you may copy the countered spell and choose new targets for it") {
        val d = driver()
        d.spellMastery()
        val bolt = d.opponentCasts("Lightning Bolt", ChosenTarget.Player(d.player1))
        d.rebut(bolt)

        d.pendingDecision.shouldBeInstanceOf<YesNoDecision>().playerId shouldBe d.player1
        d.submitYesNo(d.player1, true).error shouldBe null

        d.pendingDecision.shouldBeInstanceOf<ChooseTargetsDecision>()
        d.submitTargetSelection(d.player1, listOf(d.player2)).error shouldBe null

        withClue("the original was countered into its owner's graveyard") {
            d.state.getZone(ZoneKey(d.player2, Zone.GRAVEYARD)) shouldContain bolt
        }
        val copy = d.copiesOnStack().single()
        withClue("you control the copy") {
            d.state.getEntity(copy)!!.get<SpellOnStackComponent>()!!.casterId shouldBe d.player1
        }
        d.state.getEntity(copy)!!.get<TargetsComponent>()!!.targets shouldBe listOf(ChosenTarget.Player(d.player2))

        d.bothPass()
        d.getLifeTotal(d.player2) shouldBe 17
        d.getLifeTotal(d.player1) shouldBe 20
    }

    test("one instant or sorcery in your graveyard is not spell mastery; Psychic Rebuttal doesn't count itself") {
        val d = driver()
        d.putCardInGraveyard(d.player1, "Lightning Bolt")
        val bolt = d.opponentCasts("Lightning Bolt", ChosenTarget.Player(d.player1))
        d.rebut(bolt)

        withClue("no copy offer with only one instant in the graveyard while Rebuttal resolves") {
            d.pendingDecision shouldBe null
        }
        d.copiesOnStack().size shouldBe 0
        d.getLifeTotal(d.player1) shouldBe 20
    }

    test("declining the copy just counters the spell") {
        val d = driver()
        d.spellMastery()
        val bolt = d.opponentCasts("Lightning Bolt", ChosenTarget.Player(d.player1))
        d.rebut(bolt)

        d.submitYesNo(d.player1, false).error shouldBe null
        d.copiesOnStack().size shouldBe 0
        d.state.stack.size shouldBe 0
        d.getLifeTotal(d.player1) shouldBe 20
        d.getLifeTotal(d.player2) shouldBe 20
    }

    test("a spell that can't be countered is not copied, even with spell mastery (ruling)") {
        val d = driver()
        d.spellMastery()
        val bolt = d.opponentCasts("Unstoppable Bolt", ChosenTarget.Player(d.player1))
        d.rebut(bolt)

        withClue("no copy offer: the spell wasn't countered this way") { d.pendingDecision shouldBe null }
        d.copiesOnStack().size shouldBe 0
        d.state.stack shouldBe listOf(bolt)

        d.bothPass()
        d.getLifeTotal(d.player1) shouldBe 17
    }

    test("can't target a spell that doesn't target you") {
        val d = driver()
        val bolt = d.opponentCasts("Lightning Bolt", ChosenTarget.Player(d.player2))
        d.giveMana(d.player1, Color.BLUE, 2)
        val rebuttal = d.putCardInHand(d.player1, "Psychic Rebuttal")
        d.castSpellWithTargets(d.player1, rebuttal, listOf(ChosenTarget.Spell(bolt))).error shouldNotBe null
    }
})
