package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantWard
import com.wingedsheep.sdk.scripting.effects.WardCost
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Ward granted by an emblem's own static ability ("Knights you control … have ward {1}" —
 * Teferi Akosa of Zhalfir's −2). The emblem lives in no zone, so the battlefield walk that
 * collects printed `GrantWard` statics can't see it; these pin that its grant triggers like one
 * printed on a permanent its controller controls, and only for the group it names.
 */
class EmblemGrantedWardTest : FunSpec({

    val knightsYouControl = GroupFilter(GameObjectFilter.Creature.withSubtype("Knight").youControl())

    val knightEmblemSorcery = card("Knight Ward Emblem") {
        manaCost = "{W}"
        typeLine = "Sorcery"
        spell {
            effect = Effects.CreatePermanentEmblem(
                ownedStaticAbilities = listOf(GrantWard(WardCost.Life(2), knightsYouControl)),
                emblemDescription = "Knights you control have \"Ward—Pay 2 life.\""
            )
        }
    }

    val plainKnight = card("Plain Knight") {
        manaCost = "{1}{W}"
        typeLine = "Creature — Human Knight"
        power = 2
        toughness = 2
    }

    val plainBear = card("Plain Bear") {
        manaCost = "{1}{G}"
        typeLine = "Creature — Bear"
        power = 2
        toughness = 2
    }

    val plainChangeling = card("Plain Changeling") {
        manaCost = "{1}{U}"
        typeLine = "Creature — Shapeshifter"
        power = 2
        toughness = 2
        keywords(Keyword.CHANGELING)
    }

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(knightEmblemSorcery, plainKnight, plainBear, plainChangeling))
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    /** [playerId] casts the emblem sorcery and it resolves. */
    fun GameTestDriver.giveEmblem(playerId: com.wingedsheep.sdk.model.EntityId) {
        giveMana(playerId, Color.WHITE, 1)
        castSpell(playerId, putCardInHand(playerId, "Knight Ward Emblem"))
        bothPass()
    }

    fun GameTestDriver.boltFrom(caster: com.wingedsheep.sdk.model.EntityId, target: com.wingedsheep.sdk.model.EntityId) {
        giveMana(caster, Color.RED, 1)
        castSpellWithTargets(caster, putCardInHand(caster, "Lightning Bolt"), listOf(ChosenTarget.Permanent(target)))
        bothPass()
    }

    test("a Knight its controller's emblem covers shows ward and ward triggers against an opponent's spell") {
        val driver = createDriver()
        val active = driver.activePlayer!!
        val opponent = driver.getOpponent(active)

        driver.giveEmblem(active)
        // Enters after the emblem — the grant re-evaluates its group, it isn't locked in.
        val knight = driver.putCreatureOnBattlefield(active, "Plain Knight")
        driver.state.projectedState.getKeywords(knight) shouldContain Keyword.WARD.name

        driver.passPriority(active)
        driver.boltFrom(opponent, knight)

        val decision = driver.pendingDecision
        decision.shouldBeInstanceOf<YesNoDecision>()
        decision.playerId shouldBe opponent

        driver.submitYesNo(opponent, false)
        repeat(2) { if (driver.state.priorityPlayerId != null) driver.bothPass() }
        driver.findPermanent(active, "Plain Knight") shouldNotBe null
    }

    test("a creature outside the emblem's group gets no ward") {
        val driver = createDriver()
        val active = driver.activePlayer!!
        val opponent = driver.getOpponent(active)

        driver.giveEmblem(active)
        val bear = driver.putCreatureOnBattlefield(active, "Plain Bear")
        driver.state.projectedState.getKeywords(bear) shouldNotContain Keyword.WARD.name

        driver.passPriority(active)
        driver.boltFrom(opponent, bear)

        driver.pendingDecision shouldBe null
        driver.findPermanent(active, "Plain Bear") shouldBe null
    }

    test("an opponent's Knight is not covered by your emblem") {
        val driver = createDriver()
        val active = driver.activePlayer!!
        val opponent = driver.getOpponent(active)

        driver.giveEmblem(active)
        val theirKnight = driver.putCreatureOnBattlefield(opponent, "Plain Knight")
        driver.state.projectedState.getKeywords(theirKnight) shouldNotContain Keyword.WARD.name

        driver.boltFrom(active, theirKnight)

        driver.pendingDecision shouldBe null
        driver.findPermanent(opponent, "Plain Knight") shouldBe null
    }

    test("the emblem's controller targeting their own Knight doesn't trigger ward") {
        val driver = createDriver()
        val active = driver.activePlayer!!

        driver.giveEmblem(active)
        val knight = driver.putCreatureOnBattlefield(active, "Plain Knight")

        driver.boltFrom(active, knight)

        driver.pendingDecision shouldBe null
        driver.findPermanent(active, "Plain Knight") shouldBe null
    }

    test("a changeling is a Knight for the emblem's group — the ward trigger reads projected subtypes") {
        val driver = createDriver()
        val active = driver.activePlayer!!
        val opponent = driver.getOpponent(active)

        driver.giveEmblem(active)
        val changeling = driver.putCreatureOnBattlefield(active, "Plain Changeling")
        driver.state.projectedState.getKeywords(changeling) shouldContain Keyword.WARD.name

        driver.passPriority(active)
        driver.boltFrom(opponent, changeling)

        val decision = driver.pendingDecision
        decision.shouldBeInstanceOf<YesNoDecision>()
        decision.playerId shouldBe opponent
    }
})
