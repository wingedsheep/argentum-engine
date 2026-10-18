package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PlayLand
import com.wingedsheep.engine.mechanics.HandObjectRestrictions
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.engine.view.Visibility
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.RedirectZoneChangeWith
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Card-scoped hand restrictions — a card-targeted `CantPlayCardsFromHand` ("… can't play it") and
 * `RevealCardInHand` ("plays with that card revealed in their hand"), with
 * [Duration.UntilAffectedOwnersNextTurn], as Firestorm Phoenix's die-to-hand replacement rider uses
 * them.
 *
 * Pins down:
 *  - the card can't be cast (or played as a land) while locked — neither offered nor accepted;
 *  - the lock outlasts an intervening opponent's turn and ends as its owner's next turn begins;
 *  - the window keys on the card's **owner** ("that player"), not the replacement's controller;
 *  - the lock and reveal belong to that hand object: once the card leaves the hand it is a new
 *    object (CR 400.7) and neither follows it back;
 *  - the reveal is visible to the opponent and isn't forgotten when a same-named card is cast,
 *    while a *different* copy of the card stays playable (the card's 2009-10-01 ruling).
 */
class HandObjectRestrictionsTest : FunSpec({

    fun rebirth() = RedirectZoneChangeWith(
        newDestination = Zone.HAND,
        additionalEffect = Effects.RevealCardInHand(EffectTarget.Self, Duration.UntilAffectedOwnersNextTurn) then
            Effects.CantPlayCardsFromHand(EffectTarget.Self, Duration.UntilAffectedOwnersNextTurn),
        selfOnly = true,
        appliesTo = EventPattern.ZoneChangeEvent(filter = GameObjectFilter.Any, from = Zone.BATTLEFIELD, to = Zone.GRAVEYARD)
    )

    // Flash, so "can it be cast right now?" is a real question on the opponent's turn too.
    val bird = card("Test Rebirth Bird") {
        manaCost = "{R}"
        typeLine = "Creature — Phoenix"
        power = 3
        toughness = 2
        keywords(Keyword.FLASH)
        replacementEffect(rebirth())
    }
    val morphBird = card("Test Rebirth Morph") {
        manaCost = "{R}"
        typeLine = "Creature — Phoenix"
        power = 3
        toughness = 2
        morph = "{R}"
        replacementEffect(rebirth())
    }
    val dryad = card("Test Rebirth Dryad") {
        typeLine = "Land Creature — Forest Dryad"
        power = 1
        toughness = 1
        replacementEffect(rebirth())
    }
    val doom = card("Test Doom") {
        manaCost = "{0}"
        typeLine = "Instant"
        spell {
            val t = target(TargetFilter.Creature)
            effect = Effects.Destroy(t)
        }
    }
    val steal = card("Test Steal") {
        manaCost = "{0}"
        typeLine = "Instant"
        spell {
            val t = target(TargetFilter.Creature)
            effect = Effects.GainControl(t)
        }
    }
    val recall = card("Test Recall") {
        manaCost = "{0}"
        typeLine = "Instant"
        spell {
            val t = target(TargetFilter.CreatureInYourGraveyard)
            effect = Effects.ReturnToHand(t)
        }
    }

    val cards = TestCards.all + listOf(bird, morphBird, dryad, doom, steal, recall)

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(cards)
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40))
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN, driver.player1)
        return driver
    }

    fun GameTestDriver.castAndResolve(player: EntityId, name: String, target: EntityId) {
        castSpell(player, putCardInHand(player, name), listOf(target)).error shouldBe null
        var guard = 0
        while (state.stack.isNotEmpty()) {
            check(guard++ < 10) { "stack didn't resolve: pending ${state.pendingDecision}" }
            bothPass()
        }
    }

    fun GameTestDriver.castOffers(player: EntityId, cardId: EntityId) =
        legalActions(player).filter { (it.action as? CastSpell)?.cardId == cardId }

    fun GameTestDriver.landOffers(player: EntityId, cardId: EntityId) =
        legalActions(player).filter { (it.action as? PlayLand)?.cardId == cardId }

    fun GameTestDriver.visibleTo(cardId: EntityId, viewer: EntityId) =
        Visibility(cardRegistry, conditionEvaluator = services.predicateEvaluator.conditions)
            .isCardIdentityVisibleTo(state, Zone.HAND, cardId, viewer)

    test("dies -> owner's hand, revealed and locked through the opponent's turn, castable on the owner's next turn") {
        val d = createDriver()
        val p1 = d.player1
        val p2 = d.player2
        val phoenix = d.putCreatureOnBattlefield(p1, bird.name)

        d.castAndResolve(p1, doom.name, phoenix)
        d.getHand(p1) shouldContain phoenix
        withClue("revealed to the opponent") { d.visibleTo(phoenix, p2) shouldBe true }
        HandObjectRestrictions.isCantBePlayed(d.state, phoenix) shouldBe true

        d.giveMana(p1, Color.RED, 1)
        withClue("not offered while locked") { d.castOffers(p1, phoenix) shouldBe emptyList() }
        withClue("and rejected if submitted anyway") { d.castSpell(p1, phoenix).error shouldNotBe null }

        d.passPriorityUntil(Step.PRECOMBAT_MAIN, p2)
        d.passPriority(p2)
        d.state.priorityPlayerId shouldBe p1
        d.giveMana(p1, Color.RED, 1)
        withClue("still locked during the opponent's intervening turn (flash would allow a cast)") {
            d.castOffers(p1, phoenix) shouldBe emptyList()
            d.castSpell(p1, phoenix).error shouldNotBe null
            d.visibleTo(phoenix, p2) shouldBe true
        }

        d.passPriorityUntil(Step.UPKEEP, p1)
        withClue("the window closes as the owner's next turn begins") {
            HandObjectRestrictions.isCantBePlayed(d.state, phoenix) shouldBe false
            HandObjectRestrictions.isPinnedRevealed(d.state, phoenix) shouldBe false
        }
        d.giveMana(p1, Color.RED, 1)
        d.castOffers(p1, phoenix).isNotEmpty() shouldBe true
        d.castSpell(p1, phoenix).error shouldBe null
    }

    test("can't play it covers playing it as a land") {
        val d = createDriver()
        val p1 = d.player1
        val land = d.putCreatureOnBattlefield(p1, dryad.name)

        d.castAndResolve(p1, doom.name, land)
        d.getHand(p1) shouldContain land
        withClue("no land play offered for the locked card") { d.landOffers(p1, land) shouldBe emptyList() }
        withClue("and the handler rejects it") { d.playLand(p1, land).error shouldNotBe null }

        d.passPriorityUntil(Step.PRECOMBAT_MAIN, d.player2)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN, p1)
        d.landOffers(p1, land).isNotEmpty() shouldBe true
        d.playLand(p1, land).error shouldBe null
    }

    test("can't play it covers casting it face down") {
        val d = createDriver()
        val p1 = d.player1
        val morph = d.putCreatureOnBattlefield(p1, morphBird.name)

        d.castAndResolve(p1, doom.name, morph)
        d.getHand(p1) shouldContain morph
        d.giveMana(p1, Color.RED, 3)
        withClue("no face-down cast offered for the locked card") {
            d.castOffers(p1, morph).filter { (it.action as CastSpell).castFaceDown } shouldBe emptyList()
        }
        withClue("and a submitted face-down cast is rejected") {
            d.submit(CastSpell(p1, morph, castFaceDown = true)).error shouldNotBe null
        }
    }

    test("the lock and reveal belong to that hand object: gone once the card leaves the hand and comes back") {
        val d = createDriver()
        val p1 = d.player1
        val phoenix = d.putCreatureOnBattlefield(p1, bird.name)

        d.castAndResolve(p1, doom.name, phoenix)
        HandObjectRestrictions.isCantBePlayed(d.state, phoenix) shouldBe true

        // Discarded, then returned to hand: a new object each time (CR 400.7).
        d.moveToGraveyard(phoenix)
        d.castSpellWithTargets(
            p1, d.putCardInHand(p1, recall.name), listOf(ChosenTarget.Card(phoenix, p1, Zone.GRAVEYARD))
        ).error shouldBe null
        d.bothPass()
        d.getHand(p1) shouldContain phoenix
        HandObjectRestrictions.isCantBePlayed(d.state, phoenix) shouldBe false
        HandObjectRestrictions.isPinnedRevealed(d.state, phoenix) shouldBe false
        d.giveMana(p1, Color.RED, 1)
        d.castOffers(p1, phoenix).isNotEmpty() shouldBe true
        d.castSpell(p1, phoenix).error shouldBe null
    }

    test("a different copy stays playable, and casting it doesn't make the opponent forget the revealed one") {
        val d = createDriver()
        val p1 = d.player1
        val p2 = d.player2
        val phoenix = d.putCreatureOnBattlefield(p1, bird.name)
        d.castAndResolve(p1, doom.name, phoenix)
        val other = d.putCardInHand(p1, bird.name)

        d.giveMana(p1, Color.RED, 1)
        d.castOffers(p1, other).isNotEmpty() shouldBe true
        d.castSpell(p1, other).error shouldBe null
        withClue("a same-named cast would normally make the known copy ambiguous; a card played revealed stays shown") {
            d.visibleTo(phoenix, p2) shouldBe true
        }
        HandObjectRestrictions.isCantBePlayed(d.state, phoenix) shouldBe true
    }

    test("the window keys on the card's owner, not on who controlled the creature") {
        val d = createDriver()
        val p1 = d.player1
        val p2 = d.player2
        // Owned by p2, stolen and then killed by p1: it returns to p2's hand.
        val phoenix = d.putCreatureOnBattlefield(p2, bird.name)
        d.castAndResolve(p1, steal.name, phoenix)
        d.state.projectedState.getController(phoenix) shouldBe p1
        d.castAndResolve(p1, doom.name, phoenix)
        d.getHand(p2) shouldContain phoenix
        withClue("revealed to the player who isn't its owner") { d.visibleTo(phoenix, p1) shouldBe true }

        d.passPriority(p1)
        d.state.priorityPlayerId shouldBe p2
        d.giveMana(p2, Color.RED, 1)
        withClue("locked for the rest of this turn") { d.castOffers(p2, phoenix) shouldBe emptyList() }

        d.passPriorityUntil(Step.UPKEEP, p2)
        withClue("ends as the owner's (p2's) next turn begins, not the former controller's") {
            HandObjectRestrictions.isCantBePlayed(d.state, phoenix) shouldBe false
        }
        d.giveMana(p2, Color.RED, 1)
        d.castOffers(p2, phoenix).isNotEmpty() shouldBe true
    }
})
