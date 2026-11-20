package com.wingedsheep.mtg.sets.definitions.ths.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CanBlockAdditionalForCreatureGroup
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Coastline Chimera
 * {3}{U}
 * Creature — Chimera
 * 1/5
 *
 * Flying
 * {1}{W}: This creature can block an additional creature this turn.
 *
 * Each activation grants its own "can block an additional creature" static
 * ([CanBlockAdditionalForCreatureGroup] scoped to the holder) until end of turn — Act of
 * Heroism's shape — so activations are cumulative, as the ruling requires.
 */
val CoastlineChimera = card("Coastline Chimera") {
    manaCost = "{3}{U}"
    colorIdentity = "UW"
    typeLine = "Creature — Chimera"
    power = 1
    toughness = 5
    oracleText = "Flying\n{1}{W}: This creature can block an additional creature this turn."

    keywords(Keyword.FLYING)

    activatedAbility {
        cost = Costs.Mana("{1}{W}")
        effect = Effects.GrantStaticAbility(
            CanBlockAdditionalForCreatureGroup(count = 1, filter = GroupFilter.source()),
            EffectTarget.Self,
            Duration.EndOfTurn,
        )
        description = "This creature can block an additional creature this turn."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "44"
        artist = "Dan Murayama Scott"
        flavorText = "Seeing a chimera overhead foretells good fortune, but only because seeing one any closer foretells dismemberment."
        imageUri = "https://cards.scryfall.io/normal/front/0/1/01320a31-25fa-4324-8225-d150c6aa58a1.jpg?1783939800"
        ruling("2013-09-15", "Coastline Chimera's activated ability is cumulative. Activating it a second time will allow it to block three creatures, and so on.")
    }
}
