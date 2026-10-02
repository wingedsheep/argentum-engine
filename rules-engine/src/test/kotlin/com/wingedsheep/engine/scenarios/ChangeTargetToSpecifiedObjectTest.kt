package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.state.components.stack.TargetsComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.TypeLine
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.CardScript
import com.wingedsheep.sdk.model.CreatureStats
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.AbilityId
import com.wingedsheep.sdk.scripting.ActivatedAbility
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.AnyTarget
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject
import com.wingedsheep.sdk.scripting.targets.TargetPlayer
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * `Effects.ChangeTarget(to = …)` — "change the target of target spell with a single target to this
 * creature" — and the `withSingleTarget()` targeting restriction (`StatePredicate.HasSingleTarget`).
 *
 * CR 115.7a: a target can be changed only to another legal target; if it can't be, it's unchanged.
 * Legality is judged for the redirected spell, from its controller's side.
 */
class ChangeTargetToSpecifiedObjectTest : FunSpec({

    val redirectId = AbilityId("redirector-redirect")

    fun redirector(name: String, vararg keywords: Keyword) = CardDefinition(
        name = name,
        manaCost = ManaCost.parse("{3}"),
        typeLine = TypeLine.parse("Artifact Creature — Construct"),
        oracleText = "{0}: Change the target of target spell with a single target to this creature.",
        creatureStats = CreatureStats(1, 4),
        keywords = keywords.toSet(),
        script = CardScript.permanent(
            ActivatedAbility(
                id = redirectId,
                cost = AbilityCost.Free,
                effect = Effects.ChangeTarget(to = EffectTarget.Self),
                targetRequirements = listOf(TargetObject(filter = TargetFilter.SpellOnStack.withSingleTarget())),
            )
        )
    )

    val Redirector = redirector("Redirector")
    val HexproofRedirector = redirector("Hexproof Redirector", Keyword.HEXPROOF)

    val MindSap = CardDefinition(
        name = "Mind Sap",
        manaCost = ManaCost.parse("{B}"),
        typeLine = TypeLine.parse("Instant"),
        oracleText = "Target player loses 2 life.",
        script = CardScript.spell(
            effect = Effects.LoseLife(2, EffectTarget.ContextTarget(0)),
            TargetPlayer()
        )
    )

    val DoubleZap = CardDefinition(
        name = "Double Zap",
        manaCost = ManaCost.parse("{R}"),
        typeLine = TypeLine.parse("Instant"),
        oracleText = "Double Zap deals 1 damage to each of two targets.",
        script = CardScript.spell(
            effect = Effects.DealDamage(1, EffectTarget.ContextTarget(0)),
            AnyTarget(count = 2)
        )
    )

    data class Setup(val driver: GameTestDriver, val me: EntityId, val opponent: EntityId)

    fun setup(): Setup {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(Redirector, HexproofRedirector, MindSap, DoubleZap))
        driver.initMirrorMatch(deck = Deck.of("Island" to 40))
        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return Setup(driver, me, opponent)
    }

    /** [caster] casts [spell] at [targets] from floating mana; returns the spell's stack id. */
    fun GameTestDriver.cast(caster: EntityId, spell: String, color: Color, targets: List<ChosenTarget>): EntityId {
        val card = putCardInHand(caster, spell)
        giveMana(caster, color, 1)
        submit(CastSpell(caster, card, targets, paymentStrategy = PaymentStrategy.FromPool)).error shouldBe null
        return getTopOfStack()!!
    }

    fun GameTestDriver.redirect(me: EntityId, source: EntityId, spell: EntityId) =
        submit(ActivateAbility(me, source, redirectId, targets = listOf(ChosenTarget.Spell(spell))))

    fun GameTestDriver.targetsOf(spell: EntityId) = state.getEntity(spell)!!.get<TargetsComponent>()!!.targets

    test("the spell's single target becomes the named object, with no choice offered") {
        val (driver, me, opponent) = setup()
        val bears = driver.putPermanentOnBattlefield(me, "Grizzly Bears")
        val redirector = driver.putPermanentOnBattlefield(me, "Redirector")
        driver.passPriority(me)
        val bolt = driver.cast(opponent, "Lightning Bolt", Color.RED, listOf(ChosenTarget.Permanent(bears)))
        driver.passPriority(opponent)

        driver.redirect(me, redirector, bolt).error shouldBe null
        driver.bothPass()

        driver.state.pendingDecision shouldBe null
        driver.targetsOf(bolt) shouldBe listOf(ChosenTarget.Permanent(redirector))
        driver.bothPass()
        driver.findPermanent(me, "Grizzly Bears") shouldNotBe null
        driver.findPermanent(me, "Redirector") shouldNotBe null
    }

    test("CR 115.7a: a named object that isn't a legal target leaves the target unchanged") {
        val (driver, me, opponent) = setup()
        val redirector = driver.putPermanentOnBattlefield(me, "Redirector")
        driver.passPriority(me)
        val sap = driver.cast(opponent, "Mind Sap", Color.BLACK, listOf(ChosenTarget.Player(me)))
        driver.passPriority(opponent)

        driver.redirect(me, redirector, sap).error shouldBe null
        driver.bothPass()

        withClue("'target player' can't become a creature") {
            driver.targetsOf(sap) shouldBe listOf(ChosenTarget.Player(me))
        }
    }

    test("legality is judged from the redirected spell's controller — an opponent's spell can't be moved onto hexproof") {
        val (driver, me, opponent) = setup()
        val bears = driver.putPermanentOnBattlefield(me, "Grizzly Bears")
        val redirector = driver.putPermanentOnBattlefield(me, "Hexproof Redirector")
        driver.passPriority(me)
        val bolt = driver.cast(opponent, "Lightning Bolt", Color.RED, listOf(ChosenTarget.Permanent(bears)))
        driver.passPriority(opponent)

        driver.redirect(me, redirector, bolt).error shouldBe null
        driver.bothPass()

        driver.targetsOf(bolt) shouldBe listOf(ChosenTarget.Permanent(bears))
    }

    test("…but our own spell may be moved onto our hexproof creature") {
        val (driver, me, opponent) = setup()
        val redirector = driver.putPermanentOnBattlefield(me, "Hexproof Redirector")
        val bolt = driver.cast(me, "Lightning Bolt", Color.RED, listOf(ChosenTarget.Player(opponent)))

        driver.redirect(me, redirector, bolt).error shouldBe null
        driver.bothPass()

        driver.targetsOf(bolt) shouldBe listOf(ChosenTarget.Permanent(redirector))
    }

    test("withSingleTarget: a spell with two targets can't be chosen") {
        val (driver, me, opponent) = setup()
        val bears = driver.putPermanentOnBattlefield(me, "Grizzly Bears")
        val redirector = driver.putPermanentOnBattlefield(me, "Redirector")
        driver.passPriority(me)
        val zap = driver.cast(
            opponent, "Double Zap", Color.RED,
            listOf(ChosenTarget.Permanent(bears), ChosenTarget.Player(me))
        )
        driver.passPriority(opponent)

        driver.targetsOf(zap).size shouldBe 2
        driver.redirect(me, redirector, zap).error shouldNotBe null
    }

    test("withSingleTarget: a spell with exactly one target can be chosen") {
        val (driver, me, opponent) = setup()
        val redirector = driver.putPermanentOnBattlefield(me, "Redirector")
        driver.passPriority(me)
        val bolt = driver.cast(opponent, "Lightning Bolt", Color.RED, listOf(ChosenTarget.Player(me)))
        driver.passPriority(opponent)

        driver.redirect(me, redirector, bolt).error shouldBe null
    }
})
