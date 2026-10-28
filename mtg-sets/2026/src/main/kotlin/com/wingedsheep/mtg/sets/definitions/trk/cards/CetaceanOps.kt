package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.WardCost
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Cetacean Ops
 * {3}{U}
 * Creature — Whale Officer
 * 3/3
 *
 * Ward {1} (Whenever this creature becomes the target of a spell or ability an opponent controls, counter it unless that player pays {1}.)
 * Whenever you cast a noncreature spell, put a +1/+1 counter on this creature.
 */
val CetaceanOps = card("Cetacean Ops") {
    manaCost = "{3}{U}"
    typeLine = "Creature — Whale Officer"
    colorIdentity = "U"
    oracleText = "Ward {1} (Whenever this creature becomes the target of a spell or ability an opponent controls, counter it unless that player pays {1}.)\nWhenever you cast a noncreature spell, put a +1/+1 counter on this creature."
    power = 3
    toughness = 3
    keywordAbility(KeywordAbility.Ward(WardCost.Mana("{1}")))
    triggeredAbility {
        trigger = Triggers.you.casts(GameObjectFilter.Noncreature)
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
    }
    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "48"
        artist = "Arthur Haas"
        flavorText = "Various sapient whale species serve in Starfleet as navigators, scientists, and explorers."
        imageUri = "https://cards.scryfall.io/normal/front/a/8/a8dc7ecd-e6f0-4050-9d5b-8436f60a1395.jpg?1792672912"
    }
}
