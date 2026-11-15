package com.wingedsheep.engine.mana

import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * [SpellCostTarget.OpponentsCastTargeting]'s `targetPlayer` axis — "spells your opponents cast that
 * target **you** or a permanent you control cost {2} more to cast" (Monastery Siege's Dragons mode).
 *
 * The tax is read relative to the modifier's source: a player target counts only when it is the
 * player `targetPlayer` names (here the source's controller), only opponents of that controller are
 * taxed, and a spell that targets both the player and a permanent of theirs is taxed once.
 */
class OpponentsCastTargetingPlayerTest : FunSpec({

    val wardOfYou = card("Ward of You and Yours") {
        manaCost = "{2}{U}"
        typeLine = "Enchantment"
        oracleText = "Spells your opponents cast that target you or a permanent you control cost {2} more to cast."
        staticAbility {
            ability = ModifySpellCost(
                target = SpellCostTarget.OpponentsCastTargeting(
                    GroupFilter.AllPermanentsYouControl,
                    targetPlayer = Player.You,
                ),
                modification = CostModification.IncreaseGeneric(2),
            )
        }
    }

    // The pre-axis shape: permanents only, so a spell aimed at the player is untaxed.
    val wardOfYours = card("Ward of Yours Only") {
        manaCost = "{2}{U}"
        typeLine = "Enchantment"
        oracleText = "Spells your opponents cast that target a permanent you control cost {2} more to cast."
        staticAbility {
            ability = ModifySpellCost(
                target = SpellCostTarget.OpponentsCastTargeting(GroupFilter.AllPermanentsYouControl),
                modification = CostModification.IncreaseGeneric(2),
            )
        }
    }

    val lifeWard = card("Blood Ward of You") {
        manaCost = "{2}{B}"
        typeLine = "Enchantment"
        oracleText = "Spells your opponents cast that target you cost an additional 3 life to cast."
        staticAbility {
            ability = ModifySpellCost(
                target = SpellCostTarget.OpponentsCastTargeting(GroupFilter.source(), targetPlayer = Player.You),
                modification = CostModification.IncreaseLife(3),
            )
        }
    }

    /** Active player (the caster) vs. the non-active player, who controls [ward]. */
    fun setup(ward: String): Triple<GameTestDriver, EntityId, EntityId> {
        val d = GameTestDriver().apply {
            registerCards(TestCards.all + wardOfYou + wardOfYours + lifeWard)
            initMirrorMatch(deck = Deck.of("Mountain" to 40), skipMulligans = true)
        }
        val caster = d.activePlayer!!
        val defender = d.getOpponent(caster)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        d.putPermanentOnBattlefield(defender, ward)
        return Triple(d, caster, defender)
    }

    fun boltCost(d: GameTestDriver, caster: EntityId, targets: List<EntityId>): String =
        d.services.costCalculator.calculateEffectiveCost(
            d.state, d.cardRegistry.getCard("Lightning Bolt")!!, caster, chosenTargets = targets,
        ).toString()

    test("an opponent's spell that targets you costs {2} more") {
        val (d, caster, defender) = setup("Ward of You and Yours")
        boltCost(d, caster, listOf(defender)) shouldBe ManaCost.parse("{2}{R}").toString()

        val bolt = d.putCardInHand(caster, "Lightning Bolt")
        d.giveMana(caster, Color.RED, 1)
        d.castSpell(caster, bolt, listOf(defender)).error shouldNotBe null
        d.giveMana(caster, Color.RED, 2)
        d.castSpell(caster, bolt, listOf(defender)).error shouldBe null
    }

    test("an opponent's spell that targets a permanent you control costs {2} more") {
        val (d, caster, defender) = setup("Ward of You and Yours")
        val bears = d.putCreatureOnBattlefield(defender, "Centaur Courser")
        boltCost(d, caster, listOf(bears)) shouldBe ManaCost.parse("{2}{R}").toString()
    }

    test("targeting both you and your permanent taxes the spell once") {
        val (d, caster, defender) = setup("Ward of You and Yours")
        val bears = d.putCreatureOnBattlefield(defender, "Centaur Courser")
        boltCost(d, caster, listOf(defender, bears)) shouldBe ManaCost.parse("{2}{R}").toString()
    }

    test("a spell aimed at the caster or the caster's own permanents is untaxed") {
        val (d, caster, _) = setup("Ward of You and Yours")
        val own = d.putCreatureOnBattlefield(caster, "Centaur Courser")
        boltCost(d, caster, listOf(caster)) shouldBe ManaCost.parse("{R}").toString()
        boltCost(d, caster, listOf(own)) shouldBe ManaCost.parse("{R}").toString()
    }

    test("the ward's controller is not taxed for targeting themselves") {
        val (d, _, defender) = setup("Ward of You and Yours")
        boltCost(d, defender, listOf(defender)) shouldBe ManaCost.parse("{R}").toString()
    }

    test("without targetPlayer a player target is untaxed but a permanent target still is") {
        val (d, caster, defender) = setup("Ward of Yours Only")
        val bears = d.putCreatureOnBattlefield(defender, "Centaur Courser")
        boltCost(d, caster, listOf(defender)) shouldBe ManaCost.parse("{R}").toString()
        boltCost(d, caster, listOf(bears)) shouldBe ManaCost.parse("{2}{R}").toString()
    }

    test("a life tax counts a player target") {
        val (d, caster, defender) = setup("Blood Ward of You")
        val bolt = d.putCardInHand(caster, "Lightning Bolt")
        d.giveMana(caster, Color.RED, 1)
        d.castSpell(caster, bolt, listOf(defender)).error shouldBe null
        d.getLifeTotal(caster) shouldBe 17
    }
})
