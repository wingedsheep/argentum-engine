package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.scripting.AddCountersEffect
import com.wingedsheep.sdk.scripting.EffectTarget
import com.wingedsheep.sdk.scripting.OnOtherCreatureWithSubtypeEnters

/**
 * Elvish Vanguard
 * {1}{G}
 * Creature — Elf Warrior
 * 1/1
 * Whenever another Elf enters the battlefield, put a +1/+1 counter on Elvish Vanguard.
 */
val ElvishVanguard = card("Elvish Vanguard") {
    manaCost = "{1}{G}"
    typeLine = "Creature — Elf Warrior"
    power = 1
    toughness = 1
    oracleText = "Whenever another Elf enters the battlefield, put a +1/+1 counter on Elvish Vanguard."

    triggeredAbility {
        trigger = OnOtherCreatureWithSubtypeEnters(Subtype("Elf"))
        effect = AddCountersEffect(
            counterType = "+1/+1",
            count = 1,
            target = EffectTarget.Self
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "259"
        artist = "Glen Angus"
        flavorText = "\"Our lives are woven together like the trees' branches over our heads, forming a canopy that protects us all.\""
        imageUri = "https://cards.scryfall.io/normal/front/4/5/455c6923-8d0e-4a7f-a5c0-add8db519ee3.jpg?1562911270"
    }
}
