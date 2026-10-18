package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.engine.core.AlternativeCostType
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.mode
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.CostReductionSource
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject
import com.wingedsheep.engine.legalactions.LegalAction
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.webSlinging
import com.wingedsheep.sdk.scripting.MayCastFromGraveyard
import com.wingedsheep.sdk.scripting.MayCastSelfFromZones
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Spell costs priced per target — [CostReductionSource.ChosenTargets], "for each target"
 * (Hinata, Dawn-Crowned) and "for each target beyond the first" (Fireball).
 *
 * CR 601.2c announces targets before CR 601.2f locks the total cost, so the count is the spell's
 * chosen targets. Hinata's ruling: the count is of *different* players and objects, not of uses of
 * the word "target". Before targets exist, enumeration prices the cheapest reachable count — the
 * most different legal targets under a reduction, the fewest required under an increase — and
 * prices every count for the client. An alternative cost is priced with the targets too.
 */
class PerTargetSpellCostTest : FunSpec({

    val discounter = card("Target Discounter") {
        manaCost = "{2}"
        typeLine = "Artifact"
        oracleText = "Spells you cast cost {1} less to cast for each target."
        staticAbility {
            ability = ModifySpellCost(
                target = SpellCostTarget.YouCast(GameObjectFilter.Any),
                modification = CostModification.ReduceGenericBy(CostReductionSource.ChosenTargets()),
            )
        }
    }

    val taxer = card("Target Taxer") {
        manaCost = "{2}"
        typeLine = "Artifact"
        oracleText = "Spells your opponents cast cost {1} more to cast for each target."
        staticAbility {
            ability = ModifySpellCost(
                target = SpellCostTarget.OpponentsCast(GameObjectFilter.Any),
                modification = CostModification.IncreaseGenericBy(CostReductionSource.ChosenTargets()),
            )
        }
    }

    val twinBoost = card("Twin Boost") {
        manaCost = "{3}{G}"
        typeLine = "Instant"
        oracleText = "Target creature gets +1/+1 until end of turn. Target creature gets +1/+1 until end of turn."
        spell {
            effect = Effects.ModifyStats(1, 1, target(TargetFilter.Creature)) then
                Effects.ModifyStats(1, 1, target(TargetFilter.Creature))
        }
    }

    val spreadBoost = card("Spread Boost") {
        manaCost = "{3}{G}"
        typeLine = "Sorcery"
        oracleText = "Up to three target creatures each get +1/+1 until end of turn."
        spell {
            effect = Effects.ModifyStats(
                1, 1, target(TargetObject(count = 3, optional = true, filter = TargetFilter.Creature))
            )
        }
    }

    val flashbackBoost = card("Flashback Boost") {
        manaCost = "{2}{G}"
        typeLine = "Instant"
        oracleText = "Target creature gets +1/+1 until end of turn.\nFlashback {2}{G}"
        spell { effect = Effects.ModifyStats(1, 1, target(TargetFilter.Creature)) }
        keywordAbility(KeywordAbility.flashback("{2}{G}"))
    }

    val twoWayCharm = card("Two-Way Charm") {
        manaCost = "{2}{G}"
        typeLine = "Instant"
        oracleText = "Choose one —\n• Target creature gets +1/+1 until end of turn.\n• Draw a card."
        spell {
            effect = ModalEffect.chooseOne(
                mode("Target creature gets +1/+1 until end of turn") {
                    effect = Effects.ModifyStats(1, 1, target(TargetFilter.Creature))
                },
                mode("Draw a card") { effect = Effects.DrawCards(1) },
            )
        }
    }

    val biteTest = card("Bite Test") {
        manaCost = "{2}{G}"
        typeLine = "Sorcery"
        oracleText = "Target creature you control gets +1/+1 until end of turn. " +
            "Target creature you don't control gets -1/-1 until end of turn."
        spell {
            effect = Effects.ModifyStats(1, 1, target(TargetFilter.CreatureYouControl)) then
                Effects.ModifyStats(-1, -1, target(TargetFilter.CreatureOpponentControls))
        }
    }


    // One required target each, so a per-target modifier moves the price by exactly {1}: under a
    // reduction the offer costs one less, under an opponent's increase one more.
    val graveGrant = card("Grave Grant") {
        manaCost = "{G}"
        typeLine = "Enchantment"
        oracleText = "You may cast instant and sorcery spells from your graveyard."
        staticAbility { ability = MayCastFromGraveyard(filter = GameObjectFilter.InstantOrSorcery) }
    }

    val gravePump = card("Grave Pump") {
        manaCost = "{2}{G}"
        typeLine = "Instant"
        oracleText = "Target creature gets +1/+1 until end of turn."
        spell { effect = Effects.ModifyStats(1, 1, target(TargetFilter.Creature)) }
    }

    val escapePump = card("Escape Pump") {
        manaCost = "{4}{G}"
        typeLine = "Instant"
        oracleText = "Target creature gets +1/+1 until end of turn.\nEscape—{2}{G}, Exile another card from your graveyard."
        spell { effect = Effects.ModifyStats(1, 1, target(TargetFilter.Creature)) }
        keywordAbility(KeywordAbility.escape("{2}{G}", Costs.additional.ExileOtherCards(1)))
    }

    val pumpAdventurer = card("Pump Adventurer") {
        manaCost = "{4}{G}"
        typeLine = "Creature — Elf"
        power = 3
        toughness = 3
        adventure("Adventure Pump") {
            manaCost = "{2}{G}"
            typeLine = "Instant — Adventure"
            spell { effect = Effects.ModifyStats(1, 1, target(TargetFilter.Creature)) }
        }
    }

    val webPump = card("Web Pump") {
        manaCost = "{4}{G}"
        typeLine = "Instant"
        oracleText = "Web-slinging {2}{G}\nTarget creature gets +1/+1 until end of turn."
        webSlinging("{2}{G}")
        spell { effect = Effects.ModifyStats(1, 1, target(TargetFilter.Creature)) }
    }

    val kickerPump = card("Kicker Pump") {
        manaCost = "{2}{G}"
        typeLine = "Instant"
        oracleText = "Kicker {1}\nYou may cast this card from your graveyard.\nTarget creature gets +1/+1 until end of turn."
        keywordAbility(KeywordAbility.OptionalAdditionalCost(ManaCost.parse("{1}")))
        staticAbility { ability = MayCastSelfFromZones(zones = listOf(Zone.GRAVEYARD)) }
        spell { effect = Effects.ModifyStats(1, 1, target(TargetFilter.Creature)) }
    }

    val pairCharm = card("Pair Charm") {
        manaCost = "{3}{G}"
        typeLine = "Sorcery"
        oracleText = "Choose two —\n• Target creature gets +1/+1 until end of turn.\n" +
            "• Target creature gets +1/+1 until end of turn.\n• Draw a card."
        spell {
            effect = ModalEffect(
                modes = listOf(
                    mode("Target creature gets +1/+1 until end of turn") {
                        effect = Effects.ModifyStats(1, 1, target(TargetFilter.Creature))
                    },
                    mode("Another target creature gets +1/+1 until end of turn") {
                        effect = Effects.ModifyStats(1, 1, target(TargetFilter.Creature))
                    },
                    mode("Draw a card") { effect = Effects.DrawCards(1) },
                ),
                chooseCount = 2,
            )
        }
    }


    // Aven Interrupter-shaped: a zone-scoped tax the offer and the cast must both see.
    val graveyardTaxer = card("Graveyard Taxer") {
        manaCost = "{2}"
        typeLine = "Artifact"
        oracleText = "Spells your opponents cast from graveyards cost {2} more to cast."
        staticAbility {
            ability = ModifySpellCost(
                target = SpellCostTarget.OpponentsCastFromZones(setOf(Zone.GRAVEYARD)),
                modification = CostModification.IncreaseGeneric(2),
            )
        }
    }

    fun newGame(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(
            TestCards.all + listOf(
                discounter, taxer, twinBoost, spreadBoost, flashbackBoost, twoWayCharm, biteTest,
                graveGrant, gravePump, escapePump, pumpAdventurer, webPump, kickerPump, pairCharm, graveyardTaxer,
            )
        )
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), skipMulligans = true)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun GameTestDriver.castOn(me: EntityId, spell: EntityId, targets: List<EntityId>) =
        submit(CastSpell(me, spell, targets.map { ChosenTarget.Permanent(it) }))

    fun GameTestDriver.castAction(me: EntityId, spell: EntityId) =
        legalActions(me).firstOrNull { (it.action as? CastSpell)?.cardId == spell }

    /** Offered and affordable — an unaffordable cast is not offered at all. */
    fun GameTestDriver.canCast(me: EntityId, spell: EntityId) = castAction(me, spell)?.affordable == true

    test("a reduction counts each different target") {
        val driver = newGame()
        val me = driver.activePlayer!!
        driver.putPermanentOnBattlefield(me, "Target Discounter")
        val bears = driver.putCreatureOnBattlefield(me, "Grizzly Bears")
        val lions = driver.putCreatureOnBattlefield(me, "Savannah Lions")
        val spell = driver.putCardInHand(me, "Twin Boost")

        driver.giveMana(me, Color.GREEN, 2)
        driver.castOn(me, spell, listOf(bears, lions)).outcome shouldBe Outcome.Done
    }

    test("one object named by two target phrases is one target") {
        val driver = newGame()
        val me = driver.activePlayer!!
        driver.putPermanentOnBattlefield(me, "Target Discounter")
        val bears = driver.putCreatureOnBattlefield(me, "Grizzly Bears")
        driver.putCreatureOnBattlefield(me, "Savannah Lions")
        val spell = driver.putCardInHand(me, "Twin Boost")

        driver.giveMana(me, Color.GREEN, 2)
        driver.castOn(me, spell, listOf(bears, bears)).outcome shouldNotBe Outcome.Done
        driver.giveMana(me, Color.GREEN, 1)
        driver.castOn(me, spell, listOf(bears, bears)).outcome shouldBe Outcome.Done
    }

    test("an opponent's increase counts each different target") {
        val driver = newGame()
        val me = driver.activePlayer!!
        driver.putPermanentOnBattlefield(driver.getOpponent(me), "Target Taxer")
        val bears = driver.putCreatureOnBattlefield(me, "Grizzly Bears")
        val lions = driver.putCreatureOnBattlefield(me, "Savannah Lions")
        val spell = driver.putCardInHand(me, "Twin Boost")

        driver.giveMana(me, Color.GREEN, 5)
        driver.castOn(me, spell, listOf(bears, lions)).outcome shouldNotBe Outcome.Done
        driver.giveMana(me, Color.GREEN, 1)
        driver.castOn(me, spell, listOf(bears, lions)).outcome shouldBe Outcome.Done
    }

    test("a reduction is priced at the most different legal targets for affordability") {
        val driver = newGame()
        val me = driver.activePlayer!!
        driver.putPermanentOnBattlefield(me, "Target Discounter")
        driver.putCreatureOnBattlefield(me, "Grizzly Bears")
        val spell = driver.putCardInHand(me, "Spread Boost")
        driver.putLandOnBattlefield(me, "Forest")
        driver.putLandOnBattlefield(me, "Forest")

        // One creature: {3}{G} less {1} is {2}{G} — two Forests can't.
        driver.canCast(me, spell) shouldBe false
        driver.putCreatureOnBattlefield(me, "Savannah Lions")
        driver.putCreatureOnBattlefield(driver.getOpponent(me), "Centaur Courser")
        // Three creatures reachable: {3}{G} less {3} is {G}.
        val action = driver.castAction(me, spell).shouldNotBeNull()
        action.affordable shouldBe true
        ManaCost.parse(action.manaCostString!!) shouldBe ManaCost.parse("{G}")
    }

    test("every reachable target count is priced for the client") {
        val driver = newGame()
        val me = driver.activePlayer!!
        driver.putPermanentOnBattlefield(me, "Target Discounter")
        driver.putCreatureOnBattlefield(me, "Grizzly Bears")
        driver.putCreatureOnBattlefield(me, "Savannah Lions")
        val spell = driver.putCardInHand(me, "Spread Boost")
        repeat(4) { driver.putLandOnBattlefield(me, "Forest") }

        val byCount = driver.castAction(me, spell).shouldNotBeNull().manaCostByTargetCount.shouldNotBeNull()
        byCount.mapValues { ManaCost.parse(it.value).cmc } shouldBe mapOf(0 to 4, 1 to 3, 2 to 2)
    }

    test("an increase is priced at the fewest targets the spell requires") {
        val driver = newGame()
        val me = driver.activePlayer!!
        driver.putPermanentOnBattlefield(driver.getOpponent(me), "Target Taxer")
        driver.putCreatureOnBattlefield(me, "Grizzly Bears")
        driver.putCreatureOnBattlefield(me, "Savannah Lions")
        val spell = driver.putCardInHand(me, "Twin Boost")
        repeat(4) { driver.putLandOnBattlefield(me, "Forest") }

        // Both phrases may name one creature, so one target is the floor: {3}{G} plus {1}.
        driver.canCast(me, spell) shouldBe false
        driver.putLandOnBattlefield(me, "Forest")
        val action = driver.castAction(me, spell).shouldNotBeNull()
        action.affordable shouldBe true
        ManaCost.parse(action.manaCostString!!).cmc shouldBe 5
    }

    test("with no legal target the reduction has nothing to count") {
        val driver = newGame()
        val me = driver.activePlayer!!
        driver.putPermanentOnBattlefield(me, "Target Discounter")
        val spell = driver.putCardInHand(me, "Spread Boost")
        repeat(4) { driver.putLandOnBattlefield(me, "Forest") }

        val action = driver.castAction(me, spell).shouldNotBeNull()
        ManaCost.parse(action.manaCostString!!).cmc shouldBe 4
        action.manaCostByTargetCount shouldBe mapOf(0 to action.manaCostString)
    }

    fun GameTestDriver.canCastMode(me: EntityId, spell: EntityId, mode: Int) = legalActions(me).any {
        val cast = it.action as? CastSpell
        cast?.cardId == spell && cast.chosenModes == listOf(mode) && it.affordable
    }

    test("each mode of a modal spell is priced by its own targets under a reduction") {
        val driver = newGame()
        val me = driver.activePlayer!!
        driver.putPermanentOnBattlefield(me, "Target Discounter")
        driver.putCreatureOnBattlefield(me, "Grizzly Bears")
        val spell = driver.putCardInHand(me, "Two-Way Charm")
        repeat(2) { driver.putLandOnBattlefield(me, "Forest") }

        // The targeted mode is {1}{G}; the untargeted one is still {2}{G}.
        driver.canCastMode(me, spell, 0) shouldBe true
        driver.canCastMode(me, spell, 1) shouldBe false
    }

    test("each mode of a modal spell is priced by its own targets under an increase") {
        val driver = newGame()
        val me = driver.activePlayer!!
        driver.putPermanentOnBattlefield(driver.getOpponent(me), "Target Taxer")
        driver.putCreatureOnBattlefield(me, "Grizzly Bears")
        val spell = driver.putCardInHand(me, "Two-Way Charm")
        repeat(3) { driver.putLandOnBattlefield(me, "Forest") }

        // The untargeted mode is {2}{G}; the targeted one owes {1} more.
        driver.canCastMode(me, spell, 1) shouldBe true
        driver.canCastMode(me, spell, 0) shouldBe false
    }

    test("requirements whose legal targets can't overlap need a target each") {
        val driver = newGame()
        val me = driver.activePlayer!!
        driver.putPermanentOnBattlefield(driver.getOpponent(me), "Target Taxer")
        driver.putCreatureOnBattlefield(me, "Grizzly Bears")
        driver.putCreatureOnBattlefield(driver.getOpponent(me), "Savannah Lions")
        val spell = driver.putCardInHand(me, "Bite Test")
        repeat(4) { driver.putLandOnBattlefield(me, "Forest") }

        // Your creature and theirs are two targets: {2}{G} plus {2}.
        driver.canCast(me, spell) shouldBe false
        driver.putLandOnBattlefield(me, "Forest")
        driver.canCast(me, spell) shouldBe true
    }

    test("requirements whose legal targets can't overlap reach their sum under a reduction") {
        val driver = newGame()
        val me = driver.activePlayer!!
        driver.putPermanentOnBattlefield(me, "Target Discounter")
        driver.putCreatureOnBattlefield(me, "Grizzly Bears")
        driver.putCreatureOnBattlefield(driver.getOpponent(me), "Savannah Lions")
        val spell = driver.putCardInHand(me, "Bite Test")
        driver.putLandOnBattlefield(me, "Forest")

        // Two targets take {2} off {2}{G}.
        driver.canCast(me, spell) shouldBe true
        val byCount = driver.castAction(me, spell).shouldNotBeNull().manaCostByTargetCount.shouldNotBeNull()
        byCount.keys shouldBe setOf(2)
    }

    test("an alternative cost is priced with the chosen targets") {
        val driver = newGame()
        val me = driver.activePlayer!!
        driver.putPermanentOnBattlefield(me, "Target Discounter")
        val bears = driver.putCreatureOnBattlefield(me, "Grizzly Bears")
        val spell = driver.putCardInGraveyard(me, "Flashback Boost")

        driver.giveMana(me, Color.GREEN, 2)
        driver.submit(
            CastSpell(
                me, spell, listOf(ChosenTarget.Permanent(bears)),
                useAlternativeCost = true, alternativeCostType = AlternativeCostType.FLASHBACK,
            )
        ).outcome shouldBe Outcome.Done
    }
    // Every cast offer is priced by target count — not just the hand cast and flashback. Each case
    // sets up one offer of a spell with one required target whose base price is [base] mana.
    data class Offer(val name: String, val base: Int, val setup: (GameTestDriver, EntityId) -> EntityId, val pick: (LegalAction) -> Boolean)

    val offers = listOf(
        Offer("a graveyard cast permission", 3, { d, me ->
            d.putPermanentOnBattlefield(me, "Grave Grant")
            d.putCardInGraveyard(me, "Grave Pump")
        }, { it.actionType == "CastSpell" }),
        Offer("escape", 3, { d, me ->
            d.putCardInGraveyard(me, "Forest")
            d.putCardInGraveyard(me, "Escape Pump")
        }, { it.actionType == "CastWithEscape" }),
        Offer("an Adventure face", 3, { d, me -> d.putCardInHand(me, "Pump Adventurer") },
            { (it.action as CastSpell).faceIndex == 0 }),
        Offer("web-slinging", 3, { d, me ->
            d.tapPermanent(d.putCreatureOnBattlefield(me, "Savannah Lions"))
            d.putCardInHand(me, "Web Pump")
        }, { (it.action as CastSpell).alternativeCostType == AlternativeCostType.WEB_SLINGING }),
        Offer("a kicked cast from the graveyard", 4, { d, me -> d.putCardInGraveyard(me, "Kicker Pump") },
            { it.actionType == "CastWithKicker" }),
    )

    fun GameTestDriver.offer(me: EntityId, spell: EntityId, pick: (LegalAction) -> Boolean) =
        legalActions(me).firstOrNull { (it.action as? CastSpell)?.cardId == spell && pick(it) }

    for (o in offers) {
        test("${o.name} is priced per target under a reduction") {
            val driver = newGame()
            val me = driver.activePlayer!!
            driver.putPermanentOnBattlefield(me, "Target Discounter")
            driver.putCreatureOnBattlefield(me, "Grizzly Bears")
            val spell = o.setup(driver, me)
            repeat(o.base - 1) { driver.putLandOnBattlefield(me, "Forest") }

            // Only the one-target discounted price is payable.
            val action = driver.offer(me, spell, o.pick).shouldNotBeNull()
            action.affordable shouldBe true
            ManaCost.parse(action.manaCostString!!).cmc shouldBe o.base - 1
            action.manaCostByTargetCount.shouldNotBeNull().mapValues { ManaCost.parse(it.value).cmc } shouldBe
                mapOf(1 to o.base - 1)
        }

        test("${o.name} is priced per target under an opponent's increase") {
            val driver = newGame()
            val me = driver.activePlayer!!
            driver.putPermanentOnBattlefield(driver.getOpponent(me), "Target Taxer")
            driver.putCreatureOnBattlefield(me, "Grizzly Bears")
            val spell = o.setup(driver, me)
            repeat(o.base) { driver.putLandOnBattlefield(me, "Forest") }

            // The base price alone no longer covers the one target the spell must announce.
            driver.offer(me, spell, o.pick)?.affordable shouldNotBe true
            driver.putLandOnBattlefield(me, "Forest")
            val action = driver.offer(me, spell, o.pick).shouldNotBeNull()
            action.affordable shouldBe true
            action.manaCostByTargetCount.shouldNotBeNull().mapValues { ManaCost.parse(it.value).cmc } shouldBe
                mapOf(1 to o.base + 1)
        }
    }

    fun GameTestDriver.canCastModal(me: EntityId, spell: EntityId) =
        offer(me, spell) { it.actionType == "CastSpellModal" }?.affordable == true

    test("a choose-two spell is priced at its cheapest mode combination under a reduction") {
        val driver = newGame()
        val me = driver.activePlayer!!
        driver.putPermanentOnBattlefield(me, "Target Discounter")
        driver.putCreatureOnBattlefield(me, "Grizzly Bears")
        driver.putCreatureOnBattlefield(me, "Savannah Lions")
        val spell = driver.putCardInHand(me, "Pair Charm")
        repeat(2) { driver.putLandOnBattlefield(me, "Forest") }

        // Both targeted modes, on two different creatures: {3}{G} less {2}. No single mode reaches it.
        driver.canCastModal(me, spell) shouldBe true
    }

    test("a choose-two spell is priced at its cheapest mode combination under an increase") {
        val driver = newGame()
        val me = driver.activePlayer!!
        driver.putPermanentOnBattlefield(driver.getOpponent(me), "Target Taxer")
        driver.putCreatureOnBattlefield(me, "Grizzly Bears")
        val spell = driver.putCardInHand(me, "Pair Charm")
        repeat(4) { driver.putLandOnBattlefield(me, "Forest") }

        // Any two modes include a targeted one, so {3}{G} owes at least {1} more — the untargeted
        // mode alone, priced at {3}{G}, is not a legal selection.
        driver.canCastModal(me, spell) shouldBe false
        driver.putLandOnBattlefield(me, "Forest")
        driver.canCastModal(me, spell) shouldBe true
    }
    test("a zone-scoped tax prices a flashback offer exactly as the cast is charged") {
        val driver = newGame()
        val me = driver.activePlayer!!
        driver.putPermanentOnBattlefield(driver.getOpponent(me), "Graveyard Taxer")
        val bears = driver.putCreatureOnBattlefield(me, "Grizzly Bears")
        val spell = driver.putCardInGraveyard(me, "Flashback Boost")
        repeat(4) { driver.putLandOnBattlefield(me, "Forest") }
        val isFlashback: (LegalAction) -> Boolean = { it.actionType == "CastWithFlashback" }
        fun flashback() = driver.submit(
            CastSpell(
                me, spell, listOf(ChosenTarget.Permanent(bears)),
                useAlternativeCost = true, alternativeCostType = AlternativeCostType.FLASHBACK,
            )
        )

        // {2}{G} flashback plus the {2} graveyard tax: four Forests cover neither the offer nor the cast.
        driver.offer(me, spell, isFlashback).shouldNotBeNull().affordable shouldBe false
        flashback().outcome shouldNotBe Outcome.Done
        driver.putLandOnBattlefield(me, "Forest")
        val action = driver.offer(me, spell, isFlashback).shouldNotBeNull()
        action.affordable shouldBe true
        ManaCost.parse(action.manaCostString!!).cmc shouldBe 5
        flashback().outcome shouldBe Outcome.Done
    }
})
