package com.wingedsheep.mtg.sets.definitions.fem.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.ProtectionScope
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Order of Leitbur
 * {W}{W}
 * Creature — Human Cleric Knight
 * 2/1
 * Protection from black
 * {W}: This creature gains first strike until end of turn.
 * {W}{W}: This creature gets +1/+0 until end of turn.
 *
 * The black mirror of [OrderOfTheEbonHand].
 */
val OrderOfLeitbur = card("Order of Leitbur") {
    manaCost = "{W}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Cleric Knight"
    oracleText = "Protection from black\n" +
        "{W}: This creature gains first strike until end of turn.\n" +
        "{W}{W}: This creature gets +1/+0 until end of turn."
    power = 2
    toughness = 1

    keywordAbility(KeywordAbility.Protection(ProtectionScope.Color(Color.BLACK)))

    activatedAbility {
        cost = Costs.Mana("{W}")
        effect = Effects.GrantKeyword(Keyword.FIRST_STRIKE, EffectTarget.Self)
    }

    activatedAbility {
        cost = Costs.Mana("{W}{W}")
        effect = Effects.ModifyStats(1, 0, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "16a"
        artist = "Bryon Wackwitz"
        flavorText = "Followers of Tourach regarded all other religions equally: with open contempt. Not so the followers of Leitbur, who made it their mission to eradicate the Order of the Ebon Hand."
        imageUri = "https://cards.scryfall.io/normal/front/e/b/ebd6e51e-f042-4673-a898-291607105829.jpg?1783947914"
    }
}
