package com.wingedsheep.mtg.sets.definitions.chk.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Hisoka's Guard
 * {1}{U}
 * Creature — Human Wizard
 * 1/1
 * You may choose not to untap this creature during your untap step.
 * {1}{U}, {T}: Target creature you control other than this creature has shroud for as long as
 * this creature remains tapped.
 *
 * The Everglove Courier shape: [AbilityFlag.MAY_NOT_UNTAP] lets the controller keep the Guard
 * tapped, and [Duration.WhileSourceTapped] drops the shroud grant the moment it untaps.
 */
val HisokasGuard = card("Hisoka's Guard") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Human Wizard"
    power = 1
    toughness = 1
    oracleText = "You may choose not to untap this creature during your untap step.\n" +
        "{1}{U}, {T}: Target creature you control other than this creature has shroud for as long as this creature remains tapped. (It can't be the target of spells or abilities.)"

    flags(AbilityFlag.MAY_NOT_UNTAP)

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}{U}"), Costs.Tap)
        val t = target(TargetFilter.OtherCreatureYouControl)
        effect = Effects.GrantKeyword(Keyword.SHROUD, t, Duration.WhileSourceTapped("this creature"))
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "68"
        artist = "Wayne England"
        imageUri = "https://cards.scryfall.io/normal/front/4/d/4d7aec86-7b12-4025-af06-1c1928e56c19.jpg?1783944326"
    }
}
