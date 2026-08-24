package com.wingedsheep.mtg.sets.definitions.ala.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Rakeclaw Gargantuan
 * {2}{R}{G}{W}
 * Creature — Beast
 * 5 / 3
 *
 * {1}: Target creature with power 5 or greater gains first strike until end of turn.
 *
 * A plain [Costs.Mana] activated ability. The power threshold is a filter on the target itself —
 * [TargetFilter.Creature].`powerAtLeast(5)` — so legality is checked on announcement and rechecked
 * on resolution; the grant is [Effects.GrantKeyword] on the bound target, whose default
 * `Duration.EndOfTurn` is exactly the printed "until end of turn".
 */
val RakeclawGargantuan = card("Rakeclaw Gargantuan") {
    manaCost = "{2}{R}{G}{W}"
    colorIdentity = "WRG"
    typeLine = "Creature — Beast"
    power = 5
    toughness = 3
    oracleText = "{1}: Target creature with power 5 or greater gains first strike until end of turn."

    activatedAbility {
        cost = Costs.Mana("{1}")
        val t = target("target", TargetCreature(filter = TargetFilter.Creature.powerAtLeast(5)))
        effect = Effects.GrantKeyword(Keyword.FIRST_STRIKE, t)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "186"
        artist = "Jesper Ejsing"
        flavorText = "Naya teems with gargantuans, titanic monsters to whom both nature and civilization defer."
        imageUri = "https://cards.scryfall.io/normal/front/d/1/d1995ab8-7382-4c2a-b8c7-8b9272cab4fb.jpg"
        ruling("2008-10-01", "The ability checks the targeted creature’s power twice: when the creature becomes the target, and when the ability resolves. Once the ability resolves, it will continue to apply to the affected creature no matter what its power may become later in the turn.")
    }
}
