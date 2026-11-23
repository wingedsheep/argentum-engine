package com.wingedsheep.mtg.sets.definitions.gtc.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.events.AttackPredicate
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Ordruun Veteran
 * {2}{R}{W}
 * Creature — Minotaur Soldier
 * 3/1
 * Battalion — Whenever this creature and at least two other creatures attack, this creature
 * gains double strike until end of turn.
 */
val OrdruunVeteran = card("Ordruun Veteran") {
    manaCost = "{2}{R}{W}"
    colorIdentity = "RW"
    typeLine = "Creature — Minotaur Soldier"
    power = 3
    toughness = 1
    oracleText = "Battalion — Whenever this creature and at least two other creatures attack, this creature " +
        "gains double strike until end of turn. (It deals both first-strike and regular combat damage.)"

    triggeredAbility {
        trigger = Triggers.self.attacks(setOf(AttackPredicate.AttackerCountAtLeast(3)))
        effect = Effects.GrantKeyword(Keyword.DOUBLE_STRIKE, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "184"
        artist = "Greg Staples"
        flavorText = "\"My father was Gruul, but I chose the precision of the legion over the fury of the pack.\""
        imageUri = "https://cards.scryfall.io/normal/front/2/0/20fea3f6-e64a-4964-86bc-c0b8fef0ab25.jpg?1783940103"
    }
}
