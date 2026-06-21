package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CardsSelectedResponse
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Hauntwoods Shrieker (DSK #182) — {1}{G}{G} Creature — Beast Mutant 3/3.
 *
 * "Whenever this creature attacks, manifest dread.
 *  {1}{G}: Reveal target face-down permanent. If it's a creature card, you may turn it face up."
 *
 * Exercises:
 *  - the attack trigger reusing the shared manifest-dread recipe;
 *  - the activated ability revealing a face-down permanent (emits a reveal);
 *  - the "if it's a creature card" gate reading the *underlying* card (not the face-down 2/2
 *    projection), so a manifested creature offers an optional flip and a manifested land does not;
 *  - the optional ("you may") nature of the flip.
 */
class HauntwoodsShriekerScenarioTest : FunSpec({

    fun driver(): GameTestDriver = GameTestDriver().apply {
        registerCards(TestCards.all)
        initMirrorMatch(deck = Deck.of("Forest" to 40), skipMulligans = true, startingPlayer = 0)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    /** Cast Manifest Dread from hand and pause on the pick. */
    fun GameTestDriver.castManifestDread(you: EntityId) {
        val spell = putCardInHand(you, "Manifest Dread")
        giveMana(you, Color.GREEN, 2)
        castSpell(you, spell)
        while (!isPaused && state.stack.isNotEmpty()) bothPass()
    }

    fun GameTestDriver.activateReveal(me: EntityId, shrieker: EntityId, faceDown: EntityId) {
        val abilityId = cardRegistry.requireCard("Hauntwoods Shrieker").activatedAbilities[0].id
        giveMana(me, Color.GREEN, 2)
        submit(
            ActivateAbility(
                playerId = me,
                sourceId = shrieker,
                abilityId = abilityId,
                targets = listOf(ChosenTarget.Permanent(faceDown)),
            )
        )
        while (!isPaused && state.stack.isNotEmpty()) bothPass()
    }

    test("attacking manifests dread (creates a face-down 2/2)") {
        val d = driver()
        val me = d.player1

        val shrieker = d.putCreatureOnBattlefield(me, "Hauntwoods Shrieker")
        d.removeSummoningSickness(shrieker)
        d.putCardOnTopOfLibrary(me, "Forest")
        d.putCardOnTopOfLibrary(me, "Centaur Courser")

        val faceDownBefore = d.getCreatures(me).count {
            d.state.getEntity(it)?.has<FaceDownComponent>() == true
        }

        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(me, listOf(shrieker), d.player2)

        // Resolve the manifest-dread trigger: pick the creature to manifest.
        var guard = 0
        while (guard++ < 30) {
            val pd = d.pendingDecision
            if (pd is SelectCardsDecision) {
                d.submitDecision(me, CardsSelectedResponse(pd.id, listOf(pd.options.first())))
            } else if (d.state.stack.isNotEmpty()) {
                d.bothPass()
            } else break
        }

        val faceDownAfter = d.getCreatures(me).count {
            d.state.getEntity(it)?.has<FaceDownComponent>() == true
        }
        faceDownAfter shouldBe faceDownBefore + 1
    }

    test("reveal a face-down creature card: may turn it face up") {
        val d = driver()
        val me = d.player1

        val shrieker = d.putCreatureOnBattlefield(me, "Hauntwoods Shrieker")
        d.removeSummoningSickness(shrieker)

        // Manifest a creature face down.
        d.putCardOnTopOfLibrary(me, "Forest")
        val creature = d.putCardOnTopOfLibrary(me, "Centaur Courser")
        d.castManifestDread(me)
        val pick = d.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        d.submitDecision(me, CardsSelectedResponse(pick.id, listOf(creature)))
        d.state.getEntity(creature)?.has<FaceDownComponent>() shouldBe true

        // Activate {1}{G}: reveal it. It's a creature card, so we get a "may turn face up" prompt.
        d.activateReveal(me, shrieker, creature)

        d.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
        d.submitYesNo(me, true)
        while (d.state.stack.isNotEmpty()) d.bothPass()

        // Turned face up — it's the real Centaur Courser now.
        d.state.getEntity(creature)?.has<FaceDownComponent>() shouldBe false
        d.getCardName(creature) shouldBe "Centaur Courser"
        d.state.projectedState.getPower(creature) shouldBe 3
    }

    test("declining the may-flip leaves the creature face down (but revealed)") {
        val d = driver()
        val me = d.player1

        val shrieker = d.putCreatureOnBattlefield(me, "Hauntwoods Shrieker")
        d.removeSummoningSickness(shrieker)

        d.putCardOnTopOfLibrary(me, "Forest")
        val creature = d.putCardOnTopOfLibrary(me, "Centaur Courser")
        d.castManifestDread(me)
        val pick = d.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        d.submitDecision(me, CardsSelectedResponse(pick.id, listOf(creature)))

        d.activateReveal(me, shrieker, creature)
        d.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
        d.submitYesNo(me, false)
        while (d.state.stack.isNotEmpty()) d.bothPass()

        // Declined: stays face down.
        d.state.getEntity(creature)?.has<FaceDownComponent>() shouldBe true
    }

    test("reveal a face-down land: revealed, no flip offered, stays face down") {
        val d = driver()
        val me = d.player1

        val shrieker = d.putCreatureOnBattlefield(me, "Hauntwoods Shrieker")
        d.removeSummoningSickness(shrieker)

        // Manifest a land face down (the underlying card is NOT a creature card).
        val creature = d.putCardOnTopOfLibrary(me, "Centaur Courser")
        val land = d.putCardOnTopOfLibrary(me, "Forest")
        d.castManifestDread(me)
        val pick = d.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        d.submitDecision(me, CardsSelectedResponse(pick.id, listOf(land)))
        d.state.getEntity(land)?.has<FaceDownComponent>() shouldBe true

        val abilityId = d.cardRegistry.requireCard("Hauntwoods Shrieker").activatedAbilities[0].id
        d.giveMana(me, Color.GREEN, 2)
        d.submit(
            ActivateAbility(
                playerId = me,
                sourceId = shrieker,
                abilityId = abilityId,
                targets = listOf(ChosenTarget.Permanent(land)),
            )
        )
        var guard = 0
        while (d.state.stack.isNotEmpty() && d.pendingDecision == null && guard++ < 20) d.bothPass()

        // The reveal happened, but no flip prompt and it stays face down — "if it's a creature
        // card" is false for a land card.
        d.pendingDecision shouldBe null
        d.state.getEntity(land)?.has<FaceDownComponent>() shouldBe true
    }
})
