package com.wingedsheep.ai.engine

import com.wingedsheep.engine.core.DeclareAttackers
import com.wingedsheep.engine.legalactions.LegalActionEnumerator
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.ProtectionScope
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe

/**
 * The attack misplays from the 2026-10-09 AI game-log review, each rebuilt on the smallest board
 * that still shows it. Every test plays the same position twice: once as the live profile
 * ([AiProfile.PRODUCTION_CANDIDATE_EXPIRING]) and once as [AiProfile.PRODUCTION_CANDIDATE_ATTACKS],
 * so the test pins both that the position reproduces the misplay and that the flags fix it.
 */
class AttackDecisionReviewTest : FunSpec({

    val bears = card("Review Bears") {
        manaCost = "{1}{G}"
        typeLine = "Creature — Bear"
        power = 2
        toughness = 2
    }
    val horror = card("Review Horror") {
        manaCost = "{B}{B}"
        typeLine = "Creature — Horror"
        power = 7
        toughness = 7
        keywords(Keyword.TRAMPLE)
    }
    val witch = card("Review Witch") {
        manaCost = "{B}"
        typeLine = "Creature — Human Wizard"
        power = 1
        toughness = 1
    }
    val centaur = card("Review Centaur") {
        manaCost = "{2}{G}"
        typeLine = "Creature — Centaur"
        power = 3
        toughness = 3
        keywordAbility(KeywordAbility.Protection(ProtectionScope.Color(Color.BLACK)))
    }
    val zubera = card("Review Zubera") {
        manaCost = "{1}{G}"
        typeLine = "Creature — Zubera Spirit"
        power = 1
        toughness = 2
    }
    val pummeler = card("Review Pummeler") {
        manaCost = "{4}{G}"
        typeLine = "Creature — Giant"
        power = 6
        toughness = 4
    }
    val battler = card("Review Battler") {
        manaCost = "{3}{G}"
        typeLine = "Creature — Elf Warrior"
        power = 4
        toughness = 4
        keywords(Keyword.TRAMPLE)
    }
    val informant = card("Review Informant") {
        manaCost = "{2}{G}"
        typeLine = "Creature — Elf"
        power = 3
        toughness = 1
    }
    val drake = card("Review Drake") {
        manaCost = "{2}{U}"
        typeLine = "Creature — Drake"
        power = 3
        toughness = 2
        keywords(Keyword.FLYING)
    }
    val pegasus = card("Review Pegasus") {
        manaCost = "{W}"
        typeLine = "Creature — Pegasus"
        power = 1
        toughness = 2
        keywords(Keyword.FLYING)
    }
    val owl = card("Review Owl") {
        manaCost = "{1}{U}"
        typeLine = "Creature — Bird"
        power = 1
        toughness = 1
        keywords(Keyword.FLYING)
    }
    val unicorn = card("Review Unicorn") {
        manaCost = "{2}{W}"
        typeLine = "Creature — Unicorn"
        power = 2
        toughness = 3
    }
    val reviewCards = listOf(
        bears, horror, witch, centaur, zubera, pummeler, battler, informant, drake, pegasus, owl, unicorn,
    )

    fun driver(): GameTestDriver = GameTestDriver().apply {
        registerCards(TestCards.all + reviewCards)
        initMirrorMatch(Deck.of("Swamp" to 20, "Forest" to 20))
    }

    fun GameTestDriver.creature(owner: EntityId, name: String, tapped: Boolean = false): EntityId =
        putCreatureOnBattlefield(owner, name).also {
            removeSummoningSickness(it)
            if (tapped) tapPermanent(it)
        }

    fun GameTestDriver.attackersChosenBy(profile: AiProfile, playerId: EntityId): Set<EntityId> {
        val action = LegalActionEnumerator.create(cardRegistry).enumerate(state, playerId)
            .single { it.actionType == "DeclareAttackers" }
        val ai = AIPlayer.create(cardRegistry, playerId, profile)
        return (ai.chooseFrom(state, listOf(action)).action as DeclareAttackers).attackers.keys
    }

    test("a pro-black creature cannot be blocked by a black one") {
        val driver = driver()
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val c = driver.creature(driver.player2, "Review Centaur")
        val blackBlocker = driver.creature(driver.player1, "Review Witch")
        val greenBlocker = driver.creature(driver.player1, "Review Bears")
        val projected = driver.state.projectedState

        CombatMath.canBeBlockedBy(driver.state, projected, c, blackBlocker) shouldBe false
        CombatMath.canBeBlockedBy(driver.state, projected, c, greenBlocker) shouldBe true
        CombatMath.isEvasive(driver.state, projected, c, listOf(blackBlocker)) shouldBe true
    }

    // Game 24 turn 11: at 6 life, the AI sent its only non-black creature into an empty board for
    // two. The two pro-black Centaurs and a 1/2 untapped and dealt exactly seven.
    test("keeps home the only creature that can block a pro-black crack-back") {
        val driver = driver()
        val p1 = driver.player1
        val p2 = driver.player2
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val green = driver.creature(p1, "Review Bears")
        driver.creature(p1, "Review Horror")
        driver.creature(p1, "Review Witch")
        driver.creature(p2, "Review Centaur", tapped = true)
        driver.creature(p2, "Review Centaur", tapped = true)
        driver.creature(p2, "Review Zubera", tapped = true)
        driver.setLifeTotal(p1, 6)
        driver.setLifeTotal(p2, 11)
        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)

        driver.attackersChosenBy(AiProfile.PRODUCTION_CANDIDATE_ATTACKS, p1) shouldNotContain green
    }

    // Game 17 turn 21: at 8 life against 15 power, the AI swung both of its creatures at an
    // opponent on 7 and died to the swing back. Kept home, the two of them hold it to 7.
    test("does not trade its blockers for damage when the crack-back becomes lethal") {
        val driver = driver()
        val p1 = driver.player1
        val p2 = driver.player2
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.creature(p1, "Review Bears")
        driver.creature(p1, "Review Bears")
        driver.creature(p2, "Review Pummeler", tapped = true)
        driver.creature(p2, "Review Battler", tapped = true)
        driver.creature(p2, "Review Bears", tapped = true)
        driver.creature(p2, "Review Informant", tapped = true)
        driver.setLifeTotal(p1, 8)
        driver.setLifeTotal(p2, 7)
        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)

        driver.attackersChosenBy(AiProfile.PRODUCTION_CANDIDATE_EXPIRING, p1).size shouldBe 2
        driver.attackersChosenBy(AiProfile.PRODUCTION_CANDIDATE_ATTACKS, p1).shouldBeEmpty()
    }

    // Game 21 turn 14: three fliers against a board with no flier or reach, and the AI never
    // attacked (nor on turns 12 and 16).
    test("attacks with fliers the opponent cannot block") {
        val driver = driver()
        val p1 = driver.player1
        val p2 = driver.player2
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.creature(p1, "Review Pegasus")
        val drake = driver.creature(p1, "Review Drake")
        driver.creature(p1, "Review Owl")
        driver.creature(p1, "Review Unicorn")
        driver.creature(p2, "Review Witch")
        driver.creature(p2, "Review Bears")
        driver.creature(p2, "Review Witch")
        driver.setLifeTotal(p1, 14)
        driver.setLifeTotal(p2, 23)
        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)

        driver.attackersChosenBy(AiProfile.PRODUCTION_CANDIDATE_EXPIRING, p1) shouldBe emptySet()
        // The 3-power Drake is the attack that matters. The 1-power fliers may stay home: each one
        // kept back is a block that kills a 1/1 and survives, which is worth more than one damage
        // into 23 life.
        driver.attackersChosenBy(AiProfile.PRODUCTION_CANDIDATE_ATTACKS, p1) shouldContain drake
    }

    // Game 22 turn 13: Aesthir Glider (flying, can't block) stayed home while the opponent's only
    // flier was tapped. Attacking costs it nothing: it was never going to block.
    test("attacks with a flier that cannot block") {
        val driver = driver()
        val p1 = driver.player1
        val p2 = driver.player2
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val glider = driver.creature(p1, "Aesthir Glider")
        driver.creature(p1, "Review Witch")
        driver.creature(p2, "Review Witch")
        driver.creature(p2, "Review Witch")
        driver.creature(p2, "Review Bears")
        driver.creature(p2, "Review Pegasus", tapped = true)
        driver.setLifeTotal(p1, 18)
        driver.setLifeTotal(p2, 20)
        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)

        driver.attackersChosenBy(AiProfile.PRODUCTION_CANDIDATE_EXPIRING, p1) shouldNotContain glider
        driver.attackersChosenBy(AiProfile.PRODUCTION_CANDIDATE_ATTACKS, p1) shouldContain glider
    }
})
