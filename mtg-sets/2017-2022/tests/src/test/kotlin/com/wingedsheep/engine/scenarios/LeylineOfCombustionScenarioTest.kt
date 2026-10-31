package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.m20.cards.LeylineOfCombustion
import com.wingedsheep.mtg.sets.definitions.plc.cards.ProdigalPyromancer
import com.wingedsheep.mtg.sets.definitions.sth.cards.Shock
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Leyline of Combustion — "Whenever you and/or at least one permanent you control becomes the
 * target of a spell or ability an opponent controls, this enchantment deals 2 damage to that
 * player."
 *
 * The batch half is the point: a spell aimed at you *and* your creatures triggers it once (the
 * 2019-07-12 ruling), not once per target. The opponent is the active player and the caster; the
 * Leyline sits on the other side.
 */
class LeylineOfCombustionScenarioTest : FunSpec({

    // "Up to two target creatures each take 1 damage, and target player takes 1 damage."
    val scatterShot = card("Test Scatter Shot") {
        manaCost = "{R}"; typeLine = "Instant"
        spell {
            val (first, second) = targets(TargetFilter.Creature, count = 2, optional = true)
            val player = target(Targets.Player)
            effect = Effects.DealDamage(1, first) then Effects.DealDamage(1, second) then
                Effects.DealDamage(1, player)
        }
    }

    fun setup(): GameTestDriver = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(LeylineOfCombustion, Shock, ProdigalPyromancer, scatterShot))
        initMirrorMatch(deck = Deck.of("Mountain" to 40), skipMulligans = true)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }
    fun GameTestDriver.caster(): EntityId = activePlayer!!
    fun GameTestDriver.leylineOwner(): EntityId = getOpponent(caster())
    fun GameTestDriver.cast(name: String, targets: List<ChosenTarget>) {
        giveMana(caster(), Color.RED, 2)
        val spell = putCardInHand(caster(), name)
        castSpellWithTargets(caster(), spell, targets).error shouldBe null
    }

    test("a spell targeting you and two of your creatures triggers it once") {
        val d = setup()
        d.putPermanentOnBattlefield(d.leylineOwner(), "Leyline of Combustion")
        val bearA = d.putCreatureOnBattlefield(d.leylineOwner(), "Grizzly Bears")
        val bearB = d.putCreatureOnBattlefield(d.leylineOwner(), "Grizzly Bears")

        d.cast(
            "Test Scatter Shot",
            listOf(ChosenTarget.Permanent(bearA), ChosenTarget.Permanent(bearB), ChosenTarget.Player(d.leylineOwner()))
        )
        withClue("one Leyline trigger above the spell") { d.assertStackSize(2) }
        d.bothPass()
        withClue("the trigger resolves before the spell: 2 damage to the caster") {
            d.getLifeTotal(d.caster()) shouldBe 18
            d.getLifeTotal(d.leylineOwner()) shouldBe 20
        }
        d.bothPass()
        d.getLifeTotal(d.caster()) shouldBe 18
        d.getLifeTotal(d.leylineOwner()) shouldBe 19
    }

    test("a spell targeting only you triggers it") {
        val d = setup()
        d.putPermanentOnBattlefield(d.leylineOwner(), "Leyline of Combustion")
        d.cast("Shock", listOf(ChosenTarget.Player(d.leylineOwner())))
        d.assertStackSize(2)
        d.bothPass(); d.bothPass()
        d.getLifeTotal(d.caster()) shouldBe 18
        d.getLifeTotal(d.leylineOwner()) shouldBe 18
    }

    test("an opponent's activated ability targeting your creature triggers it") {
        val d = setup()
        d.putPermanentOnBattlefield(d.leylineOwner(), "Leyline of Combustion")
        val bears = d.putCreatureOnBattlefield(d.leylineOwner(), "Grizzly Bears")
        val pyromancer = d.putCreatureOnBattlefield(d.caster(), "Prodigal Pyromancer")
        d.removeSummoningSickness(pyromancer)

        d.submit(
            ActivateAbility(
                d.caster(), pyromancer, ProdigalPyromancer.activatedAbilities.single().id,
                listOf(ChosenTarget.Permanent(bears))
            )
        ).error shouldBe null
        d.assertStackSize(2)
        d.bothPass()
        d.getLifeTotal(d.caster()) shouldBe 18
    }

    test("a spell targeting only the opponent's own things doesn't trigger it") {
        val d = setup()
        d.putPermanentOnBattlefield(d.leylineOwner(), "Leyline of Combustion")
        val theirBears = d.putCreatureOnBattlefield(d.caster(), "Grizzly Bears")
        d.cast("Test Scatter Shot", listOf(ChosenTarget.Permanent(theirBears), ChosenTarget.Player(d.caster())))
        d.assertStackSize(1)
        d.bothPass()
        d.getLifeTotal(d.caster()) shouldBe 19
    }

    test("your own spell targeting your things doesn't trigger it") {
        val d = setup()
        d.putPermanentOnBattlefield(d.caster(), "Leyline of Combustion")
        val ownBears = d.putCreatureOnBattlefield(d.caster(), "Grizzly Bears")
        d.cast("Test Scatter Shot", listOf(ChosenTarget.Permanent(ownBears), ChosenTarget.Player(d.caster())))
        d.assertStackSize(1)
        d.bothPass()
        d.getLifeTotal(d.caster()) shouldBe 19
        d.getLifeTotal(d.leylineOwner()) shouldBe 20
    }

    test("two Leylines each trigger once") {
        val d = setup()
        d.putPermanentOnBattlefield(d.leylineOwner(), "Leyline of Combustion")
        d.putPermanentOnBattlefield(d.leylineOwner(), "Leyline of Combustion")
        val bears = d.putCreatureOnBattlefield(d.leylineOwner(), "Grizzly Bears")
        d.cast("Test Scatter Shot", listOf(ChosenTarget.Permanent(bears), ChosenTarget.Player(d.leylineOwner())))
        d.assertStackSize(3)
    }
})
