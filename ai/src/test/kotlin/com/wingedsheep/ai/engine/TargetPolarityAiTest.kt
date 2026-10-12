package com.wingedsheep.ai.engine

import com.wingedsheep.ai.engine.knowledge.IntentCatalog
import com.wingedsheep.ai.engine.knowledge.TargetPolarity
import com.wingedsheep.ai.engine.knowledge.TargetPolarityAnalyzer
import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.AlternativeCostType
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
import com.wingedsheep.mtg.sets.definitions.vow.cards.MischievousCatgeist
import com.wingedsheep.mtg.sets.definitions.rav.cards.StrandsOfUndeath
import com.wingedsheep.mtg.sets.definitions.woe.cards.DiminisherWitch
import com.wingedsheep.mtg.sets.definitions.woe.cards.EmberethVeteran
import com.wingedsheep.mtg.sets.tokens.PredefinedTokens
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.battlefield.AttachmentsComponent
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
        GloriousAnthem, MischievousCatgeist, EmberethVeteran, StrandsOfUndeath, DiminisherWitch,
        PredefinedTokens.YoungHeroRole, PredefinedTokens.CursedRole,
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
        test("an Aura granting a card-draw trigger is a gift, and its self-exile is no veto") {
            // Catlike Curiosity: "Enchanted creature has 'whenever this creature deals combat damage
            // to a player, draw a card'" plus disturb's exile-instead redirect on the Aura itself.
            TargetPolarityAnalyzer.forSpell(MischievousCatgeist.backFace!!) shouldBe listOf(TargetPolarity.BENEFICIAL)
        }
        test("stealing and pumping are one-sided") {
            TargetPolarityAnalyzer.forSpell(BlatantThievery) shouldBe listOf(TargetPolarity.HARMFUL)
            TargetPolarityAnalyzer.forSpell(GiantGrowth) shouldBe listOf(TargetPolarity.BENEFICIAL)
        }
        test("a Role token reads as what the Role does to the creature it enchants") {
            val lookup: (String) -> com.wingedsheep.sdk.model.CardDefinition? = registry::getCard
            // Young Hero Role's +1/+1-counter attack trigger is a gift.
            val veteran = EmberethVeteran.script.activatedAbilities.single()
            TargetPolarityAnalyzer.forAbility(EmberethVeteran, veteran, lookup) shouldBe
                listOf(TargetPolarity.BENEFICIAL)
            // Cursed Role's base 1/1 is a curse.
            val witch = DiminisherWitch.script.triggeredAbilities.single()
            TargetPolarityAnalyzer.forRequirements(witch.effect, listOfNotNull(witch.targetRequirement), lookup) shouldBe
                listOf(TargetPolarity.HARMFUL)
            // Without the token's definition the slot stays unread rather than guessed.
            TargetPolarityAnalyzer.forRequirements(veteran.effect, veteran.targetRequirements) shouldBe
                listOf(TargetPolarity.UNKNOWN)
        }
        test("an Aura whose own ability regenerates the enchanted creature is a gift") {
            TargetPolarityAnalyzer.forSpell(StrandsOfUndeath) shouldBe listOf(TargetPolarity.BENEFICIAL)
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

    // Engine-vs-engine VOW game: Mischievous Catgeist disturbed as Catlike Curiosity onto the
    // opponent's Shriekmaw, which then drew them two cards. A disturb cast puts the *back* face on
    // the stack, so its slot must be read off the back face, not the creature front with no targets.
    test("a disturbed card-draw Aura goes on our own creature, not theirs") {
        val d = driver()
        val ours = d.putCreatureOnBattlefield(d.player1, "Gustcloak Harrier")
        val theirs = d.putCreatureOnBattlefield(d.player2, "Grizzly Bears")
        lands(d, d.player1, "Island" to 3)
        val catgeist = d.putCardInGraveyard(d.player1, "Mischievous Catgeist")
        val disturb = d.legalActions(d.player1).single {
            val cast = it.action as? CastSpell
            cast?.cardId == catgeist && cast.alternativeCostType == AlternativeCostType.DISTURB
        }
        val intents = IntentCatalog.of(registry)

        TargetSelection.polarities(d.state, disturb, intents) shouldBe listOf(TargetPolarity.BENEFICIAL)

        fun aimedAt(polarity: Boolean): EntityId {
            val filled = TargetSelection.fillHeuristically(
                d.state, disturb, d.player1, fillPartialRequirements = true,
                intents = intents, polarityFromEffect = polarity,
            ) as CastSpell
            return (filled.targets.single() as ChosenTarget.Permanent).entityId
        }

        aimedAt(polarity = false) shouldBe theirs
        aimedAt(polarity = true) shouldBe ours
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

    // 4p log turn 24: Embereth Veteran sacrificed to put a Young Hero Role on the opponent's 7/5.
    test("a beneficial Role goes on our own creature, not theirs") {
        val d = driver()
        val veteran = d.putCreatureOnBattlefield(d.player1, "Embereth Veteran")
        val ours = d.putCreatureOnBattlefield(d.player1, "Grizzly Bears")
        val theirs = d.putCreatureOnBattlefield(d.player2, "Gustcloak Harrier")
        lands(d, d.player1, "Mountain" to 1)
        val activation = d.legalActions(d.player1).single { (it.action as? ActivateAbility)?.sourceId == veteran }
        val intents = IntentCatalog.of(registry)

        fun aimedAt(polarity: Boolean): EntityId {
            val filled = TargetSelection.fillHeuristically(
                d.state, activation, d.player1, fillPartialRequirements = true,
                intents = intents, polarityFromEffect = polarity,
            ) as ActivateAbility
            return (filled.targets.single() as ChosenTarget.Permanent).entityId
        }

        aimedAt(polarity = false) shouldBe theirs
        aimedAt(polarity = true) shouldBe ours
    }

    // 4p log turn 14: Strands of Undeath went on the opponent's Goblin Rock Sled.
    test("an Aura with a regenerate ability for its creature is cast on our own creature") {
        val d = driver()
        val ours = d.putCreatureOnBattlefield(d.player1, "Grizzly Bears")
        val theirs = d.putCreatureOnBattlefield(d.player2, "Gustcloak Harrier")
        lands(d, d.player1, "Swamp" to 4)
        val strands = d.putCardInHand(d.player1, "Strands of Undeath")
        val cast = d.legalActions(d.player1).single { (it.action as? CastSpell)?.cardId == strands }
        val intents = IntentCatalog.of(registry)

        fun aimedAt(polarity: Boolean): EntityId {
            val filled = TargetSelection.fillHeuristically(
                d.state, cast, d.player1, fillPartialRequirements = true,
                intents = intents, polarityFromEffect = polarity,
            ) as CastSpell
            return (filled.targets.single() as ChosenTarget.Permanent).entityId
        }

        aimedAt(polarity = false) shouldBe theirs
        aimedAt(polarity = true) shouldBe ours
    }

    // 4p log turn 22: {B} paid to regenerate the opponent's creature our Strands enchanted.
    test("an Aura's regenerate ability is not activated for an opponent's creature") {
        val d = driver()
        lands(d, d.player1, "Swamp" to 1)
        val intents = IntentCatalog.of(registry)

        fun enchant(host: EntityId): LegalAction {
            val aura = d.putPermanentOnBattlefield(d.player1, "Strands of Undeath")
            d.replaceState(
                d.state
                    .updateEntity(aura) { it.with(AttachedToComponent(host)) }
                    .updateEntity(host) { it.with(AttachmentsComponent(listOf(aura))) },
            )
            return d.legalActions(d.player1).single { (it.action as? ActivateAbility)?.sourceId == aura }
        }

        val onTheirs = enchant(d.putCreatureOnBattlefield(d.player2, "Gustcloak Harrier"))
        TargetSelection.aimsOnlyAtTheWrongSide(d.state, onTheirs, d.player1, intents) shouldBe true
        val pass = d.legalActions(d.player1).single { it.action is PassPriority }
        choose(fixed, d, listOf(onTheirs, pass)).action.shouldBeInstanceOf<PassPriority>()

        val onOurs = enchant(d.putCreatureOnBattlefield(d.player1, "Grizzly Bears"))
        TargetSelection.aimsOnlyAtTheWrongSide(d.state, onOurs, d.player1, intents) shouldBe false
    }
})
