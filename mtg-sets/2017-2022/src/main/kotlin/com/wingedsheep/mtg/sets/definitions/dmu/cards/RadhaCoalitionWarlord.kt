package com.wingedsheep.mtg.sets.definitions.dmu.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Radha, Coalition Warlord
 * {2}{R}{G}
 * Legendary Creature — Elf Warrior
 * 3/3
 * Domain — Whenever Radha becomes tapped, another target creature you control gets +X/+X until
 * end of turn, where X is the number of basic land types among lands you control.
 */
val RadhaCoalitionWarlord = card("Radha, Coalition Warlord") {
    manaCost = "{2}{R}{G}"
    colorIdentity = "RG"
    typeLine = "Legendary Creature — Elf Warrior"
    power = 3
    toughness = 3
    oracleText = "Domain — Whenever Radha becomes tapped, another target creature you control gets +X/+X until end of turn, where X is the number of basic land types among lands you control."

    triggeredAbility {
        trigger = Triggers.self.becomesTapped()
        val boosted = target(TargetFilter.OtherCreatureYouControl)
        val domain = DynamicAmounts.domain()
        effect = Effects.ModifyStats(domain, domain, boosted)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "211"
        artist = "Randy Vargas"
        flavorText = "\"Enough warm-up. I'm coming for you, Sheoldred!\""
        imageUri = "https://cards.scryfall.io/normal/front/2/c/2cec235e-7ce0-4947-b221-a555b2c865bd.jpg?1783921278"
    }
}
