package com.wingedsheep.mtg.sets.definitions.thb.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersWithCounters
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.MustAttack

/**
 * Underworld Rage-Hound
 * {1}{R}
 * Creature — Elemental Dog
 * 3/1
 * This creature attacks each combat if able.
 * Escape—{3}{R}, Exile three other cards from your graveyard.
 * This creature escapes with a +1/+1 counter on it.
 *
 * Juggernaut's static [MustAttack] (binds only when it is able to attack, per the ruling) plus
 * Pharika's Spawn's escape: [KeywordAbility.escape] and an [EntersWithCounters] replacement gated on
 * [Conditions.Escaped].
 */
val UnderworldRageHound = card("Underworld Rage-Hound") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Elemental Dog"
    power = 3
    toughness = 1
    oracleText = "This creature attacks each combat if able.\n" +
        "Escape—{3}{R}, Exile three other cards from your graveyard. " +
        "(You may cast this card from your graveyard for its escape cost.)\n" +
        "This creature escapes with a +1/+1 counter on it."

    staticAbility {
        ability = MustAttack()
    }

    keywordAbility(KeywordAbility.escape("{3}{R}", Costs.additional.ExileOtherCards(3)))

    replacementEffect(EntersWithCounters(
        counterType = CounterType.PLUS_ONE_PLUS_ONE,
        count = 1,
        selfOnly = true,
        condition = Conditions.Escaped
    ))

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "163"
        artist = "Tyler Walpole"
        imageUri = "https://cards.scryfall.io/normal/front/a/0/a04eef82-fd53-41f4-9c7e-28b9ac039032.jpg?1783931544"
        ruling(
            "2020-01-24",
            "If Underworld Rage-Hound can't attack for any reason (such as being tapped or having come under " +
                "that player's control that turn), then it doesn't attack. If there's a cost associated with " +
                "having it attack, the player isn't forced to pay that cost, so it doesn't have to attack in " +
                "that case either."
        )
        ruling(
            "2020-01-24",
            "After an escaped spell resolves, it returns to its owner's graveyard if it's not a permanent " +
                "spell. If it is a permanent spell, it enters the battlefield and will return to its owner's " +
                "graveyard if it dies later. Perhaps it will escape again—good underworld security is so hard " +
                "to come by these days."
        )
    }
}
