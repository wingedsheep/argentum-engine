package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.handlers.effects.ReplacementEffectUtils
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.DoubleCounterPlacement
import com.wingedsheep.sdk.scripting.EntersWithDevour
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.ModifyCounterPlacement
import com.wingedsheep.sdk.scripting.events.Recipient
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Counter-placement replacements ([ModifyCounterPlacement], [DoubleCounterPlacement]) apply on every
 * path that *puts* counters (CR 122.6), in the order the affected player would choose (CR 616.1).
 *
 * | Rule / path | Covered by |
 * |---|---|
 * | CR 616.1 — +1/+1 counters: add, then double (the larger result) | "a wanted kind adds before doubling" |
 * | CR 616.1 — -1/-1 counters: double, then add (the smaller result) | "a harmful kind doubles before adding" |
 * | the order never depends on battlefield order | "battlefield order doesn't change the result" |
 * | CR 122.6 — a token created with counters | "a token entering with counters" |
 * | CR 702.80a — wither's -1/-1 counters | "wither damage is a placement" |
 * | costs are placements, but not effects (Doubling Season's "if an effect would put") | the two cost tests |
 * | CR 606.4 — a [+1] loyalty cost is a placement | "a +1 loyalty cost is a placement too" |
 * | CR 118.12 — a pay-or-suffer "put a counter" is a cost | "a pay-or-suffer counter cost…" |
 * | CR 714.3c — the Saga turn-based lore counter isn't an effect | "the Saga turn-based lore counter…" |
 * | CR 702.82a — devour's counters are entry counters | "devour's entry counters…" |
 */
class CounterPlacementReplacementPathsTest : ScenarioTestBase() {

    /** Winding Constrictor's permanent half: +1 of any kind on a creature you control. */
    private val adder = card("Test Counter Adder") {
        manaCost = "{G}"
        typeLine = "Enchantment"
        replacementEffect(
            ModifyCounterPlacement(
                modifier = 1,
                appliesTo = EventPattern.CounterPlacementEvent(null, Recipient.CreatureYouControl)
            )
        )
    }

    /** "If one or more counters would be put on a permanent you control, twice that many" — costs too. */
    private val doubler = card("Test Counter Doubler") {
        manaCost = "{G}"
        typeLine = "Enchantment"
        replacementEffect(
            DoubleCounterPlacement(
                appliesTo = EventPattern.CounterPlacementEvent(null, Recipient.PermanentYouControl)
            )
        )
    }

    /** Doubling Season's counter half: "If an *effect* would put…". */
    private val season = card("Test Counter Season") {
        manaCost = "{G}"
        typeLine = "Enchantment"
        replacementEffect(
            DoubleCounterPlacement(
                appliesTo = EventPattern.CounterPlacementEvent(null, Recipient.PermanentYouControl),
                effectsOnly = true
            )
        )
    }

    private val grow = card("Test Grow") {
        manaCost = "{G}"
        typeLine = "Instant"
        spell {
            val t = target(TargetFilter.Creature)
            effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, t)
        }
    }

    private val wilt = card("Test Wilt") {
        manaCost = "{B}"
        typeLine = "Instant"
        spell {
            val t = target(TargetFilter.Creature)
            effect = Effects.AddCounters(CounterType.MINUS_ONE_MINUS_ONE, 1, t)
        }
    }

    private val tokenMaker = card("Test Counter Token Maker") {
        manaCost = "{G}"
        typeLine = "Sorcery"
        spell {
            effect = Effects.CreateToken(
                power = 1, toughness = 1, creatureTypes = setOf("Soldier"), name = "Test Soldier",
                initialCounters = mapOf(CounterType.PLUS_ONE_PLUS_ONE to 1)
            )
        }
    }

    private val wall = card("Test Thick Wall") {
        manaCost = "{2}"
        typeLine = "Creature — Wall"
        power = 0
        toughness = 6
    }

    private val witherer = card("Test Witherer") {
        manaCost = "{1}{B}"
        typeLine = "Creature — Elemental"
        power = 2
        toughness = 2
        keywords(Keyword.WITHER)
    }

    private val coster = card("Test Counter Coster") {
        manaCost = "{G}"
        typeLine = "Creature — Elf"
        power = 1
        toughness = 1
        activatedAbility {
            cost = Costs.PutCounterOnSelf(CounterType.PLUS_ONE_PLUS_ONE)
            effect = Effects.GainLife(1)
        }
    }

    private val walker = card("Test Counter Walker") {
        manaCost = "{2}"
        typeLine = "Planeswalker — Test"
        startingLoyalty = 3
        loyaltyAbility(+1) { effect = Effects.GainLife(1) }
    }

    private val toll = card("Test Counter Toll") {
        manaCost = "{G}"
        typeLine = "Sorcery"
        spell {
            effect = Effects.PayOrSuffer(
                cost = Costs.pay.PutCountersOnPermanent(CounterType.PLUS_ONE_PLUS_ONE, filter = GameObjectFilter.Creature),
                suffer = Effects.LoseLife(3, EffectTarget.Controller)
            )
        }
    }

    private val longSaga = card("Test Long Saga") {
        manaCost = "{G}"
        typeLine = "Enchantment — Saga"
        for (chapter in 1..6) sagaChapter(chapter) { effect = Effects.GainLife(1) }
    }

    private val devourer = card("Test Counter Devourer") {
        manaCost = "{G}"
        typeLine = "Creature — Ooze"
        power = 1
        toughness = 1
        replacementEffect(EntersWithDevour(multiplier = 1))
    }

    private fun TestGame.countersOn(entityId: EntityId, type: CounterType): Int =
        state.getEntity(entityId)?.get<CountersComponent>()?.getCount(type) ?: 0

    private fun board(vararg permanents: String) = scenario()
        .withPlayers("Player", "Opponent")
        .withLandsOnBattlefield(1, "Forest", 2)
        .withLandsOnBattlefield(1, "Swamp", 2)
        .withCardInLibrary(1, "Forest").withCardInLibrary(1, "Forest").withCardInLibrary(1, "Forest")
        .withCardInLibrary(2, "Forest").withCardInLibrary(2, "Forest").withCardInLibrary(2, "Forest")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .let { b -> permanents.fold(b) { acc, name -> acc.withCardOnBattlefield(1, name) } }

    init {
        listOf(adder, doubler, season, grow, wilt, tokenMaker, wall, witherer, coster, walker, toll, longSaga, devourer)
            .forEach { cardRegistry.register(it) }

        test("a wanted kind adds before doubling") {
            val game = board("Test Counter Adder", "Test Counter Doubler", "Test Thick Wall")
                .withCardInHand(1, "Test Grow").build()
            val wallId = game.findPermanent("Test Thick Wall")!!
            game.castSpell(1, "Test Grow", targetId = wallId).error shouldBe null
            game.resolveStack()
            withClue("(1 + 1) × 2, not 1 × 2 + 1") {
                game.countersOn(wallId, CounterType.PLUS_ONE_PLUS_ONE) shouldBe 4
            }
        }

        test("a harmful kind doubles before adding") {
            val game = board("Test Counter Adder", "Test Counter Doubler", "Test Thick Wall")
                .withCardInHand(1, "Test Wilt").build()
            val wallId = game.findPermanent("Test Thick Wall")!!
            game.castSpell(1, "Test Wilt", targetId = wallId).error shouldBe null
            game.resolveStack()
            withClue("the wall's controller picks 1 × 2 + 1 over (1 + 1) × 2") {
                game.countersOn(wallId, CounterType.MINUS_ONE_MINUS_ONE) shouldBe 3
            }
        }

        test("battlefield order doesn't change the result") {
            for (order in listOf(
                listOf("Test Counter Adder", "Test Counter Doubler"),
                listOf("Test Counter Doubler", "Test Counter Adder"),
            )) {
                val game = board(*(order + "Test Thick Wall").toTypedArray())
                    .withCardInHand(1, "Test Grow").build()
                val wallId = game.findPermanent("Test Thick Wall")!!
                game.castSpell(1, "Test Grow", targetId = wallId).error shouldBe null
                game.resolveStack()
                withClue("order $order") { game.countersOn(wallId, CounterType.PLUS_ONE_PLUS_ONE) shouldBe 4 }
            }
        }

        test("the ordering helper folds additions and doublings per kind") {
            ReplacementEffectUtils.orderedForAffectedPlayer(1, listOf(1), 1, CounterType.PLUS_ONE_PLUS_ONE) shouldBe 4
            ReplacementEffectUtils.orderedForAffectedPlayer(1, listOf(1), 1, CounterType.MINUS_ONE_MINUS_ONE) shouldBe 3
            ReplacementEffectUtils.orderedForAffectedPlayer(1, listOf(1), 1, CounterType.POISON) shouldBe 3
            // A reduction that empties the placement ends it — nothing later brings it back.
            ReplacementEffectUtils.orderedForAffectedPlayer(1, listOf(-1, 1), 1, CounterType.MINUS_ONE_MINUS_ONE) shouldBe 0
            // A wanted kind takes its reduction last, after the doubling.
            ReplacementEffectUtils.orderedForAffectedPlayer(1, listOf(-1), 1, CounterType.PLUS_ONE_PLUS_ONE) shouldBe 1
        }

        test("a token entering with counters") {
            val game = board("Test Counter Adder", "Test Counter Doubler")
                .withCardInHand(1, "Test Counter Token Maker").build()
            game.castSpell(1, "Test Counter Token Maker").error shouldBe null
            game.resolveStack()
            val tokenId = game.findPermanent("Test Soldier")!!
            game.countersOn(tokenId, CounterType.PLUS_ONE_PLUS_ONE) shouldBe 4
        }

        test("wither damage is a placement") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Test Witherer", summoningSickness = false)
                .withCardOnBattlefield(2, "Test Thick Wall")
                .withCardOnBattlefield(2, "Test Counter Adder")
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(2, "Forest")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Test Witherer" to 2)).error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
            game.declareBlockers(mapOf("Test Thick Wall" to listOf("Test Witherer"))).error shouldBe null
            game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)
            val wallId = game.findPermanent("Test Thick Wall")!!
            game.countersOn(wallId, CounterType.MINUS_ONE_MINUS_ONE) shouldBe 3
        }

        test("a cost's counters are doubled by a would-be-put doubler") {
            val game = board("Test Counter Doubler", "Test Counter Coster").build()
            val costerId = game.findPermanent("Test Counter Coster")!!
            val abilityId = cardRegistry.requireCard("Test Counter Coster").activatedAbilities[0].id
            game.execute(ActivateAbility(game.player1Id, costerId, abilityId)).error shouldBe null
            game.countersOn(costerId, CounterType.PLUS_ONE_PLUS_ONE) shouldBe 2
        }

        test("a cost's counters pass an if-an-effect-would-put doubler by") {
            val game = board("Test Counter Season", "Test Counter Coster").build()
            val costerId = game.findPermanent("Test Counter Coster")!!
            val abilityId = cardRegistry.requireCard("Test Counter Coster").activatedAbilities[0].id
            game.execute(ActivateAbility(game.player1Id, costerId, abilityId)).error shouldBe null
            game.countersOn(costerId, CounterType.PLUS_ONE_PLUS_ONE) shouldBe 1
        }

        test("a +1 loyalty cost is a placement too") {
            val doubled = board("Test Counter Doubler", "Test Counter Walker").build()
            val walkerId = doubled.findPermanent("Test Counter Walker")!!
            val abilityId = cardRegistry.requireCard("Test Counter Walker").activatedAbilities[0].id
            doubled.execute(ActivateAbility(doubled.player1Id, walkerId, abilityId)).error shouldBe null
            doubled.countersOn(walkerId, CounterType.LOYALTY) shouldBe 5

            val season = board("Test Counter Season", "Test Counter Walker").build()
            val seasonWalker = season.findPermanent("Test Counter Walker")!!
            season.execute(ActivateAbility(season.player1Id, seasonWalker, abilityId)).error shouldBe null
            withClue("a cost isn't an effect") { season.countersOn(seasonWalker, CounterType.LOYALTY) shouldBe 4 }
        }
        test("a pay-or-suffer counter cost passes an if-an-effect-would-put doubler by") {
            for ((doublerName, expected) in listOf("Test Counter Season" to 1, "Test Counter Doubler" to 2)) {
                val game = board(doublerName, "Test Thick Wall").withCardInHand(1, "Test Counter Toll").build()
                val wallId = game.findPermanent("Test Thick Wall")!!
                game.castSpell(1, "Test Counter Toll").error shouldBe null
                game.resolveStack()
                game.selectCards(listOf(wallId)).error shouldBe null
                game.resolveStack()
                withClue(doublerName) { game.countersOn(wallId, CounterType.PLUS_ONE_PLUS_ONE) shouldBe expected }
            }
        }

        test("the Saga turn-based lore counter isn't an effect's placement") {
            for ((doublerName, entry, afterTurn) in listOf(
                Triple("Test Counter Season", 2, 3),
                Triple("Test Counter Doubler", 2, 4),
            )) {
                val game = board(doublerName).withCardInHand(1, "Test Long Saga").build()
                game.castSpell(1, "Test Long Saga").error shouldBe null
                game.resolveStack()
                val sagaId = game.findPermanent("Test Long Saga")!!
                withClue("$doublerName: entering with a lore counter (CR 122.6)") {
                    game.countersOn(sagaId, CounterType.LORE) shouldBe entry
                }
                // Around to the Saga controller's next precombat main phase (CR 714.3c).
                game.passUntilPhase(Phase.ENDING, Step.END)
                game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                game.passUntilPhase(Phase.ENDING, Step.END)
                game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                game.state.activePlayerId shouldBe game.player1Id
                withClue("$doublerName: the turn-based lore counter") {
                    game.countersOn(sagaId, CounterType.LORE) shouldBe afterTurn
                }
            }
        }

        test("devour's entry counters run through the placement replacements") {
            val game = board("Test Counter Adder", "Test Counter Doubler", "Test Thick Wall")
                .withCardInHand(1, "Test Counter Devourer").build()
            val wallId = game.findPermanent("Test Thick Wall")!!
            game.castSpell(1, "Test Counter Devourer").error shouldBe null
            game.resolveStack()
            game.selectCards(listOf(wallId)).error shouldBe null
            game.resolveStack()
            val devourerId = game.findPermanent("Test Counter Devourer")!!
            withClue("one devoured → 1 counter → (1 + 1) × 2") {
                game.countersOn(devourerId, CounterType.PLUS_ONE_PLUS_ONE) shouldBe 4
            }
        }
    }
}
