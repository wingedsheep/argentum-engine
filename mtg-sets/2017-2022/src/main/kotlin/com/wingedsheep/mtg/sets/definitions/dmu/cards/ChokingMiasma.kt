package com.wingedsheep.mtg.sets.definitions.dmu.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Choking Miasma
 * {1}{B}{B}
 * Sorcery
 *
 * Kicker {G}
 * If this spell was kicked, put a +1/+1 counter on a creature you control.
 * All creatures get -2/-2 until end of turn.
 *
 * The kicked counter doesn't target: the creature is chosen on resolution (Kayla's Command's
 * non-targeting `selectTarget` shape), before the global -2/-2 applies.
 */
val ChokingMiasma = card("Choking Miasma") {
    manaCost = "{1}{B}{B}"
    colorIdentity = "BG"
    typeLine = "Sorcery"
    oracleText = "Kicker {G} (You may pay an additional {G} as you cast this spell.)\n" +
        "If this spell was kicked, put a +1/+1 counter on a creature you control.\n" +
        "All creatures get -2/-2 until end of turn."

    keywordAbility(KeywordAbility.kicker("{G}"))

    spell {
        effect = Effects.If(
            Conditions.WasKicked,
            Effects.Pipeline {
                val chosen = selectTarget(
                    TargetObject(filter = TargetFilter.CreatureYouControl),
                    nonTargeting = true,
                    prompt = "Choose a creature you control to get a +1/+1 counter"
                )
                run(Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, chosen.asTarget))
            }
        ) then Patterns.Group.modifyStatsForAll(-2, -2, GroupFilter.AllCreatures)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "86"
        artist = "Inka Schulz"
        flavorText = "The Phyrexians were as surprised as anyone to discover they still needed to breathe."
        imageUri = "https://cards.scryfall.io/normal/front/b/a/ba537452-3dab-4ecd-b65f-9e311a130d31.jpg?1783921335"
    }
}
