package com.wingedsheep.mtg.sets.definitions.chk.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetSpellOrPermanent

/**
 * Eight-and-a-Half-Tails
 * {W}{W}
 * Legendary Creature — Fox Cleric
 * 2/2
 * {1}{W}: Target permanent you control gains protection from white until end of turn.
 * {1}: Target spell or permanent becomes white until end of turn.
 */
val EightAndAHalfTails = card("Eight-and-a-Half-Tails") {
    manaCost = "{W}{W}"
    colorIdentity = "W"
    typeLine = "Legendary Creature — Fox Cleric"
    power = 2
    toughness = 2
    oracleText = "{1}{W}: Target permanent you control gains protection from white until end of turn.\n" +
        "{1}: Target spell or permanent becomes white until end of turn."

    activatedAbility {
        cost = Costs.Mana("{1}{W}")
        val permanent = target(TargetFilter.PermanentYouControl)
        effect = Effects.GrantProtectionFromColor(Color.WHITE, permanent)
        description = "{1}{W}: Target permanent you control gains protection from white until end of turn."
    }

    activatedAbility {
        cost = Costs.Mana("{1}")
        val spellOrPermanent = target(TargetSpellOrPermanent())
        effect = Effects.ChangeColor(spellOrPermanent, setOf(Color.WHITE))
        description = "{1}: Target spell or permanent becomes white until end of turn."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "8"
        artist = "Daren Bader"
        flavorText = "\"Virtue is an inner light that can prevail in every soul.\""
        imageUri = "https://cards.scryfall.io/normal/front/5/b/5bc72076-fb6c-420b-9c88-faabb9b91a03.jpg?1783944340"
        ruling(
            "2016-06-08",
            "A permanent spell that becomes white this way will enter the battlefield and continue to be white until end of turn."
        )
    }
}
