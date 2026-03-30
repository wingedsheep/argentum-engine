package com.wingedsheep.mtg.sets.definitions.bloomburrow.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.TimingRule

/**
 * Raccoon Rallier
 * {1}{R}
 * Creature — Raccoon Bard
 * 2/2
 *
 * {T}: Target creature you control gains haste until end of turn. Activate only as a sorcery.
 */
val RaccoonRallier = card("Raccoon Rallier") {
    manaCost = "{1}{R}"
    typeLine = "Creature — Raccoon Bard"
    oracleText = "{T}: Target creature you control gains haste until end of turn. Activate only as a sorcery."
    power = 2
    toughness = 2

    activatedAbility {
        cost = AbilityCost.Tap
        val creature = target("creature you control", Targets.CreatureYouControl)
        effect = Effects.GrantKeyword(Keyword.HASTE, creature)
        timing = TimingRule.SorcerySpeed
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "148"
        artist = "Borja Pindado"
        flavorText = "\"I used to play for family only until I realized I could inspire an entire party with a single note.\""
        imageUri = "https://cards.scryfall.io/normal/front/b/5/b5b5180f-5a1c-4df8-9019-195e65a50ce3.jpg?1721426682"
    }
}
