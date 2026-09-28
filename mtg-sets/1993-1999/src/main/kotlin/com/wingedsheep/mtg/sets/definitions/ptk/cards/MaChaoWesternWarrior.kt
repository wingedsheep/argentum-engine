package com.wingedsheep.mtg.sets.definitions.ptk.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.events.AttackPredicate
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Ma Chao, Western Warrior
 * {3}{R}{R}
 * Legendary Creature — Human Soldier Warrior
 * 3/3
 * Horsemanship
 * Whenever Ma Chao attacks alone, it can't be blocked this combat.
 */
val MaChaoWesternWarrior = card("Ma Chao, Western Warrior") {
    manaCost = "{3}{R}{R}"
    colorIdentity = "R"
    typeLine = "Legendary Creature — Human Soldier Warrior"
    power = 3
    toughness = 3
    oracleText = "Horsemanship (This creature can't be blocked except by creatures with horsemanship.)\nWhenever Ma Chao attacks alone, it can't be blocked this combat."

    keywordAbility(KeywordAbility.Simple(Keyword.HORSEMANSHIP))

    triggeredAbility {
        trigger = Triggers.self.attacks(setOf(AttackPredicate.Alone))
        effect = Effects.GrantKeyword(AbilityFlag.CANT_BE_BLOCKED, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "116"
        artist = "Koji"
        imageUri = "https://cards.scryfall.io/normal/front/3/9/39b8cf7f-8c40-40e9-8118-e8626c3c6433.jpg?1783946105"
    }
}
