package com.wingedsheep.ai.engine

import com.wingedsheep.ai.audit.AiReadingAudit
import com.wingedsheep.ai.engine.knowledge.CardIntentAnalyzer
import com.wingedsheep.ai.engine.knowledge.IntentTag
import com.wingedsheep.ai.engine.knowledge.TargetPolarity
import com.wingedsheep.ai.engine.knowledge.TargetPolarityAnalyzer
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.hou.cards.ImaginaryThreats
import com.wingedsheep.mtg.sets.definitions.hou.cards.OminousSphinx
import com.wingedsheep.mtg.sets.definitions.hou.cards.SamutTheTested
import com.wingedsheep.mtg.sets.definitions.pcy.cards.ManaVapors
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Three hostile effects the AI used to leave unread:
 *
 *  - [com.wingedsheep.sdk.scripting.effects.SkipUntapEffect] on a target player — "lands target
 *    player controls don't untap during their next untap step" (Mana Vapors, Imaginary Threats) —
 *    is a tapper's lock: harmful to the player it names, [IntentTag.TAPPER].
 *  - [com.wingedsheep.sdk.scripting.effects.DividedDamageEffect] names no target of its own, but
 *    damages every one its requirement chose (Samut, the Tested's −2): harmful.
 *  - A cut to an opposing creature's power alone (Ominous Sphinx's "-2/-0 until end of turn")
 *    answers nothing but blunts it in the fight: [IntentTag.COMBAT_ENHANCEMENT].
 *
 * The decision it changes: with an unread slot the AI simulated both players for Mana Vapors, and
 * the simulation sees no difference until the next untap step — so it could lock its own lands.
 */
class SkipUntapDividedDamageReadingAiTest : FunSpec({

    val cards = TestCards.all + listOf(ManaVapors, ImaginaryThreats, SamutTheTested, OminousSphinx)
    val registry = CardRegistry().apply { register(cards) }

    test("skipping a target player's untap is harmful to that player and reads as a tapper") {
        TargetPolarityAnalyzer.forSpell(ManaVapors) shouldBe listOf(TargetPolarity.HARMFUL)
        TargetPolarityAnalyzer.forSpell(ImaginaryThreats) shouldBe listOf(TargetPolarity.HARMFUL)
        CardIntentAnalyzer.analyze(ManaVapors).tags shouldContain IntentTag.TAPPER
    }

    test("divided damage is harmful to the targets it is divided among") {
        val divide = SamutTheTested.script.activatedAbilities[1]
        TargetPolarityAnalyzer.forAbility(SamutTheTested, divide) shouldBe listOf(TargetPolarity.HARMFUL)
    }

    test("a power-only cut to an opposing creature reads as a combat effect") {
        CardIntentAnalyzer.analyze(OminousSphinx).tags shouldContain IntentTag.COMBAT_ENHANCEMENT
    }

    test("Ominous Sphinx and Samut, the Tested read fully") {
        for (card in listOf(OminousSphinx, SamutTheTested)) {
            val reading = AiReadingAudit.read(card)
            withClue("${card.name}: $reading") { reading.readFully shouldBe true }
        }
    }

    test("Mana Vapors locks the opponent's lands, not its own") {
        val d = GameTestDriver().apply {
            registerCards(cards)
            initMirrorMatch(deck = Deck.of("Island" to 40), skipMulligans = true, startingPlayer = 0)
            passPriorityUntil(Step.PRECOMBAT_MAIN)
        }
        repeat(2) { d.putLandOnBattlefield(d.player1, "Island") }
        repeat(4) { d.putLandOnBattlefield(d.player2, "Island") }
        val vapors = d.putCardInHand(d.player1, "Mana Vapors")
        val casts = d.legalActions(d.player1).filter { (it.action as? CastSpell)?.cardId == vapors }
        val chosen = AIPlayer.create(registry, d.player1, AiProfile.LIVE).chooseFrom(d.state, casts).action
        val target = chosen.shouldBeInstanceOf<CastSpell>().targets.single()
        (target as ChosenTarget.Player).playerId shouldBe d.player2
    }
})
