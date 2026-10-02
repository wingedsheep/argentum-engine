package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.AlternativeCostType
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.core.TargetsResponse
import com.wingedsheep.engine.mechanics.PrototypedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.CardType
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CreatureStats
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/** Prototype's cast choice, characteristics, and zone boundaries (CR 702.160, 718). */
class PrototypeTest : FunSpec({
    val proto = card("Prototype Test Golem") {
        manaCost = "{6}"
        typeLine = "Artifact Creature — Golem"
        power = 5
        toughness = 5
        keywordAbility(KeywordAbility.prototype("{1}{R}", 2, 1))
        keywordAbility(KeywordAbility.Affinity(CardType.ARTIFACT))
        triggeredAbility {
            trigger = Triggers.self.enters()
            effect = Effects.GainLife(1)
        }
    }
    val bestowProto = card("Prototype Test Bestow Golem") {
        manaCost = "{6}"
        typeLine = "Enchantment Artifact Creature — Golem"
        power = 5
        toughness = 5
        keywordAbility(KeywordAbility.bestow("{1}{W}"))
        keywordAbility(KeywordAbility.prototype("{1}{R}", 2, 1))
    }
    val rock = card("Prototype Test Rock") {
        manaCost = "{0}"
        typeLine = "Artifact"
    }
    val kill = card("Prototype Test Kill") {
        manaCost = "{0}"
        typeLine = "Instant"
        spell { val t = target(TargetFilter.Creature); effect = Effects.Move(t, Zone.GRAVEYARD) }
    }
    val bounce = card("Prototype Test Bounce") {
        manaCost = "{0}"
        typeLine = "Instant"
        spell { val t = target(TargetFilter.Creature); effect = Effects.Move(t, Zone.HAND) }
    }
    val spellCopy = card("Prototype Test Spell Copy") {
        manaCost = "{0}"
        typeLine = "Instant"
        spell { val t = target(TargetFilter.SpellOnStack); effect = Effects.CopyTargetSpell(t) }
    }
    val permanentCopy = card("Prototype Test Permanent Copy") {
        manaCost = "{0}"
        typeLine = "Instant"
        spell { val t = target(TargetFilter.Permanent); effect = Effects.CreateTokenCopyOfTarget(t) }
    }
    val remand = card("Prototype Test Remand") {
        manaCost = "{0}"
        typeLine = "Instant"
        spell {
            target(TargetFilter.SpellOnStack)
            effect = Effects.ReturnSpellToOwnersHand()
        }
    }
    val counter = card("Prototype Test Counter") {
        manaCost = "{0}"
        typeLine = "Instant"
        spell {
            target(TargetFilter.SpellOnStack)
            effect = Effects.CounterSpell()
        }
    }

    fun driver(): GameTestDriver = GameTestDriver().also {
        it.registerCards(TestCards.all + listOf(proto, bestowProto, rock, kill, bounce, spellCopy, permanentCopy, remand, counter))
        it.initMirrorMatch(deck = Deck.of("Mountain" to 40))
        it.passPriorityUntil(Step.PRECOMBAT_MAIN)
    }
    fun GameTestDriver.castPrototyped(cardId: EntityId) = submit(CastSpell(
        playerId = activePlayer!!, cardId = cardId, castPrototyped = true, paymentStrategy = PaymentStrategy.FromPool
    ))
    fun GameTestDriver.resolveAll() {
        repeat(20) {
            val decision = state.pendingDecision
            if (decision is ChooseTargetsDecision) {
                submitDecision(decision.playerId, TargetsResponse(decision.id,
                    decision.legalTargets.mapValues { (_, targets) -> targets.take(1) })).error shouldBe null
            } else if (decision != null) autoResolveDecision()
            else if (stackSize > 0) bothPass()
            else return
        }
        error("Prototype test did not finish resolving")
    }
    fun GameTestDriver.castFree(card: String, target: ChosenTarget) {
        val player = activePlayer!!
        castSpellWithTargets(player, putCardInHand(player, card), listOf(target)).outcome shouldBe Outcome.Done
    }
    fun GameTestDriver.card(id: EntityId) = state.getEntity(id)!!.get<CardComponent>()!!
    fun CardComponent.isPrinted() {
        manaValue shouldBe 6
        colors shouldBe emptySet()
        baseStats shouldBe CreatureStats(5, 5)
    }

    test("a normal cast uses the printed cost, color and size") {
        val game = driver()
        val player = game.activePlayer!!
        val golem = game.putCardInHand(player, proto.name)
        game.giveColorlessMana(player, 6)
        game.castSpell(player, golem).outcome shouldBe Outcome.Done
        game.resolveAll()
        val permanent = game.findPermanent(player, proto.name)!!
        game.state.projectedState.getPower(permanent) shouldBe 5
        game.state.projectedState.getColors(permanent) shouldBe emptySet()
        game.state.getEntity(permanent)?.has<PrototypedComponent>() shouldBe false
    }

    test("a prototyped spell and its permanent have the prototype cost, color and size, and keep everything else") {
        val game = driver()
        val player = game.activePlayer!!
        val golem = game.putCardInHand(player, proto.name)
        game.giveMana(player, Color.RED, 2)
        game.castPrototyped(golem).outcome shouldBe Outcome.Done
        val spell = game.card(game.state.stack.single())
        spell.manaValue shouldBe 2
        spell.colors shouldBe setOf(Color.RED)
        spell.typeLine.isArtifact shouldBe true
        game.resolveAll()
        val permanent = game.findPermanent(player, proto.name)!!
        game.state.projectedState.getPower(permanent) shouldBe 2
        game.state.projectedState.getToughness(permanent) shouldBe 1
        game.state.projectedState.hasColor(permanent, Color.RED) shouldBe true
        game.state.projectedState.hasSubtype(permanent, "Golem") shouldBe true
        game.card(permanent).manaValue shouldBe 2
        game.getLifeTotal(player) shouldBe 21
    }

    test("the prototype mana cost must be paid with its colors (CR 718.3a)") {
        val game = driver()
        val player = game.activePlayer!!
        val golem = game.putCardInHand(player, proto.name)
        game.giveColorlessMana(player, 2)
        game.castPrototyped(golem).error shouldNotBe null
        game.stackSize shouldBe 0
        game.card(golem).isPrinted()
        game.state.getEntity(golem)?.has<PrototypedComponent>() shouldBe false
    }

    test("legal actions offer the prototyped cast alongside the normal one only when affordable") {
        val game = driver()
        val player = game.activePlayer!!
        val golem = game.putCardInHand(player, proto.name)
        fun casts() = game.legalActions(player).mapNotNull { (it.action as? CastSpell)?.takeIf { a -> a.cardId == golem } }
        game.giveMana(player, Color.RED, 2)
        casts().map { it.castPrototyped } shouldBe listOf(true)
        game.giveColorlessMana(player, 4)
        casts().map { it.castPrototyped }.toSet() shouldBe setOf(false, true)
        game.legalActions(player).single { (it.action as? CastSpell)?.castPrototyped == true }.manaCostString shouldBe "{1}{R}"
    }

    test("cost reductions such as affinity apply to the prototype cost") {
        val game = driver()
        val player = game.activePlayer!!
        game.putPermanentOnBattlefield(player, rock.name)
        val golem = game.putCardInHand(player, proto.name)
        game.giveMana(player, Color.RED, 1)
        game.castPrototyped(golem).outcome shouldBe Outcome.Done
        game.resolveAll()
        game.state.projectedState.getPower(game.findPermanent(player, proto.name)!!) shouldBe 2
    }

    for ((label, removal, zone) in listOf(Triple("dies", kill, Zone.GRAVEYARD), Triple("is bounced", bounce, Zone.HAND))) {
        test("a prototyped permanent that $label has only its normal characteristics (CR 718.4)") {
            val game = driver()
            val player = game.activePlayer!!
            val golem = game.putCardInHand(player, proto.name)
            game.giveMana(player, Color.RED, 2)
            game.castPrototyped(golem).outcome shouldBe Outcome.Done
            game.resolveAll()
            val permanent = game.findPermanent(player, proto.name)!!
            game.castFree(removal.name, ChosenTarget.Permanent(permanent))
            game.resolveAll()
            val card = game.state.getZone(player, zone).single { game.card(it).name == proto.name }
            game.card(card).isPrinted()
            game.state.getEntity(card)?.has<PrototypedComponent>() shouldBe false
        }
    }

    test("a countered prototyped spell has only its normal characteristics in the graveyard") {
        val game = driver()
        val player = game.activePlayer!!
        val golem = game.putCardInHand(player, proto.name)
        game.giveMana(player, Color.RED, 2)
        game.castPrototyped(golem).outcome shouldBe Outcome.Done
        game.castFree(counter.name, ChosenTarget.Spell(game.state.stack.single()))
        game.resolveAll()
        val card = game.state.getGraveyard(player).single { game.card(it).name == proto.name }
        game.card(card).isPrinted()
    }

    test("a copy of a prototyped spell is prototyped too (CR 718.3c)") {
        val game = driver()
        val player = game.activePlayer!!
        val golem = game.putCardInHand(player, proto.name)
        game.giveMana(player, Color.RED, 2)
        game.castPrototyped(golem).outcome shouldBe Outcome.Done
        game.castFree(spellCopy.name, ChosenTarget.Spell(game.state.stack.single()))
        game.resolveAll()
        val golems = game.state.getBattlefield().filter { game.card(it).name == proto.name }
        golems.size shouldBe 2
        golems.forEach { game.state.projectedState.getPower(it) shouldBe 2 }
    }

    test("a copy of a prototyped permanent has the prototype characteristics (CR 718.3d)") {
        val game = driver()
        val player = game.activePlayer!!
        val golem = game.putCardInHand(player, proto.name)
        game.giveMana(player, Color.RED, 2)
        game.castPrototyped(golem).outcome shouldBe Outcome.Done
        game.resolveAll()
        val original = game.findPermanent(player, proto.name)!!
        game.castFree(permanentCopy.name, ChosenTarget.Permanent(original))
        game.resolveAll()
        val token = game.state.getBattlefield().single { it != original && game.card(it).name == proto.name }
        game.state.projectedState.getPower(token) shouldBe 2
        game.state.projectedState.hasColor(token, Color.RED) shouldBe true
        game.card(token).manaValue shouldBe 2
    }

    test("a prototyped spell returned to hand has only its normal characteristics, and recasts normally") {
        val game = driver()
        val player = game.activePlayer!!
        val golem = game.putCardInHand(player, proto.name)
        game.giveMana(player, Color.RED, 2)
        game.castPrototyped(golem).outcome shouldBe Outcome.Done
        game.castFree(remand.name, ChosenTarget.Spell(game.state.stack.single()))
        game.resolveAll()
        val card = game.getHand(player).single { game.card(it).name == proto.name }
        game.card(card).isPrinted()
        game.state.getEntity(card)?.has<PrototypedComponent>() shouldBe false
        game.giveColorlessMana(player, 6)
        game.castSpell(player, card).outcome shouldBe Outcome.Done
        game.resolveAll()
        game.state.projectedState.getPower(game.findPermanent(player, proto.name)!!) shouldBe 5
    }

    test("castPrototyped is refused for a card without prototype and alongside bestow") {
        val game = driver()
        val player = game.activePlayer!!
        val plain = game.putCardInHand(player, rock.name)
        game.castPrototyped(plain).error shouldBe "This card has no prototype"
        val both = game.putCardInHand(player, bestowProto.name)
        game.giveMana(player, Color.WHITE, 2)
        game.submit(CastSpell(player, both, castPrototyped = true, useAlternativeCost = true,
            alternativeCostType = AlternativeCostType.BESTOW, paymentStrategy = PaymentStrategy.FromPool)).error shouldBe
            "A prototyped spell is cast with its own characteristics only"
        game.stackSize shouldBe 0
    }
})
