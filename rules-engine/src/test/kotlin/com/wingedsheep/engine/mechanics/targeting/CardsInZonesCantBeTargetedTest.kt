package com.wingedsheep.engine.mechanics.targeting

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.GameAction
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
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
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * [CardsInZonesCantBeTargeted] — "Cards in graveyards can't be the targets of spells or
 * abilities" (Ground Seal; Dennick, Pious Apprentice). Pins every targeting site — legal-target
 * enumeration, cast/activation validation, the resolution re-check — plus the zone and filter
 * axes, that it binds its own controller, and that it stops when its holder loses its abilities.
 * The spell-only axis and the player-held "this turn" grant (Display of Dominance) close it out.
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

    // Tomik, Distinguished Advokist's shape: lands anywhere on the battlefield and land cards in
    // graveyards, scoped to spells and abilities the lock's controller's opponents control.
    val landWard = card("Test Land Ward") {
        manaCost = "{1}"
        typeLine = "Enchantment"
        staticAbility {
            ability = CardsInZonesCantBeTargeted(
                zones = setOf(Zone.BATTLEFIELD, Zone.GRAVEYARD),
                filter = GameObjectFilter.Land,
                sourceController = Player.EachOpponent
            )
        }
    }
    val stoneRain = card("Test Seal Stone Rain") {
        manaCost = "{1}"
        typeLine = "Sorcery"
        spell {
            val t = target(TargetFilter.Land)
            effect = Effects.Destroy(t)
        }
    }

    // Display of Dominance's second mode: a turn-long rule handed to the caster, stopping only blue
    // or black spells an opponent controls from targeting the caster's permanents.
    val blueOrBlack = GameObjectFilter.Any.withAnyColor(Color.BLUE, Color.BLACK)
    val shieldLock = CardsInZonesCantBeTargeted(
        zones = setOf(Zone.BATTLEFIELD),
        filter = GameObjectFilter.Permanent.youControl(),
        sourceController = Player.EachOpponent,
        spellFilter = blueOrBlack
    )
    val shield = card("Test Dominance Shield") {
        manaCost = "{1}"
        typeLine = "Instant"
        spell { effect = Effects.GrantStaticAbility(shieldLock, EffectTarget.Controller) }
    }
    val blueMurder = card("Test Blue Murder") {
        manaCost = "{U}"
        typeLine = "Instant"
        spell {
            val t = target(TargetFilter.Creature)
            effect = Effects.Destroy(t)
        }
    }
    val pinger = card("Test Blue Pinger") {
        manaCost = "{U}"
        typeLine = "Artifact"
        activatedAbility {
            cost = Costs.Mana("{1}")
            val t = target(TargetFilter.Creature)
            effect = Effects.DealDamage(1, t)
        }
    }
    // A printed spell-only lock, so the axis is pinned apart from the player-held grant.
    val spellWard = card("Test Spell Ward") {
        manaCost = "{1}"
        typeLine = "Enchantment"
        staticAbility {
            ability = CardsInZonesCantBeTargeted(
                zones = setOf(Zone.BATTLEFIELD),
                filter = GameObjectFilter.Creature,
                spellFilter = GameObjectFilter.Any
            )
        }
    }

    fun driver() = GameTestDriver().also {
        it.registerCards(
            TestCards.all + listOf(
                seal, creatureSeal, exileSeal, silencer, raise, crypt, murder, landWard, stoneRain,
                shield, blueMurder, pinger, spellWard
            )
        )
        it.initMirrorMatch(Deck.of("Forest" to 40), skipMulligans = true, startingPlayer = 0)
        it.passPriorityUntil(Step.PRECOMBAT_MAIN)
    }
    val shovelId = crypt.script.activatedAbilities.single().id
    val pingId = pinger.script.activatedAbilities.single().id

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
        CardsInZonesCantBeTargeted(
            zones = setOf(Zone.BATTLEFIELD, Zone.GRAVEYARD), filter = GameObjectFilter.Land,
            sourceController = Player.EachOpponent
        ).description shouldBe "Lands on the battlefield and land cards in graveyards can't be the targets of " +
            "spells or abilities your opponents control"
        shieldLock.description shouldBe
            "Permanents you control can't be the targets of blue or black spells your opponents control"
        CardsInZonesCantBeTargeted(zones = setOf(Zone.BATTLEFIELD), spellFilter = GameObjectFilter.Any)
            .description shouldBe "Permanents can't be the targets of spells"
    }

    test("spell-only axis: spells can't target the creature, abilities still can") {
        val d = driver()
        d.putPermanentOnBattlefield(d.player2, spellWard.name)
        val bear = d.putCreatureOnBattlefield(d.player2, "Grizzly Bears")
        val spell = d.putCardInHand(d.player1, murder.name)
        val pingerId = d.putPermanentOnBattlefield(d.player1, pinger.name)
        d.giveColorlessMana(d.player1, 2)
        d.castSpell(d.player1, spell, listOf(bear)).error shouldNotBe null
        d.offeredTargets(d.player1) { (it as? ActivateAbility)?.sourceId == pingerId }.contains(bear) shouldBe true
        d.submit(ActivateAbility(d.player1, pingerId, pingId, listOf(ChosenTarget.Permanent(bear)))).error shouldBe null
    }

    fun GameTestDriver.raiseShield(holder: EntityId) {
        val spell = putCardInHand(holder, shield.name)
        giveColorlessMana(holder, 1)
        castSpell(holder, spell, emptyList()).error shouldBe null
        bothPass()
    }

    test("player-held grant: an opponent's blue spell can't target the holder's permanents, a colourless one can") {
        val d = driver()
        val bear = d.putCreatureOnBattlefield(d.player1, "Grizzly Bears")
        val theirBear = d.putCreatureOnBattlefield(d.player2, "Grizzly Bears")
        d.raiseShield(d.player1)
        UntargetableByRules.zoneLocks(d.state).single().controllerId shouldBe d.player1

        val blue = d.putCardInHand(d.player1, blueMurder.name)
        d.giveMana(d.player1, Color.BLUE, 1)
        withClue("the holder's own blue spell still targets their own creature") {
            d.offeredTargets(d.player1) { it is CastSpell && it.cardId == blue }.contains(bear) shouldBe true
        }
        withClue("the opponent's permanents aren't covered") {
            d.offeredTargets(d.player1) { it is CastSpell && it.cardId == blue }.contains(theirBear) shouldBe true
        }
    }

    test("player-held grant binds the opponent's blue spell — and covers a permanent arriving later — but not a colourless spell or a blue ability") {
        val d = driver()
        d.raiseShield(d.player1)
        val lateBear = d.putCreatureOnBattlefield(d.player1, "Grizzly Bears")
        d.passPriorityUntil(Step.END)
        // Hand the opponent priority within the holder's turn via the end step's priority window.
        if (d.state.priorityPlayerId == d.player1) d.passPriority(d.player1)

        val blue = d.putCardInHand(d.player2, blueMurder.name)
        val colourless = d.putCardInHand(d.player2, murder.name)
        val pingerId = d.putPermanentOnBattlefield(d.player2, pinger.name)
        d.giveMana(d.player2, Color.BLUE, 1)
        d.giveColorlessMana(d.player2, 2)
        d.castSpell(d.player2, blue, listOf(lateBear)).error shouldNotBe null
        d.offeredTargets(d.player2) { (it as? ActivateAbility)?.sourceId == pingerId }.contains(lateBear) shouldBe true
        d.castSpell(d.player2, colourless, listOf(lateBear)).error shouldBe null
    }

    test("player-held grant makes an earlier blue spell's target illegal on resolution, and expires at end of turn") {
        val d = driver()
        val bear = d.putCreatureOnBattlefield(d.player1, "Grizzly Bears")
        d.passPriority(d.player1)
        val blue = d.putCardInHand(d.player2, blueMurder.name)
        d.giveMana(d.player2, Color.BLUE, 1)
        d.castSpell(d.player2, blue, listOf(bear)).error shouldBe null
        d.passPriority(d.player2)
        val shieldSpell = d.putCardInHand(d.player1, shield.name)
        d.giveColorlessMana(d.player1, 1)
        d.castSpell(d.player1, shieldSpell, emptyList()).error shouldBe null
        d.bothPass()
        d.bothPass()
        d.state.getBattlefield().contains(bear) shouldBe true

        d.passPriorityUntil(Step.UPKEEP, activePlayer = d.player2)
        UntargetableByRules.zoneLocks(d.state) shouldBe emptyList()
    }

    test("opponent scope: an opponent's spell can't target any land on the battlefield, theirs included") {
        val d = driver()
        d.putPermanentOnBattlefield(d.player2, landWard.name)
        val wardersLand = d.putLandOnBattlefield(d.player2, "Forest")
        val ownLand = d.putLandOnBattlefield(d.player1, "Forest")
        val spell = d.putCardInHand(d.player1, stoneRain.name)
        d.giveColorlessMana(d.player1, 1)
        d.legalActions(d.player1).none { (it.action as? CastSpell)?.cardId == spell } shouldBe true
        d.castSpell(d.player1, spell, listOf(wardersLand)).error shouldNotBe null
        d.castSpell(d.player1, spell, listOf(ownLand)).error shouldNotBe null
    }

    test("opponent scope: the lock's controller still targets lands, anyone's") {
        val d = driver()
        d.putPermanentOnBattlefield(d.player1, landWard.name)
        val theirLand = d.putLandOnBattlefield(d.player2, "Forest")
        val ownLand = d.putLandOnBattlefield(d.player1, "Forest")
        val spell = d.putCardInHand(d.player1, stoneRain.name)
        d.giveColorlessMana(d.player1, 1)
        val offered = d.offeredTargets(d.player1) { it is CastSpell && it.cardId == spell }
        offered.contains(theirLand) shouldBe true
        offered.contains(ownLand) shouldBe true
        d.castSpell(d.player1, spell, listOf(theirLand)).error shouldBe null
        d.bothPass()
        d.getGraveyard(d.player2).contains(theirLand) shouldBe true
    }

    test("opponent scope: an opponent's ability can't target a land card in a graveyard, other cards it can") {
        val d = driver()
        d.putPermanentOnBattlefield(d.player2, landWard.name)
        val forest = d.putCardInGraveyard(d.player2, "Forest")
        val bear = d.putCardInGraveyard(d.player2, "Grizzly Bears")
        val shovel = d.putPermanentOnBattlefield(d.player1, crypt.name)
        d.giveColorlessMana(d.player1, 1)
        val offered = d.offeredTargets(d.player1) { (it as? ActivateAbility)?.sourceId == shovel }
        offered.contains(forest) shouldBe false
        offered.contains(bear) shouldBe true
        d.submit(ActivateAbility(d.player1, shovel, shovelId, d.graveTarget(d.player2, forest))).error shouldNotBe null
        d.submit(ActivateAbility(d.player1, shovel, shovelId, d.graveTarget(d.player2, bear))).error shouldBe null
    }

    test("opponent scope: non-land permanents stay targetable by opponents") {
        val d = driver()
        d.putPermanentOnBattlefield(d.player2, landWard.name)
        val bear = d.putCreatureOnBattlefield(d.player2, "Grizzly Bears")
        val spell = d.putCardInHand(d.player1, murder.name)
        d.giveColorlessMana(d.player1, 1)
        d.castSpell(d.player1, spell, listOf(bear)).error shouldBe null
    }

    test("a battlefield lock arriving after targeting makes a land target illegal on resolution (CR 608.2b)") {
        val d = driver()
        val land = d.putLandOnBattlefield(d.player2, "Forest")
        val spell = d.putCardInHand(d.player1, stoneRain.name)
        d.giveColorlessMana(d.player1, 1)
        d.castSpell(d.player1, spell, listOf(land)).error shouldBe null
        d.putPermanentOnBattlefield(d.player2, landWard.name)
        d.bothPass()
        d.state.getBattlefield().contains(land) shouldBe true
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
