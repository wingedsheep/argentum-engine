package com.wingedsheep.engine.mechanics.cost

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.AlternativeCostType
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.emerge
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import com.wingedsheep.sdk.scripting.CantBeSacrificed
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * `CantBeSacrificed(GroupFilter.AllCreatures, Scope.CASTING_AND_ACTIVATION_COSTS)` — "Players can't
 * sacrifice creatures to cast spells or activate abilities" (Angel of Jubilation). Each case pins one
 * of the card's rulings:
 *
 *  - a spell whose cost sacrifices a creature can't be cast (Fling, Bone Splinters);
 *  - an activated ability whose cost sacrifices a creature can't be activated — mana abilities
 *    included (Ashnod's Altar), and "Sacrifice this creature" too;
 *  - it is per permanent, not per wording: "Sacrifice an artifact" can't take an artifact creature,
 *    but a noncreature artifact still pays it;
 *  - a resolving spell or ability may still make a player sacrifice a creature;
 *  - it binds every player, and ends when its source leaves the battlefield.
 */
class SacrificeForCostsProhibitionTest : FunSpec({

    // An enchantment, so it is never itself fodder for the "Sacrifice another artifact" case.
    val lock = card("Sacrifice Cost Lock") {
        manaCost = "{3}"
        typeLine = "Enchantment"
        oracleText = "Players can't sacrifice creatures to cast spells or activate abilities."
        staticAbility {
            ability = CantBeSacrificed(GroupFilter.AllCreatures, CantBeSacrificed.Scope.CASTING_AND_ACTIVATION_COSTS)
        }
    }
    val bear = card("Sac Test Bear") {
        manaCost = "{1}{G}"
        typeLine = "Creature — Bear"
        power = 2
        toughness = 2
    }
    val construct = card("Sac Test Construct") {
        manaCost = "{1}"
        typeLine = "Artifact Creature — Construct"
        power = 1
        toughness = 1
    }
    val trinket = card("Sac Test Trinket") {
        manaCost = "{1}"
        typeLine = "Artifact"
    }
    val splinters = card("Sac Cost Sorcery") {
        manaCost = "{B}"
        typeLine = "Sorcery"
        oracleText = "As an additional cost to cast this spell, sacrifice a creature.\nDraw a card."
        additionalCost(Costs.additional.SacrificePermanent(GameObjectFilter.Creature))
        spell { effect = Effects.DrawCards(1) }
    }
    val edict = card("Sac Effect Sorcery") {
        manaCost = "{B}"
        typeLine = "Sorcery"
        oracleText = "Sacrifice a creature. Draw a card."
        spell { effect = Effects.SacrificeOwn(GameObjectFilter.Creature).then(Effects.DrawCards(1)) }
    }
    val outlet = card("Creature Sac Outlet") {
        manaCost = "{1}"
        typeLine = "Artifact"
        oracleText = "Sacrifice a creature: Draw a card."
        activatedAbility {
            cost = Costs.Sacrifice(GameObjectFilter.Creature)
            effect = Effects.DrawCards(1)
            timing = TimingRule.InstantSpeed
        }
    }
    val artifactOutlet = card("Artifact Sac Outlet") {
        manaCost = "{1}"
        typeLine = "Artifact"
        oracleText = "Sacrifice another artifact: Draw a card."
        activatedAbility {
            cost = Costs.SacrificeAnother(GameObjectFilter.Artifact)
            effect = Effects.DrawCards(1)
            timing = TimingRule.InstantSpeed
        }
    }
    val altar = card("Sac Test Altar") {
        manaCost = "{3}"
        typeLine = "Artifact"
        oracleText = "Sacrifice a creature: Add {C}{C}."
        activatedAbility {
            cost = Costs.Sacrifice(GameObjectFilter.Creature)
            effect = Effects.AddColorlessMana(2)
            manaAbility = true
        }
    }
    val selfSac = card("Self Sac Creature") {
        manaCost = "{1}"
        typeLine = "Creature — Spirit"
        power = 1
        toughness = 1
        oracleText = "Sacrifice this creature: Draw a card."
        activatedAbility {
            cost = Costs.SacrificeSelf
            effect = Effects.DrawCards(1)
            timing = TimingRule.InstantSpeed
        }
    }
    val emerger = card("Sac Test Emerger") {
        manaCost = "{5}{B}"
        typeLine = "Creature — Horror"
        power = 5
        toughness = 5
        oracleText = "Emerge {B}"
        emerge("{B}")
    }
    val greed = card("Sac Any Number Sorcery") {
        manaCost = "{B}"
        typeLine = "Sorcery"
        oracleText = "As an additional cost to cast this spell, you may sacrifice any number of creatures.\nDraw a card."
        additionalCost(Costs.additional.SacrificePermanents(GameObjectFilter.Creature))
        spell { effect = Effects.DrawCards(1) }
    }
    val soulblast = card("Sac All Sorcery") {
        manaCost = "{B}"
        typeLine = "Sorcery"
        oracleText = "As an additional cost to cast this spell, sacrifice all creatures you control.\nDraw a card."
        additionalCost(Costs.additional.SacrificeAll(GameObjectFilter.Creature))
        spell { effect = Effects.DrawCards(1) }
    }
    val plainLock = card("Plain Sacrifice Lock") {
        manaCost = "{3}"
        typeLine = "Enchantment"
        oracleText = "Creatures you control can't be sacrificed."
        staticAbility { ability = CantBeSacrificed(GroupFilter.AllCreaturesYouControl) }
    }
    val colorlessSpell = card("Two Colorless Sorcery") {
        manaCost = "{2}"
        typeLine = "Sorcery"
        oracleText = "Draw a card."
        spell { effect = Effects.DrawCards(1) }
    }

    fun newDriver(): Pair<GameTestDriver, EntityId> {
        val driver = GameTestDriver()
        driver.registerCards(
            TestCards.all + listOf(
                lock, bear, construct, trinket, splinters, edict, outlet, artifactOutlet, altar, selfSac, emerger, greed, soulblast, plainLock, colorlessSpell
            )
        )
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), skipMulligans = true, startingPlayer = 0)
        val me = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver to me
    }

    fun GameTestDriver.canCast(player: EntityId, cardId: EntityId) =
        legalActions(player).any { (it.action as? CastSpell)?.cardId == cardId && it.affordable }

    fun GameTestDriver.canActivate(player: EntityId, sourceId: EntityId) =
        legalActions(player).any { (it.action as? ActivateAbility)?.sourceId == sourceId && it.affordable }

    fun sacrificing(id: EntityId) = AdditionalCostPayment(sacrificedPermanents = listOf(id))

    test("a spell with a sacrifice-a-creature additional cost can't be cast, offered or submitted") {
        val (driver, me) = newDriver()
        driver.putLandOnBattlefield(me, "Swamp")
        val creature = driver.putCreatureOnBattlefield(me, "Sac Test Bear")
        val spell = driver.putCardInHand(me, "Sac Cost Sorcery")
        withClue("castable without the lock") { driver.canCast(me, spell) shouldBe true }

        driver.putPermanentOnBattlefield(driver.getOpponent(me), "Sacrifice Cost Lock")
        withClue("an opponent's lock binds every player") { driver.canCast(me, spell) shouldBe false }
        driver.submit(
            CastSpell(playerId = me, cardId = spell, additionalCostPayment = sacrificing(creature))
        ).error shouldNotBe null
        driver.findPermanent(me, "Sac Test Bear") shouldBe creature
    }

    test("an alternative cost that sacrifices a creature (emerge) can't be paid") {
        val (driver, me) = newDriver()
        driver.putLandOnBattlefield(me, "Swamp")
        val creature = driver.putCreatureOnBattlefield(me, "Sac Test Bear")
        val horror = driver.putCardInHand(me, "Sac Test Emerger")
        fun emergeOffered() = driver.legalActions(me).any {
            val action = it.action
            action is CastSpell && action.cardId == horror && action.alternativeCostType == AlternativeCostType.EMERGE && it.affordable
        }
        withClue("emerge is on offer without the lock") { emergeOffered() shouldBe true }

        driver.putPermanentOnBattlefield(me, "Sacrifice Cost Lock")
        emergeOffered() shouldBe false
        driver.submit(
            CastSpell(
                playerId = me, cardId = horror, useAlternativeCost = true,
                alternativeCostType = AlternativeCostType.EMERGE, additionalCostPayment = sacrificing(creature)
            )
        ).error shouldNotBe null
        driver.findPermanent(me, "Sac Test Bear") shouldBe creature
    }

    test("'sacrifice any number of creatures' offers none of them and refuses one") {
        val (driver, me) = newDriver()
        driver.putLandOnBattlefield(me, "Swamp")
        val creature = driver.putCreatureOnBattlefield(me, "Sac Test Bear")
        val spell = driver.putCardInHand(me, "Sac Any Number Sorcery")
        fun offer() = driver.legalActions(me).single { (it.action as? CastSpell)?.cardId == spell }
        withClue("the creature is on offer without the lock") {
            offer().additionalCostInfo?.validSacrificeTargets.orEmpty() shouldContain creature
        }

        driver.putPermanentOnBattlefield(me, "Sacrifice Cost Lock")
        withClue("choosing none still pays it, so the spell stays castable — without the creature") {
            offer().affordable shouldBe true
            offer().additionalCostInfo?.validSacrificeTargets.orEmpty() shouldNotContain creature
        }
        driver.submit(
            CastSpell(
                playerId = me, cardId = spell,
                additionalCostPayment = AdditionalCostPayment(variableCostPermanents = listOf(creature)),
            )
        ).error shouldNotBe null
        driver.findPermanent(me, "Sac Test Bear") shouldBe creature
    }

    test("'sacrifice all creatures you control' can't be paid while you control a creature") {
        val (driver, me) = newDriver()
        driver.putPermanentOnBattlefield(me, "Sacrifice Cost Lock")
        driver.putLandOnBattlefield(me, "Swamp")
        val spell = driver.putCardInHand(me, "Sac All Sorcery")
        withClue("controlling none sacrifices nothing, so it is payable") { driver.canCast(me, spell) shouldBe true }

        val creature = driver.putCreatureOnBattlefield(me, "Sac Test Bear")
        driver.canCast(me, spell) shouldBe false
        driver.submit(CastSpell(playerId = me, cardId = spell)).error shouldNotBe null
        driver.findPermanent(me, "Sac Test Bear") shouldBe creature
    }

    test("a permanent that can't be sacrificed at all can't pay a sacrifice cost either") {
        val (driver, me) = newDriver()
        val outletId = driver.putPermanentOnBattlefield(me, "Creature Sac Outlet")
        val creature = driver.putCreatureOnBattlefield(me, "Sac Test Bear")
        val abilityId = outlet.activatedAbilities.single().id
        driver.putPermanentOnBattlefield(me, "Plain Sacrifice Lock")

        driver.canActivate(me, outletId) shouldBe false
        driver.submit(
            ActivateAbility(playerId = me, sourceId = outletId, abilityId = abilityId, costPayment = sacrificing(creature))
        ).error shouldNotBe null
        driver.findPermanent(me, "Sac Test Bear") shouldBe creature
    }

    test("an activated ability with a sacrifice-a-creature cost can't be activated") {
        val (driver, me) = newDriver()
        val outletId = driver.putPermanentOnBattlefield(me, "Creature Sac Outlet")
        val creature = driver.putCreatureOnBattlefield(me, "Sac Test Bear")
        val abilityId = outlet.activatedAbilities.single().id
        withClue("activatable without the lock") { driver.canActivate(me, outletId) shouldBe true }

        driver.putPermanentOnBattlefield(me, "Sacrifice Cost Lock")
        driver.canActivate(me, outletId) shouldBe false
        driver.submit(
            ActivateAbility(playerId = me, sourceId = outletId, abilityId = abilityId, costPayment = sacrificing(creature))
        ).error shouldNotBe null
        withClue("nor with the creature left for the engine to auto-pick") {
            driver.submit(ActivateAbility(playerId = me, sourceId = outletId, abilityId = abilityId)).error shouldNotBe null
        }
        driver.findPermanent(me, "Sac Test Bear") shouldBe creature
    }

    test("'Sacrifice this creature' can't be paid either") {
        val (driver, me) = newDriver()
        val spirit = driver.putCreatureOnBattlefield(me, "Self Sac Creature")
        val abilityId = selfSac.activatedAbilities.single().id
        withClue("activatable without the lock") { driver.canActivate(me, spirit) shouldBe true }

        driver.putPermanentOnBattlefield(me, "Sacrifice Cost Lock")
        driver.canActivate(me, spirit) shouldBe false
        driver.submit(ActivateAbility(playerId = me, sourceId = spirit, abilityId = abilityId)).error shouldNotBe null
        driver.findPermanent(me, "Self Sac Creature") shouldBe spirit
    }

    test("a mana ability that sacrifices a creature can't be activated, directly or by auto-pay") {
        val (driver, me) = newDriver()
        val altarId = driver.putPermanentOnBattlefield(me, "Sac Test Altar")
        val creature = driver.putCreatureOnBattlefield(me, "Sac Test Bear")
        val abilityId = altar.activatedAbilities.single().id
        val spell = driver.putCardInHand(me, "Two Colorless Sorcery")
        withClue("the altar pays for a spell without the lock") { driver.canCast(me, spell) shouldBe true }

        driver.putPermanentOnBattlefield(me, "Sacrifice Cost Lock")
        withClue("auto-pay can't lean on the altar") { driver.canCast(me, spell) shouldBe false }
        driver.canActivate(me, altarId) shouldBe false
        driver.submit(
            ActivateAbility(playerId = me, sourceId = altarId, abilityId = abilityId, costPayment = sacrificing(creature))
        ).error shouldNotBe null
        driver.findPermanent(me, "Sac Test Bear") shouldBe creature
        driver.state.getEntity(me)!!.get<ManaPoolComponent>()!!.colorless shouldBe 0
    }

    test("'Sacrifice an artifact' takes a noncreature artifact, never an artifact creature") {
        val (driver, me) = newDriver()
        val outletId = driver.putPermanentOnBattlefield(me, "Artifact Sac Outlet")
        val construct = driver.putCreatureOnBattlefield(me, "Sac Test Construct")
        val abilityId = artifactOutlet.activatedAbilities.single().id
        driver.putPermanentOnBattlefield(me, "Sacrifice Cost Lock")
        withClue("only an artifact creature to give: unpayable") { driver.canActivate(me, outletId) shouldBe false }

        val trinket = driver.putPermanentOnBattlefield(me, "Sac Test Trinket")
        val offer = driver.legalActions(me).single { (it.action as? ActivateAbility)?.sourceId == outletId }
        offer.affordable shouldBe true
        withClue("the artifact creature is not on offer") {
            offer.additionalCostInfo?.validSacrificeTargets.orEmpty() shouldNotContain construct
        }
        driver.submit(
            ActivateAbility(playerId = me, sourceId = outletId, abilityId = abilityId, costPayment = sacrificing(construct))
        ).error shouldNotBe null
        driver.findPermanent(me, "Sac Test Construct") shouldBe construct

        val hand = driver.getHandSize(me)
        driver.submitSuccess(
            ActivateAbility(playerId = me, sourceId = outletId, abilityId = abilityId, costPayment = sacrificing(trinket))
        )
        driver.getGraveyard(me) shouldContain trinket
        driver.bothPass()
        driver.getHandSize(me) shouldBe hand + 1
    }

    test("a resolving spell may still make a player sacrifice a creature") {
        val (driver, me) = newDriver()
        driver.putPermanentOnBattlefield(me, "Sacrifice Cost Lock")
        driver.putLandOnBattlefield(me, "Swamp")
        val creature = driver.putCreatureOnBattlefield(me, "Sac Test Bear")
        val spell = driver.putCardInHand(me, "Sac Effect Sorcery")

        driver.submitSuccess(CastSpell(playerId = me, cardId = spell))
        driver.bothPass()
        driver.getGraveyard(me) shouldContain creature
    }

    test("the prohibition ends when its source leaves the battlefield") {
        val (driver, me) = newDriver()
        val lockId = driver.putPermanentOnBattlefield(me, "Sacrifice Cost Lock")
        val outletId = driver.putPermanentOnBattlefield(me, "Creature Sac Outlet")
        val creature = driver.putCreatureOnBattlefield(me, "Sac Test Bear")
        val abilityId = outlet.activatedAbilities.single().id
        driver.canActivate(me, outletId) shouldBe false

        driver.moveToGraveyard(lockId)
        driver.canActivate(me, outletId) shouldBe true
        driver.submitSuccess(
            ActivateAbility(playerId = me, sourceId = outletId, abilityId = abilityId, costPayment = sacrificing(creature))
        )
        driver.getGraveyard(me) shouldContain creature
    }
})
