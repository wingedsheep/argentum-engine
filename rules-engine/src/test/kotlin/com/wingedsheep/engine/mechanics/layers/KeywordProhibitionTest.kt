package com.wingedsheep.engine.mechanics.layers

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.RemoveKeywordStatic
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * "Creatures your opponents control lose flying and can't have or gain flying" —
 * [RemoveKeywordStatic] with `cantHaveOrGain` (the Born of the Gods Archetypes). Pinned against the
 * Archetype rulings (2014-02-01):
 * - it applies to each opposing creature no matter when it entered;
 * - a resolving spell or ability that would give the keyword to such a creature doesn't create
 *   that effect for it — the creature stays without the keyword even after the Archetype leaves;
 * - a static grant (an Aura, an anthem) resumes once the Archetype leaves;
 * - when each player controls the Archetype, no creature has the keyword.
 * Unlike a plain "loses flying", a grant with a later timestamp never wins.
 */
class KeywordProhibitionTest : FunSpec({
    val archetype = card("Prohibition Test Archetype") {
        manaCost = "{0}"; typeLine = "Enchantment Creature — Wizard"; power = 1; toughness = 1
        staticAbility { ability = GrantKeyword(Keyword.FLYING, GroupFilter.AllCreaturesYouControl) }
        staticAbility {
            ability = RemoveKeywordStatic(Keyword.FLYING, GroupFilter.AllCreaturesOpponentsControl, cantHaveOrGain = true)
        }
    }
    val plainLoss = card("Prohibition Test Gravity") {
        manaCost = "{0}"; typeLine = "Enchantment"
        staticAbility { ability = RemoveKeywordStatic(Keyword.FLYING, GroupFilter(GameObjectFilter.Creature)) }
    }
    val wings = card("Prohibition Test Wings") {
        manaCost = "{0}"; typeLine = "Enchantment"
        staticAbility { ability = GrantKeyword(Keyword.FLYING, GroupFilter.AllCreaturesYouControl) }
    }
    val skyWatch = card("Prohibition Test Sky Watch") {
        manaCost = "{0}"; typeLine = "Enchantment"
        staticAbility { ability = GrantKeyword(Keyword.VIGILANCE, GroupFilter(GameObjectFilter.Creature.withKeyword(Keyword.FLYING))) }
    }
    val flyer = card("Prohibition Test Flyer") {
        manaCost = "{0}"; typeLine = "Creature — Bird"; power = 1; toughness = 1
        keywords(Keyword.FLYING)
    }
    val jump = card("Prohibition Test Jump") {
        manaCost = "{0}"; typeLine = "Instant"
        spell { val t = target(TargetFilter.Creature); effect = Effects.GrantKeyword(Keyword.FLYING, t) }
    }
    val mute = card("Prohibition Test Mute") {
        manaCost = "{0}"; typeLine = "Instant"
        spell { val t = target(TargetFilter.Creature); effect = Effects.RemoveAllAbilities(t) }
    }

    fun driver() = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(archetype, plainLoss, wings, skyWatch, flyer, jump, mute))
        initMirrorMatch(Deck.of("Forest" to 40))
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun GameTestDriver.cast(name: String, target: EntityId) {
        val me = activePlayer!!
        castSpell(me, putCardInHand(me, name), listOf(target)).error shouldBe null
        bothPass()
    }

    fun GameTestDriver.flies(id: EntityId) = state.projectedState.hasKeyword(id, Keyword.FLYING)

    test("opposing creatures lose the keyword, whenever they entered; your creatures have it") {
        val d = driver()
        val me = d.activePlayer!!
        val them = d.getOpponent(me)
        val early = d.putCreatureOnBattlefield(me, flyer.name)
        val mine = d.putCreatureOnBattlefield(them, "Grizzly Bears")
        d.putPermanentOnBattlefield(them, archetype.name)
        val late = d.putCreatureOnBattlefield(me, flyer.name)

        d.flies(early) shouldBe false
        d.flies(late) shouldBe false
        d.flies(mine) shouldBe true
        d.state.projectedState.cantGainKeyword(late, Keyword.FLYING.name) shouldBe true
    }

    test("a resolving grant made while the prohibition applies is never created, even after it ends") {
        val d = driver()
        val me = d.activePlayer!!
        val them = d.getOpponent(me)
        val bears = d.putCreatureOnBattlefield(me, "Grizzly Bears")
        val source = d.putPermanentOnBattlefield(them, archetype.name)

        d.cast(jump.name, bears)
        d.flies(bears) shouldBe false

        d.moveToGraveyard(source)
        d.flies(bears) shouldBe false
    }

    test("a grant that resolved before the prohibition is suppressed, then applies again once it ends") {
        val d = driver()
        val me = d.activePlayer!!
        val them = d.getOpponent(me)
        val bears = d.putCreatureOnBattlefield(me, "Grizzly Bears")
        d.cast(jump.name, bears)
        val source = d.putPermanentOnBattlefield(them, archetype.name)
        d.flies(bears) shouldBe false

        d.moveToGraveyard(source)
        d.flies(bears) shouldBe true
    }

    test("a newer static grant can't give the keyword back, and resumes once the prohibition ends") {
        val d = driver()
        val me = d.activePlayer!!
        val them = d.getOpponent(me)
        val bears = d.putCreatureOnBattlefield(me, "Grizzly Bears")
        val source = d.putPermanentOnBattlefield(them, archetype.name)
        d.putPermanentOnBattlefield(me, wings.name)
        d.flies(bears) shouldBe false

        d.moveToGraveyard(source)
        d.flies(bears) shouldBe true
    }

    test("a plain loss is timestamp-ordered: a newer grant wins") {
        val d = driver()
        val me = d.activePlayer!!
        val bears = d.putCreatureOnBattlefield(me, "Grizzly Bears")
        d.putPermanentOnBattlefield(me, plainLoss.name)
        d.cast(jump.name, bears)
        d.flies(bears) shouldBe true
    }

    test("a keyword counter doesn't give the keyword back") {
        val d = driver()
        val me = d.activePlayer!!
        val them = d.getOpponent(me)
        val bears = d.putCreatureOnBattlefield(me, "Grizzly Bears")
        d.replaceState(d.state.updateEntity(bears) { it.with(CountersComponent(mapOf(CounterType.FLYING to 1))) })
        d.flies(bears) shouldBe true
        val source = d.putPermanentOnBattlefield(them, archetype.name)
        d.flies(bears) shouldBe false

        d.moveToGraveyard(source)
        d.flies(bears) shouldBe true
    }

    test("when each player controls the Archetype, no creature has the keyword") {
        val d = driver()
        val me = d.activePlayer!!
        val them = d.getOpponent(me)
        val mine = d.putPermanentOnBattlefield(me, archetype.name)
        val theirs = d.putPermanentOnBattlefield(them, archetype.name)
        d.flies(mine) shouldBe false
        d.flies(theirs) shouldBe false
    }

    test("a keyword-reading grant doesn't see a prohibited keyword") {
        val d = driver()
        val me = d.activePlayer!!
        val them = d.getOpponent(me)
        d.putPermanentOnBattlefield(them, archetype.name)
        d.putPermanentOnBattlefield(me, skyWatch.name)
        val bird = d.putCreatureOnBattlefield(me, flyer.name)
        d.state.projectedState.hasKeyword(bird, Keyword.VIGILANCE) shouldBe false
    }

    test("an Archetype that lost all abilities no longer prohibits") {
        val d = driver()
        val me = d.activePlayer!!
        val them = d.getOpponent(me)
        val bird = d.putCreatureOnBattlefield(me, flyer.name)
        val source = d.putPermanentOnBattlefield(them, archetype.name)
        d.flies(bird) shouldBe false

        d.cast(mute.name, source)
        d.flies(bird) shouldBe true
        d.state.projectedState.cantGainKeyword(bird, Keyword.FLYING.name) shouldBe false
    }
})
