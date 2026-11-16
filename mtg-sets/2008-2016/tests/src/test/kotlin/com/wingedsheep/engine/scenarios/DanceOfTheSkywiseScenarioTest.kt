package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.TurnFaceUp
import com.wingedsheep.engine.handlers.effects.FaceDownTurnUp
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.state.components.identity.FaceDownModeComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.dtk.cards.DanceOfTheSkywise
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.FaceDownMode
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Dance of the Skywise — "Until end of turn, target creature you control becomes a blue Dragon
 * Illusion with base power and toughness 4/4, loses all abilities, and gains flying."
 *
 * Pins the layered result (types and colour replaced, other card types kept, flying survives the
 * ability loss), and the ruling that a face-down morph it targets can't be turned face up for its
 * morph cost.
 */
class DanceOfTheSkywiseScenarioTest : FunSpec({

    val vigilantGolem = card("Skywise Test Golem") {
        manaCost = "{3}"
        typeLine = "Artifact Creature — Golem"
        power = 2; toughness = 2
        keywords(Keyword.VIGILANCE)
    }
    val morphling = card("Skywise Test Morphling") {
        manaCost = "{3}{G}"
        typeLine = "Creature — Elf"
        power = 1; toughness = 1
        morph = "{G}"
    }
    val allCards = TestCards.all + listOf(DanceOfTheSkywise, vigilantGolem, morphling)

    fun driver() = GameTestDriver().also {
        it.registerCards(allCards)
        it.initMirrorMatch(Deck.of("Island" to 40), skipMulligans = true, startingPlayer = 0)
        it.passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun GameTestDriver.dance(target: EntityId) {
        val spell = putCardInHand(player1, DanceOfTheSkywise.name)
        giveMana(player1, Color.BLUE, 2)
        castSpell(player1, spell, listOf(target)).error shouldBe null
        bothPass().error shouldBe null
    }

    test("the creature becomes a blue 4/4 Dragon Illusion with flying and no other abilities") {
        val d = driver()
        val golem = d.putCreatureOnBattlefield(d.player1, vigilantGolem.name)
        d.dance(golem)

        val p = d.state.projectedState
        p.getPower(golem) shouldBe 4
        p.getToughness(golem) shouldBe 4
        p.getSubtypes(golem) shouldBe setOf("Dragon", "Illusion")
        p.getColors(golem) shouldBe setOf("BLUE")
        p.hasType(golem, "ARTIFACT") shouldBe true
        p.hasKeyword(golem, Keyword.FLYING) shouldBe true
        p.hasKeyword(golem, Keyword.VIGILANCE) shouldBe false
    }

    test("it only targets a creature you control") {
        val d = driver()
        val theirs = d.putCreatureOnBattlefield(d.player2, vigilantGolem.name)
        val spell = d.putCardInHand(d.player1, DanceOfTheSkywise.name)
        d.giveMana(d.player1, Color.BLUE, 2)
        d.castSpell(d.player1, spell, listOf(theirs)).error shouldNotBe null
    }

    test("the effect ends at end of turn") {
        val d = driver()
        val golem = d.putCreatureOnBattlefield(d.player1, vigilantGolem.name)
        d.dance(golem)
        d.passPriorityUntil(Step.UPKEEP, activePlayer = d.player2)

        val p = d.state.projectedState
        p.getPower(golem) shouldBe 2
        p.getSubtypes(golem) shouldBe setOf("Golem")
        p.hasKeyword(golem, Keyword.FLYING) shouldBe false
        p.hasKeyword(golem, Keyword.VIGILANCE) shouldBe true
    }

    test("a face-down morph it targets can't be turned face up for its morph cost") {
        val d = driver()
        val elf = d.putCreatureOnBattlefield(d.player1, morphling.name)
        d.replaceState(
            d.state.updateEntity(elf) { c ->
                c.with(FaceDownComponent)
                    .with(FaceDownModeComponent(FaceDownMode.MORPH))
                    .with(FaceDownTurnUp.dataFor(morphling, morphling.name, FaceDownMode.MORPH)!!)
            }
        )
        d.giveMana(d.player1, Color.GREEN, 1)
        d.legalActions(d.player1).filter { (it.action as? TurnFaceUp)?.sourceId == elf }.size shouldBe 1

        d.dance(elf)
        d.state.projectedState.getPower(elf) shouldBe 4
        d.state.projectedState.hasKeyword(elf, Keyword.FLYING) shouldBe true
        d.legalActions(d.player1).filter { (it.action as? TurnFaceUp)?.sourceId == elf }.shouldBeEmpty()
        d.giveMana(d.player1, Color.GREEN, 1)
        d.submit(TurnFaceUp(d.player1, elf)).error shouldNotBe null
        d.state.getEntity(elf)!!.has<FaceDownComponent>() shouldBe true
    }
})
