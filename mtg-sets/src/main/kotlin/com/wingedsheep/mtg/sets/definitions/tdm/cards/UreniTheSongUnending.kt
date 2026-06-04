package com.wingedsheep.mtg.sets.definitions.tdm.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.ProtectionScope
import com.wingedsheep.sdk.scripting.effects.DividedDamageEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Ureni, the Song Unending — Tarkir: Dragonstorm #233
 * {5}{G}{U}{R} · Legendary Creature — Spirit Dragon · Mythic
 * 10/10
 *
 * Flying, protection from white and from black
 * When Ureni enters, it deals X damage divided as you choose among any number of target creatures
 * and/or planeswalkers your opponents control, where X is the number of lands you control.
 *
 * The ETB is a triggered ability whose targets ("any number of ... your opponents control") are
 * chosen as it goes on the stack via an unlimited [TargetObject]; the total (X = lands you control)
 * is computed when it resolves and divided among the chosen targets ([DividedDamageEffect] with
 * [DividedDamageEffect.dynamicTotal]). With zero targets chosen, nothing happens.
 */
val UreniTheSongUnending = card("Ureni, the Song Unending") {
    manaCost = "{5}{G}{U}{R}"
    colorIdentity = "GUR"
    typeLine = "Legendary Creature — Spirit Dragon"
    power = 10
    toughness = 10
    oracleText = "Flying, protection from white and from black\n" +
        "When Ureni enters, it deals X damage divided as you choose among any number of target " +
        "creatures and/or planeswalkers your opponents control, where X is the number of lands you control."

    keywords(Keyword.FLYING)
    keywordAbility(KeywordAbility.Protection(ProtectionScope.Colors(setOf(Color.WHITE, Color.BLACK))))

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        // "any number of target" — but divided damage requires at least 1 per chosen target, so
        // the number of targets can never exceed X (lands you control). dynamicMaxCount enforces
        // that cap at the moment the trigger goes on the stack; optional allows choosing zero.
        target = TargetObject(
            optional = true,
            filter = TargetFilter(GameObjectFilter.CreatureOrPlaneswalker.opponentControls()),
            dynamicMaxCount = DynamicAmounts.landsYouControl(),
            id = "target creatures and/or planeswalkers your opponents control"
        )
        effect = DividedDamageEffect(
            totalDamage = 0,
            dynamicTotal = DynamicAmounts.landsYouControl()
        )
        description = "When Ureni enters, it deals X damage divided as you choose among any number of " +
            "target creatures and/or planeswalkers your opponents control, where X is the number of " +
            "lands you control."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "233"
        artist = "Alexander Ostrowski"
        imageUri = "https://cards.scryfall.io/normal/front/2/2/227802c0-4ff6-43a8-a850-ed0f546dc5ac.jpg?1743204921"
    }
}
