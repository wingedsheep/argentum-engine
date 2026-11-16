package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.CantBeBlockedBy
import com.wingedsheep.sdk.scripting.CantBeBlockedByCreaturesWithLessPower
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.Duration
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * A creature's own printed blocking restrictions switch off while it is face down or has lost
 * all its abilities. `BlockCheckContext.printedStatics` is the one read every printed-restriction
 * rule in `BlockEvasionRules` goes through: the attacker's `CantBeBlockedBy` (Silumgar Assassin as
 * a face-down 2/2, or after Dance of the Skywise), a battlefield host's group-scoped
 * `CantBeBlockedBy`, and the power-comparison restrictions.
 */
class PrintedBlockEvasionAbilityLossTest : FunSpec({

    val shadowStalker = card("Evasion Test Stalker") {
        manaCost = "{1}{B}"
        typeLine = "Creature — Human"
        power = 2; toughness = 2
        morph = "{B}"
        staticAbility {
            ability = CantBeBlockedBy(GameObjectFilter.Creature.powerGreaterThanEntity(EffectTarget.Self))
        }
    }
    val skyDenier = card("Evasion Test Sky Denier") {
        manaCost = "{2}{G}"
        typeLine = "Creature — Elemental"
        power = 3; toughness = 3
        staticAbility {
            ability = CantBeBlockedBy(
                blockerFilter = GameObjectFilter.Creature.withKeyword(Keyword.FLYING),
                filter = GroupFilter(GameObjectFilter.Creature.youControl(), excludeSelf = true),
            )
        }
    }
    val breaker = card("Evasion Test Breaker") {
        manaCost = "{1}{W}"
        typeLine = "Creature — Soldier"
        power = 3; toughness = 3
        staticAbility { ability = CantBeBlockedByCreaturesWithLessPower() }
    }
    val grunt = card("Evasion Test Grunt") {
        manaCost = "{1}{G}"
        typeLine = "Creature — Beast"
        power = 2; toughness = 2
    }
    val flier = card("Evasion Test Flier") {
        manaCost = "{1}{U}"
        typeLine = "Creature — Bird"
        power = 1; toughness = 1
        keywords(Keyword.FLYING)
    }
    val ogre = card("Evasion Test Ogre") {
        manaCost = "{2}{R}"
        typeLine = "Creature — Ogre"
        power = 4; toughness = 4
    }
    val strip = card("Evasion Test Strip") {
        manaCost = "{U}"
        typeLine = "Instant"
        spell {
            val creature = target(TargetFilter.Creature)
            effect = Effects.RemoveAllAbilities(creature, Duration.EndOfTurn)
        }
    }

    fun driver() = GameTestDriver().also {
        it.registerCards(TestCards.all + listOf(shadowStalker, skyDenier, breaker, grunt, flier, ogre, strip))
        it.initMirrorMatch(Deck.of("Island" to 40), skipMulligans = true, startingPlayer = 0)
        it.passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun GameTestDriver.creature(owner: EntityId, name: String) =
        putCreatureOnBattlefield(owner, name).also { removeSummoningSickness(it) }

    fun GameTestDriver.stripAbilities(target: EntityId) {
        val spell = putCardInHand(player1, strip.name)
        giveMana(player1, Color.BLUE, 1)
        castSpell(player1, spell, listOf(target)).error shouldBe null
        bothPass().error shouldBe null
        state.projectedState.hasLostAllAbilities(target) shouldBe true
    }

    fun GameTestDriver.blockError(attacker: EntityId, blocker: EntityId): String? {
        passPriorityUntil(Step.DECLARE_ATTACKERS)
        declareAttackers(player1, listOf(attacker), player2).error shouldBe null
        passPriorityUntil(Step.DECLARE_BLOCKERS)
        return declareBlockers(player2, mapOf(blocker to listOf(attacker))).error
    }

    test("the printed restriction applies face up") {
        val d = driver()
        val stalker = d.creature(d.player1, shadowStalker.name)
        val big = d.creature(d.player2, ogre.name)
        d.blockError(stalker, big) shouldNotBe null
    }

    test("a face-down attacker has no printed CantBeBlockedBy") {
        val d = driver()
        val stalker = d.creature(d.player1, shadowStalker.name)
        d.replaceState(d.state.updateEntity(stalker) { it.with(FaceDownComponent) })
        val big = d.creature(d.player2, ogre.name)
        d.blockError(stalker, big) shouldBe null
    }

    test("an attacker that lost all abilities has no printed CantBeBlockedBy") {
        val d = driver()
        val stalker = d.creature(d.player1, shadowStalker.name)
        val big = d.creature(d.player2, ogre.name)
        d.stripAbilities(stalker)
        d.blockError(stalker, big) shouldBe null
    }

    test("a host that lost all abilities no longer projects its group restriction") {
        val d = driver()
        val denier = d.creature(d.player1, skyDenier.name)
        val attacker = d.creature(d.player1, grunt.name)
        val bird = d.creature(d.player2, flier.name)
        d.stripAbilities(denier)
        withClue("the Denier's 'other creatures you control' clause is gone") {
            d.blockError(attacker, bird) shouldBe null
        }
    }

    test("a power-comparison restriction is gone once the attacker loses all abilities") {
        val d = driver()
        val soldier = d.creature(d.player1, breaker.name)
        val small = d.creature(d.player2, grunt.name)
        d.stripAbilities(soldier)
        d.blockError(soldier, small) shouldBe null
    }
})
