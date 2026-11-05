package com.wingedsheep.mtg.sets.definitions.akh.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Samut, Voice of Dissent
 * {3}{R}{G}
 * Legendary Creature — Human Warrior
 * 3/4
 * Flash
 * Double strike, vigilance, haste
 * Other creatures you control have haste.
 * {W}, {T}: Untap another target creature.
 */
val SamutVoiceOfDissent = card("Samut, Voice of Dissent") {
    manaCost = "{3}{R}{G}"
    colorIdentity = "GRW"
    typeLine = "Legendary Creature — Human Warrior"
    oracleText = "Flash\n" +
        "Double strike, vigilance, haste\n" +
        "Other creatures you control have haste.\n" +
        "{W}, {T}: Untap another target creature."
    power = 3
    toughness = 4

    keywords(Keyword.FLASH, Keyword.DOUBLE_STRIKE, Keyword.VIGILANCE, Keyword.HASTE)

    staticAbility {
        ability = GrantKeyword(Keyword.HASTE, GroupFilter.AllCreaturesYouControl.other())
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{W}"), Costs.Tap)
        val creature = target(TargetFilter.Creature.other())
        effect = Effects.Untap(creature)
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "205"
        artist = "Aleksi Briclot"
        flavorText = "\"I'll fight no more just for the honor of dying. The afterlife will have to wait.\""
        imageUri = "https://cards.scryfall.io/normal/front/6/0/60f2d8f1-43a9-438a-830c-9d6b5701f950.jpg"
    }
}
