package com.wingedsheep.engine.mechanics.targeting

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.GameAction
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.CardsInZonesCantBeTargeted
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.LoseAllAbilities
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * [CardsInZonesCantBeTargeted] — "Cards in graveyards can't be the targets of spells or
 * abilities" (Ground Seal; Dennick, Pious Apprentice). Pins every targeting site — legal-target
 * enumeration, cast/activation validation, the resolution re-check — plus the zone and filter
 * axes, that it binds its own controller, and that it stops when its holder loses its abilities.
 */
class CardsInZonesCantBeTargetedTest : FunSpec({

    val seal = card("Test Graveyard Seal") {
        manaCost = "{1}"
        typeLine = "Creature — Spirit"
        power = 1
        toughness = 1
        staticAbility { ability = CardsInZonesCantBeTargeted() }
    }
    val creatureSeal = card("Test Creature Seal") {
        manaCost = "{1}"
        typeLine = "Enchantment"
        staticAbility { ability = CardsInZonesCantBeTargeted(filter = GameObjectFilter.Creature) }
    }
    val exileSeal = card("Test Exile Seal") {
        manaCost = "{1}"
        typeLine = "Enchantment"
        staticAbility { ability = CardsInZonesCantBeTargeted(zones = setOf(Zone.EXILE)) }
    }
    val silencer = card("Test Silencer") {
        manaCost = "{1}"
        typeLine = "Enchantment"
        staticAbility { ability = LoseAllAbilities(GroupFilter(GameObjectFilter.Creature)) }
    }
    val raise = card("Test Raise") {
        manaCost = "{1}"
        typeLine = "Sorcery"
        spell {
            val t = target(TargetFilter.CardInGraveyard)
            effect = Effects.ReturnToHandFromGraveyard(t)
        }
    }
    val crypt = card("Test Crypt Shovel") {
        manaCost = "{1}"
        typeLine = "Artifact"
        activatedAbility {
            cost = Costs.Mana("{1}")
            val t = target(TargetFilter.CardInGraveyard)
            effect = Effects.Exile(t)
        }
    }
    val murder = card("Test Seal Murder") {
        manaCost = "{1}"
        typeLine = "Instant"
        spell {
            val t = target(TargetFilter.Creature)
            effect = Effects.Destroy(t)
        }
    }

    fun driver() = GameTestDriver().also {
        it.registerCards(TestCards.all + listOf(seal, creatureSeal, exileSeal, silencer, raise, crypt, murder))
        it.initMirrorMatch(Deck.of("Forest" to 40), skipMulligans = true, startingPlayer = 0)
        it.passPriorityUntil(Step.PRECOMBAT_MAIN)
    }
    val shovelId = crypt.script.activatedAbilities.single().id

    fun GameTestDriver.offeredTargets(player: EntityId, matches: (GameAction) -> Boolean) =
        legalActions(player).first { matches(it.action) }.let { la ->
            la.targetRequirements?.firstOrNull()?.validTargets ?: la.validTargets.orEmpty()
        }
    fun GameTestDriver.graveTarget(owner: EntityId, cardId: EntityId) =
        listOf(ChosenTarget.Card(cardId, owner, Zone.GRAVEYARD))

    test("description renders the oracle phrasing") {
        CardsInZonesCantBeTargeted().description shouldBe
            "Cards in graveyards can't be the targets of spells or abilities"
        CardsInZonesCantBeTargeted(filter = GameObjectFilter.Creature).description shouldBe
            "Creature cards in graveyards can't be the targets of spells or abilities"
    }

    test("a spell can't target a card in a graveyard, and isn't offered one") {
        val d = driver()
        d.putPermanentOnBattlefield(d.player2, seal.name)
        val bear = d.putCardInGraveyard(d.player1, "Grizzly Bears")
        val spell = d.putCardInHand(d.player1, raise.name)
        d.giveColorlessMana(d.player1, 1)
        d.legalActions(d.player1).none { (it.action as? CastSpell)?.cardId == spell } shouldBe true
        d.castSpellWithTargets(d.player1, spell, d.graveTarget(d.player1, bear)).error shouldNotBe null
    }

    test("without the lock the same spell targets it") {
        val d = driver()
        val bear = d.putCardInGraveyard(d.player1, "Grizzly Bears")
        val spell = d.putCardInHand(d.player1, raise.name)
        d.giveColorlessMana(d.player1, 1)
        d.offeredTargets(d.player1) { it is CastSpell && it.cardId == spell }.contains(bear) shouldBe true
        d.castSpellWithTargets(d.player1, spell, d.graveTarget(d.player1, bear)).error shouldBe null
        d.bothPass()
        d.getHand(d.player1).contains(bear) shouldBe true
    }

    test("an ability can't target one either — its controller's own graveyard included") {
        val d = driver()
        d.putPermanentOnBattlefield(d.player1, seal.name)
        val own = d.putCardInGraveyard(d.player1, "Grizzly Bears")
        val theirs = d.putCardInGraveyard(d.player2, "Grizzly Bears")
        val shovel = d.putPermanentOnBattlefield(d.player1, crypt.name)
        d.giveColorlessMana(d.player1, 1)
        d.legalActions(d.player1).none { (it.action as? ActivateAbility)?.sourceId == shovel } shouldBe true
        d.submit(ActivateAbility(d.player1, shovel, shovelId, d.graveTarget(d.player1, own))).error shouldNotBe null
        d.submit(ActivateAbility(d.player1, shovel, shovelId, d.graveTarget(d.player2, theirs))).error shouldNotBe null
    }

    test("permanents on the battlefield stay targetable") {
        val d = driver()
        d.putPermanentOnBattlefield(d.player1, seal.name)
        val bear = d.putCreatureOnBattlefield(d.player2, "Grizzly Bears")
        val spell = d.putCardInHand(d.player1, murder.name)
        d.giveColorlessMana(d.player1, 1)
        d.castSpell(d.player1, spell, listOf(bear)).error shouldBe null
    }

    test("a lock arriving after targeting makes the target illegal on resolution (CR 608.2b)") {
        val d = driver()
        val bear = d.putCardInGraveyard(d.player1, "Grizzly Bears")
        val spell = d.putCardInHand(d.player1, raise.name)
        d.giveColorlessMana(d.player1, 1)
        d.castSpellWithTargets(d.player1, spell, d.graveTarget(d.player1, bear)).error shouldBe null
        d.putPermanentOnBattlefield(d.player2, seal.name)
        d.bothPass()
        d.getGraveyard(d.player1).contains(bear) shouldBe true
        d.getHand(d.player1).contains(bear) shouldBe false
    }

    test("the filter axis: a creature-only lock leaves other cards targetable") {
        val d = driver()
        d.putPermanentOnBattlefield(d.player2, creatureSeal.name)
        val bear = d.putCardInGraveyard(d.player1, "Grizzly Bears")
        val forest = d.putCardInGraveyard(d.player1, "Forest")
        val spell = d.putCardInHand(d.player1, raise.name)
        d.giveColorlessMana(d.player1, 1)
        val offered = d.offeredTargets(d.player1) { it is CastSpell && it.cardId == spell }
        offered.contains(bear) shouldBe false
        offered.contains(forest) shouldBe true
    }

    test("the zone axis: an exile lock doesn't reach graveyards") {
        val d = driver()
        d.putPermanentOnBattlefield(d.player2, exileSeal.name)
        val bear = d.putCardInGraveyard(d.player1, "Grizzly Bears")
        val spell = d.putCardInHand(d.player1, raise.name)
        d.giveColorlessMana(d.player1, 1)
        d.castSpellWithTargets(d.player1, spell, d.graveTarget(d.player1, bear)).error shouldBe null
    }

    test("a holder that loses all its abilities no longer locks graveyards") {
        val d = driver()
        d.putCreatureOnBattlefield(d.player2, seal.name)
        d.putPermanentOnBattlefield(d.player2, silencer.name)
        val bear = d.putCardInGraveyard(d.player1, "Grizzly Bears")
        val spell = d.putCardInHand(d.player1, raise.name)
        d.giveColorlessMana(d.player1, 1)
        d.castSpellWithTargets(d.player1, spell, d.graveTarget(d.player1, bear)).error shouldBe null
    }
})
