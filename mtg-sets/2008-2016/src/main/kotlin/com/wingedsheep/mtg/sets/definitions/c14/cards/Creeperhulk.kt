package com.wingedsheep.mtg.sets.definitions.c14.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Creeperhulk
 * {3}{G}{G}
 * Creature — Plant Elemental
 * 5/5
 * Trample
 * {1}{G}: Until end of turn, target creature you control has base power and toughness 5/5 and
 * gains trample.
 *
 * The ability *sets* base P/T (layer 7b) via `SetBasePowerAndToughness`, so +1/+1 counters and
 * pumps still apply on top, then grants trample until end of turn — one target, two effects.
 */
val Creeperhulk = card("Creeperhulk") {
    manaCost = "{3}{G}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Plant Elemental"
    power = 5
    toughness = 5
    oracleText = "Trample\n{1}{G}: Until end of turn, target creature you control has base power and " +
        "toughness 5/5 and gains trample."

    keywords(Keyword.TRAMPLE)

    activatedAbility {
        cost = Costs.Mana("{1}{G}")
        val creature = target(TargetFilter.CreatureYouControl)
        effect = Effects.SetBasePowerAndToughness(5, 5, creature) then
            Effects.GrantKeyword(Keyword.TRAMPLE, creature)
        description = "{1}{G}: Until end of turn, target creature you control has base power and " +
            "toughness 5/5 and gains trample."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "42"
        artist = "Ralph Horsley"
        flavorText = "\"I saw the devastation when the juggernauts crashed the walls of An Karras. " +
            "Then I got to thinking.\"\n—Janji, sylvan druid"
        imageUri = "https://cards.scryfall.io/normal/front/c/7/c70f3a79-5b79-4496-9234-58baeae74af3.jpg?1783938866"
    }
}
