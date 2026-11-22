package com.wingedsheep.mtg.sets.definitions.gtc.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Truefire Paladin
 * {R}{W}
 * Creature — Human Knight
 * 2/2
 * Vigilance
 * {R}{W}: This creature gets +2/+0 until end of turn.
 * {R}{W}: This creature gains first strike until end of turn.
 */
val TruefirePaladin = card("Truefire Paladin") {
    manaCost = "{R}{W}"
    colorIdentity = "RW"
    typeLine = "Creature — Human Knight"
    oracleText = "Vigilance\n" +
        "{R}{W}: This creature gets +2/+0 until end of turn.\n" +
        "{R}{W}: This creature gains first strike until end of turn."
    power = 2
    toughness = 2

    keywords(Keyword.VIGILANCE)

    activatedAbility {
        cost = Costs.Mana("{R}{W}")
        effect = Effects.ModifyStats(2, 0, EffectTarget.Self)
    }

    activatedAbility {
        cost = Costs.Mana("{R}{W}")
        effect = Effects.GrantKeyword(Keyword.FIRST_STRIKE, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "202"
        artist = "Michael C. Hayes"
        imageUri = "https://cards.scryfall.io/normal/front/3/9/39610192-6d3c-4d03-9c3e-cda966c924b1.jpg"
    }
}
