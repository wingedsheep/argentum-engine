package com.wingedsheep.ai.engine

import com.wingedsheep.ai.draftsim.DraftsimData
import com.wingedsheep.ai.draftsim.withFallbackRatings
import com.wingedsheep.ai.llm.CardSummary
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.mtg.sets.MtgSetCatalog
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.comparables.shouldBeGreaterThan
import io.kotest.matchers.shouldBe

/**
 * The bot's draft picks: Draftsim-scored, with cards a set's table leaves unrated — every card of a
 * set we ship no ratings file for — rated off their definitions instead of by rarity alone.
 */
class EngineAiDraftPickTest : FunSpec({

    // A set code no ratings file exists for, like a new or custom set.
    val unratedSet = "ZZZ"

    val murder = card("Test Murder") {
        manaCost = "{1}{B}"
        typeLine = "Instant"
        oracleText = "Destroy target creature."
        spell {
            val creature = target(TargetFilter.Creature)
            effect = Effects.Destroy(creature)
        }
        metadata { rarity = Rarity.COMMON }
    }
    val overcostedRare = card("Test Overcosted Rare") {
        manaCost = "{4}{W}"
        typeLine = "Creature — Soldier"
        power = 1
        toughness = 1
        metadata { rarity = Rarity.RARE }
    }

    fun CardDefinition.summary() = CardSummary(
        name = name,
        manaCost = manaCost.toString(),
        typeLine = typeLine.toString(),
        rarity = metadata.rarity.name,
        power = creatureStats?.basePower,
        toughness = creatureStats?.baseToughness,
        oracleText = oracleText,
        setCode = unratedSet,
    )

    fun bot(vararg cards: CardDefinition): EngineAiPlayerController {
        val registry = CardRegistry().apply { register(cards.toList()) }
        return EngineAiPlayerController(registry, EntityId.of("bot"), { null })
    }

    test("in a set with no ratings file the bot takes common removal over an overcosted rare") {
        val pick = bot(murder, overcostedRare).chooseDraftPick(
            pack = listOf(overcostedRare.summary(), murder.summary()),
            pickedSoFar = emptyList(),
            packNumber = 1,
            pickNumber = 1,
        )

        pick shouldContainExactly listOf("Test Murder")
    }

    test("fallback ratings fill only the cards a set's table leaves unrated") {
        val ltr = MtgSetCatalog.all.first { it.code == "LTR" }
        val ratedCard = ltr.cards.first { !it.typeLine.isLand && DraftsimData.nameKey(it.name) in DraftsimData.tablesFor(listOf("LTR")).ratings }
        val base = DraftsimData.tablesFor(listOf("LTR"))

        val tables = base.withFallbackRatings(listOf(ratedCard, murder, overcostedRare))

        tables.ratings[DraftsimData.nameKey(ratedCard.name)] shouldBe base.ratings[DraftsimData.nameKey(ratedCard.name)]
        tables.ratings.getValue(DraftsimData.nameKey("Test Murder")) shouldBeGreaterThan
            tables.ratings.getValue(DraftsimData.nameKey("Test Overcosted Rare"))
    }

    test("fallback draft picks a lasting creature over a four-mana sorcery pump") {
        val pump = card("Test Expensive Sorcery Pump") {
            manaCost = "{3}{G}"
            typeLine = "Sorcery"
            spell {
                val creature = target(TargetFilter.Creature)
                effect = Effects.ModifyStats(5, 5, creature) then Effects.MustBeBlocked(creature, allCreatures = false)
            }
            metadata { rarity = Rarity.UNCOMMON }
        }
        val creature = card("Test Solid Green Creature") {
            manaCost = "{2}{G}"
            typeLine = "Creature — Beast"
            power = 3
            toughness = 3
            metadata { rarity = Rarity.COMMON }
        }
        bot(pump, creature).chooseDraftPick(
            pack = listOf(pump.summary(), creature.summary()), pickedSoFar = emptyList(),
            packNumber = 1, pickNumber = 1,
        ) shouldContainExactly listOf(creature.name)
    }

    test("sorcery pump discount excludes instants, lasting effects, cantrips and lure") {
        fun pump(type: String, duration: com.wingedsheep.sdk.scripting.Duration, rider: String) = card("Test Pump $type $duration $rider") {
            manaCost = "{3}{G}"
            typeLine = type
            spell {
                val creature = target(TargetFilter.Creature)
                val stats = Effects.ModifyStats(5, 5, creature, duration)
                effect = when (rider) {
                    "draw" -> stats then Effects.DrawCards(1)
                    "lure" -> stats then Effects.MustBeBlocked(creature, allCreatures = true)
                    else -> stats
                }
            }
        }
        val eot = com.wingedsheep.sdk.scripting.Duration.EndOfTurn
        val plain = pump("Sorcery", eot, "none")
        LimitedCardRater.explain(plain).heuristic.any { it.part == "Sorcery pump" } shouldBe true
        for (control in listOf(
            pump("Instant", eot, "none"),
            pump("Sorcery", com.wingedsheep.sdk.scripting.Duration.Permanent, "none"),
            pump("Sorcery", eot, "draw"),
            pump("Sorcery", eot, "lure"),
        )) {
            LimitedCardRater.explain(control).heuristic.any { it.part == "Sorcery pump" } shouldBe false
        }
    }

})
