package com.wingedsheep.mtg.sets.definitions.ptk.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.LookAtTargetHandEffect

/**
 * Wu Scout
 * {1}{U}
 * Creature — Human Soldier Scout
 * 1/1
 * Horsemanship
 * When this creature enters, look at target opponent's hand.
 */
val WuScout = card("Wu Scout") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Human Soldier Scout"
    power = 1
    toughness = 1
    oracleText =
        "Horsemanship (This creature can't be blocked except by creatures with horsemanship.)\n" +
        "When this creature enters, look at target opponent's hand."

    keywordAbility(KeywordAbility.Simple(Keyword.HORSEMANSHIP))

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        val t = target("target", Targets.Opponent)
        effect = LookAtTargetHandEffect(t)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "62"
        artist = "Jiaming"
        imageUri = "https://cards.scryfall.io/normal/front/8/d/8d13330f-6e07-451f-b17a-4e1606a74c3f.jpg"
    }
}
