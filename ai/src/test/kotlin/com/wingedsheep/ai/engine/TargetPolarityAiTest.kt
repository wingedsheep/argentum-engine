package com.wingedsheep.ai.engine

import com.wingedsheep.ai.engine.knowledge.IntentCatalog
import com.wingedsheep.ai.engine.knowledge.TargetPolarity
import com.wingedsheep.ai.engine.knowledge.TargetPolarityAnalyzer
import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PassPriority
import com.wingedsheep.engine.legalactions.LegalAction
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.ecl.cards.Blossombind
import com.wingedsheep.mtg.sets.definitions.ecl.cards.ExplosiveProdigy
import com.wingedsheep.mtg.sets.definitions.inv.cards.Prohibit
import com.wingedsheep.mtg.sets.definitions.inv.cards.TolarianEmissary
import com.wingedsheep.mtg.sets.definitions.inv.cards.TravelersCloak
import com.wingedsheep.mtg.sets.definitions.inv.cards.TreefolkHealer
import com.wingedsheep.mtg.sets.definitions.lea.cards.GiantGrowth
import com.wingedsheep.mtg.sets.definitions.lea.cards.Unsummon
import com.wingedsheep.mtg.sets.definitions.mir.cards.Pacifism
import com.wingedsheep.mtg.sets.definitions.ons.cards.BlatantThievery
import com.wingedsheep.mtg.sets.definitions.ons.cards.DaruHealer
import com.wingedsheep.mtg.sets.definitions.ons.cards.GustcloakHarrier
import com.wingedsheep.mtg.sets.definitions.ons.cards.Sandskin
import com.wingedsheep.mtg.sets.definitions.usg.cards.GloriousAnthem
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * [AiProfile.targetPolarityFromEffect]: aim at the side of the table the effect is *for*.
 *
 * Every board here is one cited in the 2026-10-09 AI-vs-AI log review, cut down to the cards that
 * decide it. Each case asserts the misplay on [AiProfile.PRODUCTION_CANDIDATE_EXPIRING] (the live
 * profile the logs were played on) as well as the fix on [AiProfile.PRODUCTION_CANDIDATE_POLARITY],
 * so a test that passes is evidence the flag is what moved it.
 */
class TargetPolarityAiTest : FunSpec({

    val cards = TestCards.all + listOf(
        Blossombind, ExplosiveProdigy, Prohibit, TolarianEmissary, TravelersCloak, TreefolkHealer,
        GiantGrowth, Unsummon, Pacifism, BlatantThievery, DaruHealer, GustcloakHarrier, Sandskin,
        GloriousAnthem,
    )
    val registry = CardRegistry().apply { register(cards) }

    fun driver(): GameTestDriver = GameTestDriver().apply {
        registerCards(cards)
        initMirrorMatch(deck = Deck.of("Island" to 40), skipMulligans = true, startingPlayer = 0)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun lands(d: GameTestDriver, player: EntityId, vararg names: Pair<String, Int>) {
        for ((name, count) in names) repeat(count) { d.putLandOnBattlefield(player, name) }
    }

    fun choose(profile: AiProfile, d: GameTestDriver, choices: List<LegalAction>): LegalAction =
        AIPlayer.create(registry, d.player1, profile).chooseFrom(d.state, choices)

    val live = AiProfile.PRODUCTION_CANDIDATE_EXPIRING
    val fixed = AiProfile.PRODUCTION_CANDIDATE_POLARITY

    context("polarity is read off the effect") {
        test("a damage shield on any target is for our side") {
            val ability = DaruHealer.script.activatedAbilities.single()
            TargetPolarityAnalyzer.forAbility(DaruHealer, ability) shouldBe listOf(TargetPolarity.BENEFICIAL)
        }
        test("a counterspell behind a mana-value gate is still aimed at their spell") {
            TargetPolarityAnalyzer.forSpell(Prohibit) shouldBe listOf(TargetPolarity.HARMFUL)
        }
        test("an Aura reads off what it does to the enchanted creature") {
            TargetPolarityAnalyzer.forSpell(Blossombind) shouldBe listOf(TargetPolarity.HARMFUL)
            TargetPolarityAnalyzer.forSpell(Pacifism) shouldBe listOf(TargetPolarity.HARMFUL)
            TargetPolarityAnalyzer.forSpell(TravelersCloak) shouldBe listOf(TargetPolarity.BENEFICIAL)
        }
        test("stealing and pumping are one-sided") {
            TargetPolarityAnalyzer.forSpell(BlatantThievery) shouldBe listOf(TargetPolarity.HARMFUL)
            TargetPolarityAnalyzer.forSpell(GiantGrowth) shouldBe listOf(TargetPolarity.BENEFICIAL)
        }
        test("either-way effects stay unknown") {
            // Bouncing our own creature in response to removal is a real play.
            TargetPolarityAnalyzer.forSpell(Unsummon) shouldBe listOf(TargetPolarity.UNKNOWN)
            // "Prevent combat damage dealt to and by": a Fog on their attacker or armour on ours.
            TargetPolarityAnalyzer.forSpell(Sandskin) shouldBe listOf(TargetPolarity.UNKNOWN)
        }
    }

    // Game 18 turn 26: Daru Healer's shield went on the opponent's Gustcloak Harrier.
    test("a damage shield is aimed at our own creature, not theirs") {
        val d = driver()
        val healer = d.putCreatureOnBattlefield(d.player1, "Daru Healer")
        d.removeSummoningSickness(healer)
        d.putCreatureOnBattlefield(d.player1, "Grizzly Bears")
        val harrier = d.putCreatureOnBattlefield(d.player2, "Gustcloak Harrier")
        val activation = d.legalActions(d.player1).single { (it.action as? ActivateAbility)?.sourceId == healer }
        val intents = IntentCatalog.of(registry)

        fun aimedAt(polarity: Boolean): EntityId {
            val filled = TargetSelection.fillHeuristically(
                d.state, activation, d.player1, fillPartialRequirements = true,
                intents = intents, polarityFromEffect = polarity,
            ) as ActivateAbility
            return when (val target = filled.targets.single()) {
                is ChosenTarget.Permanent -> target.entityId
                is ChosenTarget.Player -> target.playerId
                else -> error("unexpected target $target")
            }
        }

        aimedAt(polarity = false) shouldBe harrier
        // Ourselves or one of our creatures — anything on our side of the table.
        val shielded = aimedAt(polarity = true)
        (shielded == d.player1 || d.state.projectedState.getController(shielded) == d.player1) shouldBe true
    }

    // Game 15 turn 12: Blossombind on our own Explosive Prodigy because they had no creatures.
    test("a lock Aura with only our own creatures to enchant is held") {
        val d = driver()
        d.putCreatureOnBattlefield(d.player1, "Explosive Prodigy")
        val aura = d.putCardInHand(d.player1, "Blossombind")
        lands(d, d.player1, "Island" to 2)
        val choices = d.legalActions(d.player1).filter {
            (it.action as? CastSpell)?.cardId == aura || it.action is PassPriority
        }

        (choose(live, d, choices).action as? CastSpell)?.cardId shouldBe aura
        choose(fixed, d, choices).action.shouldBeInstanceOf<PassPriority>()
    }

    // Game 3 turn 21: Prohibit aimed at our own Treefolk Healer, which it could not even counter.
    test("a counterspell with only our own spell on the stack is held") {
        val d = driver()
        lands(d, d.player1, "Forest" to 5, "Island" to 4)
        val healer = d.putCardInHand(d.player1, "Treefolk Healer")
        val prohibit = d.putCardInHand(d.player1, "Prohibit")
        d.castSpell(d.player1, healer)
        d.state.stack.size shouldBe 1
        val choices = d.legalActions(d.player1).filter {
            (it.action as? CastSpell)?.cardId == prohibit || it.action is PassPriority
        }

        choose(fixed, d, choices).action.shouldBeInstanceOf<PassPriority>()
    }

    // Game 3 turn 19: kicked Tolarian Emissary destroyed our own Traveler's Cloak, the only
    // enchantment on the table. An anthem stands in for the Cloak: same trigger, same lone target.
    test("the unkicked cast stays a candidate when the kicker can only hit our own permanent") {
        val d = driver()
        d.putPermanentOnBattlefield(d.player1, "Glorious Anthem")
        d.putCreatureOnBattlefield(d.player1, "Grizzly Bears")
        lands(d, d.player1, "Island" to 3, "Plains" to 2)
        val emissary = d.putCardInHand(d.player1, "Tolarian Emissary")
        val choices = d.legalActions(d.player1).filter {
            (it.action as? CastSpell)?.cardId == emissary || it.action is PassPriority
        }

        // The live profile only ever sees the kicked cast, so its options are "blow up our own
        // anthem" or "pass" — never the plain 1/2 flier that was the right play.
        val liveChoice = choose(live, d, choices).action
        (liveChoice is CastSpell && liveChoice.declaredCostSlot == null) shouldBe false
        val chosen = choose(fixed, d, choices).action
        chosen.shouldBeInstanceOf<CastSpell>()
        chosen.declaredCostSlot shouldBe null
    }
})
