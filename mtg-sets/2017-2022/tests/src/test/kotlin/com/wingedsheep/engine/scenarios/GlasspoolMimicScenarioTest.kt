package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.atq.cards.MishrasFactory
import com.wingedsheep.mtg.sets.definitions.znr.cards.GlasspoolMimic
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Glasspool Mimic // Glasspool Shore (ZNR #60) — front: {2}{U} Creature — Shapeshifter Rogue 0/0.
 * "You may have this creature enter as a copy of a creature you control, except it's a Shapeshifter
 * Rogue in addition to its other types."
 *
 * The added subtypes apply only when the copy is a creature: copying a land animated by a non-copy
 * effect gives a noncreature copy that isn't a Shapeshifter Rogue (ruling).
 */
class GlasspoolMimicScenarioTest : FunSpec({

    fun driver() = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(GlasspoolMimic, MishrasFactory))
        initMirrorMatch(deck = Deck.of("Island" to 40), startingLife = 20)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun GameTestDriver.castMimic(p: EntityId, copy: EntityId?, offered: (List<EntityId>) -> Unit = {}): EntityId {
        val mimic = putCardInHand(p, "Glasspool Mimic")
        giveMana(p, Color.BLUE, 3)
        castSpell(p, mimic).error shouldBe null
        bothPass()
        val decision = pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        offered(decision.options)
        submitCardSelection(p, listOfNotNull(copy)).error shouldBe null
        return mimic
    }

    test("copies a creature you control and is a Shapeshifter Rogue in addition to its types") {
        val d = driver()
        val p = d.activePlayer!!
        val giant = d.putCreatureOnBattlefield(p, "Hill Giant")
        val theirs = d.putCreatureOnBattlefield(d.getOpponent(p), "Hill Giant")

        val mimic = d.castMimic(p, giant) { options ->
            options shouldContain giant
            withClue("only creatures you control are offered") { options shouldNotContain theirs }
        }

        val projected = d.state.projectedState
        d.state.getBattlefield() shouldContain mimic
        d.state.getEntity(mimic)!!.get<CardComponent>()!!.name shouldBe "Hill Giant"
        projected.getPower(mimic) shouldBe 3
        projected.getToughness(mimic) shouldBe 3
        projected.hasSubtype(mimic, "Giant") shouldBe true
        projected.hasSubtype(mimic, "Shapeshifter") shouldBe true
        projected.hasSubtype(mimic, "Rogue") shouldBe true
        withClue("the original is unchanged") { projected.hasSubtype(giant, "Rogue") shouldBe false }
    }

    test("copying a land animated by a non-copy effect gives a noncreature that isn't a Shapeshifter Rogue") {
        val d = driver()
        val p = d.activePlayer!!
        val factory = d.putLandOnBattlefield(p, "Mishra's Factory")
        d.giveColorlessMana(p, 1)
        d.submitSuccess(ActivateAbility(p, factory, MishrasFactory.activatedAbilities[1].id))
        d.bothPass()
        d.state.projectedState.isCreature(factory) shouldBe true

        val mimic = d.castMimic(p, factory)

        val projected = d.state.projectedState
        d.state.getBattlefield() shouldContain mimic
        projected.isCreature(mimic) shouldBe false
        projected.hasSubtype(mimic, "Shapeshifter") shouldBe false
        projected.hasSubtype(mimic, "Rogue") shouldBe false
    }

    test("declining the copy leaves a 0/0 that dies to state-based actions") {
        val d = driver()
        val p = d.activePlayer!!
        d.putCreatureOnBattlefield(p, "Hill Giant")

        val mimic = d.castMimic(p, null)

        d.state.getBattlefield() shouldNotContain mimic
        d.state.getGraveyard(p) shouldContain mimic
    }
})
