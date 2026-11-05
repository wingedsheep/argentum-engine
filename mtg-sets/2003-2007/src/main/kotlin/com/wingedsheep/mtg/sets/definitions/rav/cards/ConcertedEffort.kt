package com.wingedsheep.mtg.sets.definitions.rav.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.core.Step

/**
 * Concerted Effort — Ravnica: City of Guilds #8
 * {2}{W}{W} · Enchantment
 *
 * At the beginning of each upkeep, creatures you control gain flying until end of turn if a
 * creature you control has flying. The same is true for fear, first strike, double strike,
 * landwalk, protection, trample, and vigilance.
 *
 * The Odric, Lunarch Marshal shape: one [Effects.If] per fixed keyword, each fanning an
 * end-of-turn grant over the creatures you control with [Effects.ForEachInGroup]. The gates are
 * resolution-time tests and the grants are snapshotted per creature, which is exactly the rulings:
 * creatures keep what they gained even if Concerted Effort or the creature that supplied it leaves.
 *
 * "Landwalk" means every landwalk *variant* a creature has — each is its own keyword (swampwalk,
 * forestwalk, …, nonbasic landwalk), so each is its own clause. "Protection" is parameterised by
 * quality, so it can't be a fixed-keyword clause: [Effects.GrantKeywordVariantsSharedByGroup] (`Keyword.PROTECTION`) reads
 * every protection any creature you control has and grants all of them — the ruling's creature
 * with protection from red plus one with protection from green give everyone both. It needs no
 * gate: with no protection in the group it grants nothing.
 *
 * "Each upkeep" is `Triggers.anyPlayer.beginningOf(Step.UPKEEP)`, so it works on opponents' turns as well.
 */
private val SHARED_KEYWORDS = listOf(
    Keyword.FLYING,
    Keyword.FEAR,
    Keyword.FIRST_STRIKE,
    Keyword.DOUBLE_STRIKE,
    Keyword.SWAMPWALK,
    Keyword.FORESTWALK,
    Keyword.ISLANDWALK,
    Keyword.MOUNTAINWALK,
    Keyword.PLAINSWALK,
    Keyword.DESERTWALK,
    Keyword.NONBASIC_LANDWALK,
    Keyword.TRAMPLE,
    Keyword.VIGILANCE,
)

private val CREATURES_YOU_CONTROL = GroupFilter(GameObjectFilter.Creature.youControl())

val ConcertedEffort = card("Concerted Effort") {
    manaCost = "{2}{W}{W}"
    colorIdentity = "W"
    typeLine = "Enchantment"
    oracleText = "At the beginning of each upkeep, creatures you control gain flying until end of turn if a " +
        "creature you control has flying. The same is true for fear, first strike, double strike, landwalk, " +
        "protection, trample, and vigilance."

    triggeredAbility {
        trigger = Triggers.anyPlayer.beginningOf(Step.UPKEEP)
        effect = Effects.Composite(
            SHARED_KEYWORDS.map { keyword ->
                Effects.If(
                    condition = Conditions.ControlCreatureWithKeyword(keyword),
                    then = Effects.ForEachInGroup(
                        CREATURES_YOU_CONTROL,
                        Effects.GrantKeyword(keyword, EffectTarget.IterationEntity, Duration.EndOfTurn)
                    )
                )
            } + Effects.ForEachInGroup(
                CREATURES_YOU_CONTROL,
                Effects.GrantKeywordVariantsSharedByGroup(Keyword.PROTECTION, CREATURES_YOU_CONTROL, EffectTarget.IterationEntity)
            ),
            descriptionOverride = "Creatures you control gain flying until end of turn if a creature you " +
                "control has flying. The same is true for fear, first strike, double strike, landwalk, " +
                "protection, trample, and vigilance."
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "8"
        artist = "Michael Sutfin"
        imageUri = "https://cards.scryfall.io/normal/front/4/a/4afa3367-eb7e-4c92-96a1-2b6865b24f52.jpg?1783943704"
        ruling("2006-05-01", "In Two-Headed Giant, triggers only once per upkeep, not once for each player.")
        ruling("2005-10-01", "For example, a player controls three creatures when Concerted Effort’s ability resolves: one creature with flying and protection from red, one with islandwalk and protection from green; and one with vigilance. All three creatures will have flying, islandwalk, vigilance, protection from red, and protection from green until the end of the turn.")
        ruling("2005-10-01", "Creatures keep all abilities granted this way until the end of the turn, even if Concerted Effort or the original creature with that ability leaves the battlefield.")
    }
}
