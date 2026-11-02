package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Saavik, Stoic Student
 * {4}{W}
 * Legendary Creature — Vulcan Advisor
 * 2/2
 *
 * Flash
 * Federation — When Saavik enters, she deals X damage to target attacking or blocking creature an
 * opponent controls, where X is the number of creature types among non-Borg creatures you control.
 *
 * Saavik is the damage source (the trigger's source permanent); X counts on resolution, Saavik's own
 * Vulcan and Advisor included.
 */
val SaavikStoicStudent = card("Saavik, Stoic Student") {
    manaCost = "{4}{W}"
    typeLine = "Legendary Creature — Vulcan Advisor"
    colorIdentity = "W"
    oracleText = "Flash\nFederation — When Saavik enters, she deals X damage to target attacking or blocking " +
        "creature an opponent controls, where X is the number of creature types among non-Borg creatures you control."
    power = 2
    toughness = 2
    keywords(Keyword.FLASH)
    triggeredAbility {
        trigger = Triggers.self.enters()
        val creature = target(TargetFilter.AttackingOrBlockingCreature.opponentControls())
        effect = Effects.DealDamage(DynamicAmounts.federation(), creature)
    }
    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "33"
        artist = "Mushk Rizvi"
        flavorText = "\"I don't believe this was a fair test of my command abilities.\""
        imageUri = "https://cards.scryfall.io/normal/front/3/7/3783c530-a2da-4dc1-811a-0a9a412c8e78.jpg?1785981363"
    }
}
