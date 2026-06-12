package com.wingedsheep.mtg.sets.definitions.otj.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.MayEffect
import com.wingedsheep.sdk.scripting.effects.SearchDestination

/**
 * Silver Deputy
 * {2}
 * Artifact Creature — Mercenary
 * 1/2
 * When this creature enters, you may search your library for a basic land card or a Desert card,
 * reveal it, then shuffle and put it on top.
 * {T}: Target creature you control gets +1/+0 until end of turn. Activate only as a sorcery.
 */
val SilverDeputy = card("Silver Deputy") {
    manaCost = "{2}"
    colorIdentity = ""
    typeLine = "Artifact Creature — Mercenary"
    power = 1
    toughness = 2
    oracleText = "When this creature enters, you may search your library for a basic land card or a Desert card, reveal it, then shuffle and put it on top.\n{T}: Target creature you control gets +1/+0 until end of turn. Activate only as a sorcery."

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = MayEffect(
            Patterns.Library.searchLibrary(
                filter = GameObjectFilter.BasicLand or GameObjectFilter.Land.withSubtype("Desert"),
                count = 1,
                destination = SearchDestination.TOP_OF_LIBRARY,
                reveal = true
            )
        )
    }

    activatedAbility {
        cost = Costs.Tap
        val creature = target("target creature you control", Targets.CreatureYouControl)
        effect = Effects.ModifyStats(1, 0, creature)
        timing = TimingRule.SorcerySpeed
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "248"
        artist = "Artur Nakhodkin"
        flavorText = "It couldn't comprehend the spirit of the law, so the Sterling Company settled for the letter."
        imageUri = "https://cards.scryfall.io/normal/front/3/9/39d2c11d-1eb8-4768-bc61-fa8f20a69462.jpg?1712356286"
    }
}
