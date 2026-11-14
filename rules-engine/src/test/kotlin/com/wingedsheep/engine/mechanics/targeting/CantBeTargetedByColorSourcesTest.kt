package com.wingedsheep.engine.mechanics.targeting

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.GameAction
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.CantBeTargetedBy
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ProtectionScope
import com.wingedsheep.sdk.scripting.UntargetableBy
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.text.TextReplacer
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * [UntargetableBy.SourcesOfColor] — "can't be the target of nongreen spells or abilities from
 * nongreen sources" (Gaea's Revenge) and "… of white spells or abilities from white sources" (Rebel
 * Informer). Pins spells and abilities at every targeting site, colorless and multicolored sources
 * (CR 105.2), that it binds its own controller (unlike hexproof from a quality), and that it is only
 * a targeting restriction (not protection).
 */
class CantBeTargetedByColorSourcesTest : FunSpec({

    val revenant = card("Test Nongreen-Proof Giant") {
        manaCost = "{5}{G}{G}"
        typeLine = "Creature — Elemental"
        power = 8
        toughness = 5
        staticAbility { ability = CantBeTargetedBy(UntargetableBy.SourcesOfColor(ProtectionScope.NonColor(Color.GREEN))) }
    }
    val informer = card("Test White-Proof Informer") {
        manaCost = "{2}{B}"
        typeLine = "Creature — Human Rebel Mercenary"
        power = 1
        toughness = 1
        staticAbility { ability = CantBeTargetedBy(UntargetableBy.SourcesOfColor(ProtectionScope.Color(Color.WHITE))) }
    }
    val greenPinger = card("Test Green Pinger") {
        manaCost = "{G}"
        typeLine = "Creature — Elf"
        power = 1
        toughness = 1
        activatedAbility {
            cost = Costs.Mana("{1}")
            val t = target(Targets.Any)
            effect = Effects.DealDamage(1, t)
        }
    }
    val artifactPinger = card("Test Colorless Pinger") {
        manaCost = "{1}"
        typeLine = "Artifact"
        activatedAbility {
            cost = Costs.Mana("{1}")
            val t = target(Targets.Any)
            effect = Effects.DealDamage(1, t)
        }
    }
    val whitePinger = card("Test White Pinger") {
        manaCost = "{W}"
        typeLine = "Creature — Human"
        power = 1
        toughness = 1
        activatedAbility {
            cost = Costs.Mana("{1}")
            val t = target(Targets.Any)
            effect = Effects.DealDamage(1, t)
        }
    }
    val selesnyaShot = card("Test Selesnya Shot") {
        manaCost = "{G}{W}"
        typeLine = "Instant"
        spell {
            val t = target(Targets.Any)
            effect = Effects.DealDamage(1, t)
        }
    }
    val whiteShot = card("Test White Shot") {
        manaCost = "{W}"
        typeLine = "Instant"
        spell {
            val t = target(Targets.Any)
            effect = Effects.DealDamage(1, t)
        }
    }
    val redSweep = card("Test Red Sweep") {
        manaCost = "{R}"
        typeLine = "Sorcery"
        spell { effect = Effects.ForEachInGroup(GroupFilter(GameObjectFilter.Creature), Effects.DealDamage(8, EffectTarget.IterationEntity)) }
    }
    // "Creatures you control can't be the target of nongreen spells …" — adds the restriction after
    // a spell has already targeted.
    val nongreenWardLord = card("Test Nongreen Ward Lord") {
        manaCost = "{1}"
        typeLine = "Enchantment"
        staticAbility {
            ability = CantBeTargetedBy(
                UntargetableBy.SourcesOfColor(ProtectionScope.NonColor(Color.GREEN)),
                GroupFilter(GameObjectFilter.Creature.youControl())
            )
        }
    }

    fun driver() = GameTestDriver().also {
        it.registerCards(
            TestCards.all + listOf(
                revenant, informer, greenPinger, artifactPinger, whitePinger, selesnyaShot, whiteShot, redSweep, nongreenWardLord
            )
        )
        it.initMirrorMatch(Deck.of("Forest" to 40), skipMulligans = true, startingPlayer = 0)
        it.passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun GameTestDriver.offeredTargets(player: EntityId, matches: (GameAction) -> Boolean) =
        legalActions(player).first { matches(it.action) }.let { la ->
            la.targetRequirements?.firstOrNull()?.validTargets ?: la.validTargets.orEmpty()
        }
    fun GameTestDriver.offeredToCast(player: EntityId, cardId: EntityId) =
        offeredTargets(player) { it is CastSpell && it.cardId == cardId }
    fun GameTestDriver.offeredToActivate(player: EntityId, sourceId: EntityId) =
        offeredTargets(player) { it is ActivateAbility && it.sourceId == sourceId }
    fun abilityOf(def: com.wingedsheep.sdk.model.CardDefinition) = def.script.activatedAbilities.single().id

    test("description renders the oracle phrasing") {
        CantBeTargetedBy(UntargetableBy.SourcesOfColor(ProtectionScope.NonColor(Color.GREEN))).description shouldBe
            "This creature can't be the target of nongreen spells or abilities from nongreen sources"
        CantBeTargetedBy(UntargetableBy.SourcesOfColor(ProtectionScope.Color(Color.WHITE))).description shouldBe
            "This creature can't be the target of white spells or abilities from white sources"
    }

    test("only a colour or non-colour scope is accepted") {
        shouldThrow<IllegalArgumentException> { UntargetableBy.SourcesOfColor(ProtectionScope.Multicolored) }
    }

    test("a text-changing effect rewrites the colour word") {
        val toBlue = object : TextReplacer {
            override fun replaceCreatureType(subtype: String) = subtype
            override fun replaceSubtype(subtype: com.wingedsheep.sdk.core.Subtype) = subtype
            override fun replaceColor(color: Color) = if (color == Color.GREEN) Color.BLUE else color
        }
        UntargetableBy.SourcesOfColor(ProtectionScope.NonColor(Color.GREEN)).applyTextReplacement(toBlue) shouldBe
            UntargetableBy.SourcesOfColor(ProtectionScope.NonColor(Color.BLUE))
    }

    context("nongreen") {
        test("an opponent's red spell can't target it and it isn't offered; a green spell can") {
            val d = driver()
            val giant = d.putCreatureOnBattlefield(d.player2, revenant.name)
            val bear = d.putCreatureOnBattlefield(d.player2, "Grizzly Bears")
            val bolt = d.putCardInHand(d.player1, "Lightning Bolt")
            d.giveMana(d.player1, Color.RED, 1)
            val offered = d.offeredToCast(d.player1, bolt)
            offered.contains(giant) shouldBe false
            offered.contains(bear) shouldBe true
            d.castSpell(d.player1, bolt, listOf(giant)).error shouldNotBe null

            val growth = d.putCardInHand(d.player1, "Giant Growth")
            d.giveMana(d.player1, Color.GREEN, 1)
            d.offeredToCast(d.player1, growth).contains(giant) shouldBe true
            d.castSpell(d.player1, growth, listOf(giant)).error shouldBe null
        }

        test("not controller-gated: its own controller's nongreen spell can't target it either") {
            val d = driver()
            val giant = d.putCreatureOnBattlefield(d.player1, revenant.name)
            d.putCreatureOnBattlefield(d.player1, "Grizzly Bears")
            val bolt = d.putCardInHand(d.player1, "Lightning Bolt")
            d.giveMana(d.player1, Color.RED, 1)
            d.offeredToCast(d.player1, bolt).contains(giant) shouldBe false
            d.castSpell(d.player1, bolt, listOf(giant)).error shouldNotBe null
        }

        test("a multicolored spell that is green can target it (it isn't nongreen)") {
            val d = driver()
            val giant = d.putCreatureOnBattlefield(d.player2, revenant.name)
            val shot = d.putCardInHand(d.player1, selesnyaShot.name)
            d.giveMana(d.player1, Color.GREEN, 1)
            d.giveMana(d.player1, Color.WHITE, 1)
            d.offeredToCast(d.player1, shot).contains(giant) shouldBe true
            d.castSpell(d.player1, shot, listOf(giant)).error shouldBe null
        }

        test("an ability from a colorless source can't target it; one from a green source can") {
            val d = driver()
            val giant = d.putCreatureOnBattlefield(d.player2, revenant.name)
            val artifact = d.putPermanentOnBattlefield(d.player1, artifactPinger.name)
            val elf = d.putCreatureOnBattlefield(d.player1, greenPinger.name)
            d.giveColorlessMana(d.player1, 2)
            d.offeredToActivate(d.player1, artifact).contains(giant) shouldBe false
            d.submit(ActivateAbility(d.player1, artifact, abilityOf(artifactPinger), listOf(ChosenTarget.Permanent(giant))))
                .error shouldNotBe null
            d.offeredToActivate(d.player1, elf).contains(giant) shouldBe true
            d.submit(ActivateAbility(d.player1, elf, abilityOf(greenPinger), listOf(ChosenTarget.Permanent(giant))))
                .error shouldBe null
        }

        test("gaining the restriction after being targeted makes the nongreen spell fizzle on resolution") {
            val d = driver()
            val bear = d.putCreatureOnBattlefield(d.player2, "Grizzly Bears")
            val bolt = d.putCardInHand(d.player1, "Lightning Bolt")
            d.giveMana(d.player1, Color.RED, 1)
            d.castSpell(d.player1, bolt, listOf(bear)).error shouldBe null
            d.putPermanentOnBattlefield(d.player2, nongreenWardLord.name)
            d.bothPass()
            d.state.getBattlefield().contains(bear) shouldBe true
            d.getGraveyardCardNames(d.player1).contains("Lightning Bolt") shouldBe true
        }

        test("only a targeting restriction: untargeted damage from a nongreen spell still kills it") {
            val d = driver()
            val giant = d.putCreatureOnBattlefield(d.player2, revenant.name)
            val sweep = d.putCardInHand(d.player1, redSweep.name)
            d.giveMana(d.player1, Color.RED, 1)
            d.castSpell(d.player1, sweep, emptyList()).error shouldBe null
            d.bothPass()
            d.state.getBattlefield().contains(giant) shouldBe false
        }
    }

    context("a named colour") {
        test("a white spell and an ability from a white source can't target it; nonwhite ones can") {
            val d = driver()
            val rebel = d.putCreatureOnBattlefield(d.player2, informer.name)
            d.putCreatureOnBattlefield(d.player2, "Grizzly Bears")
            val shot = d.putCardInHand(d.player1, whiteShot.name)
            val white = d.putCreatureOnBattlefield(d.player1, whitePinger.name)
            val artifact = d.putPermanentOnBattlefield(d.player1, artifactPinger.name)
            d.giveMana(d.player1, Color.WHITE, 1)
            d.giveColorlessMana(d.player1, 2)
            d.offeredToCast(d.player1, shot).contains(rebel) shouldBe false
            d.castSpell(d.player1, shot, listOf(rebel)).error shouldNotBe null
            d.offeredToActivate(d.player1, white).contains(rebel) shouldBe false
            d.offeredToActivate(d.player1, artifact).contains(rebel) shouldBe true

            val bolt = d.putCardInHand(d.player1, "Lightning Bolt")
            d.giveMana(d.player1, Color.RED, 1)
            d.castSpell(d.player1, bolt, listOf(rebel)).error shouldBe null
        }
    }
})
