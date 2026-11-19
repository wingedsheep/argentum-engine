package com.wingedsheep.engine.mechanics.targeting

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.CardType
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GrantProtectionFromToGroup
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.ProtectionScope
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.predicates.ControllerPredicate
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * "Protection from <card type>s your opponents control" — [ProtectionScope.CardType] with
 * `controller = ControlledByOpponent` (Crypsis). The quality is relative to the protected object's
 * controller and read whenever protection is checked (CR 702.16b: a source "with the stated
 * quality"; control is information about the object). Pins:
 * - a creature that changes hands changes whether it matches;
 * - the player-protection reading (an opponent's creature can't target the player, their own can);
 * - the SDK contract — only `ControlledByOpponent`, granted rather than printed, and its wording.
 *
 * Blocking, damage and the non-creature-spell case are pinned through a real card in
 * `CrypsisScenarioTest`.
 */
class OpponentCardTypeProtectionTest : FunSpec({

    val opponents = ControllerPredicate.ControlledByOpponent

    val shield = card("Test Opponent-Creature Shield") {
        manaCost = "{U}"
        typeLine = "Instant"
        spell {
            val t = target(TargetFilter.CreatureYouControl)
            effect = Effects.GrantProtectionFromCardType(CardType.CREATURE, t, controller = opponents)
        }
    }
    val playerShield = card("Test Player Opponent-Creature Shield") {
        manaCost = "{U}"
        typeLine = "Instant"
        spell { effect = Effects.GrantPlayerProtection(ProtectionScope.CardType("Creature", opponents)) }
    }
    val steal = card("Test Steal") {
        manaCost = "{U}"
        typeLine = "Instant"
        spell {
            val t = target(TargetFilter.Creature)
            effect = Effects.GainControl(t)
        }
    }
    val pinger = card("Test Pinger") {
        manaCost = "{2}{R}"
        typeLine = "Creature — Human Wizard"
        power = 1
        toughness = 1
        activatedAbility {
            cost = Costs.Tap
            val t = target(Targets.Any)
            effect = Effects.DealDamage(1, t)
        }
    }

    fun GameTestDriver.resolveAll() {
        var guard = 0
        while (stackSize > 0 && pendingDecision == null && guard++ < 10) bothPass()
    }

    fun newGame(): Triple<GameTestDriver, EntityId, EntityId> {
        val d = GameTestDriver().apply {
            registerCards(TestCards.all + shield + playerShield + steal + pinger)
            initMirrorMatch(deck = Deck.of("Island" to 40), skipMulligans = true)
        }
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val me = d.activePlayer!!
        return Triple(d, me, d.getOpponent(me))
    }

    fun GameTestDriver.cast(player: EntityId, name: String, targets: List<EntityId> = emptyList()) {
        val spell = putCardInHand(player, name)
        giveMana(player, Color.BLUE, 1)
        castSpell(player, spell, targets).error shouldBe null
        resolveAll()
    }

    fun GameTestDriver.ping(controller: EntityId, pingerId: EntityId, target: ChosenTarget) =
        submit(ActivateAbility(controller, pingerId, pinger.activatedAbilities.first().id, listOf(target)))

    test("a creature that changes hands changes whether it matches") {
        val (d, me, opp) = newGame()
        val courser = d.putCreatureOnBattlefield(me, "Centaur Courser")
        val theirPinger = d.putCreatureOnBattlefield(opp, "Test Pinger")
        d.removeSummoningSickness(theirPinger)
        d.cast(me, "Test Opponent-Creature Shield", listOf(courser))

        withClue("while an opponent controls the pinger, its ability can't target the Courser") {
            d.passPriority(me)
            d.ping(opp, theirPinger, ChosenTarget.Permanent(courser)).error shouldNotBe null
            d.passPriority(opp)
        }

        d.cast(me, "Test Steal", listOf(theirPinger))
        d.removeSummoningSickness(theirPinger)
        withClue("once I control it, it no longer has the quality") {
            d.ping(me, theirPinger, ChosenTarget.Permanent(courser)).error shouldBe null
        }
    }

    test("player protection: an opponent's creature can't target the player, their own creature can") {
        val (d, me, opp) = newGame()
        val theirPinger = d.putCreatureOnBattlefield(opp, "Test Pinger")
        val myPinger = d.putCreatureOnBattlefield(me, "Test Pinger")
        d.removeSummoningSickness(theirPinger)
        d.removeSummoningSickness(myPinger)
        d.cast(me, "Test Player Opponent-Creature Shield")

        d.passPriority(me)
        d.ping(opp, theirPinger, ChosenTarget.Player(me)).error shouldNotBe null
        d.passPriority(opp)
        d.ping(me, myPinger, ChosenTarget.Player(me)).error shouldBe null
        d.resolveAll()
        d.getLifeTotal(me) shouldBe 19
    }

    test("only ControlledByOpponent is a supported controller") {
        shouldThrow<IllegalArgumentException> {
            ProtectionScope.CardType("Creature", ControllerPredicate.ControlledByYou)
        }
    }

    test("it is a granted quality: a printed Protection keyword rejects it, a group grant takes it") {
        shouldThrow<IllegalArgumentException> {
            KeywordAbility.Protection(ProtectionScope.CardType("Creature", opponents))
        }
        GrantProtectionFromToGroup.isSupported(ProtectionScope.CardType("Creature", opponents)) shouldBe true
        GrantProtectionFromToGroup.isSupported(ProtectionScope.CardType("Creature")) shouldBe false
        ProtectionKeywords.keywordsFor(ProtectionScope.CardType("Creature", opponents)) shouldBe
            setOf("PROTECTION_FROM_OPPONENTS_CARDTYPE_CREATURE")
    }

    test("the grant reads as the printed text") {
        Effects.GrantProtectionFromCardType(
            CardType.CREATURE, EffectTarget.ContextTarget(0), controller = opponents
        ).description shouldBe
            "${EffectTarget.ContextTarget(0).description} gains protection from creatures your opponents control until end of turn"
    }
})
