package com.wingedsheep.mtg.sets.definitions.vow.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Toxic Scorpion
 * {1}{G}
 * Creature — Scorpion
 * 1/1
 * Deathtouch
 * When this creature enters, another target creature you control gains deathtouch until end of turn.
 */
val ToxicScorpion = card("Toxic Scorpion") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Scorpion"
    oracleText = "Deathtouch\nWhen this creature enters, another target creature you control gains deathtouch until end of turn."
    power = 1
    toughness = 1
    keywords(Keyword.DEATHTOUCH)
    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        val t = target("target", TargetCreature(filter = TargetFilter.OtherCreatureYouControl))
        effect = Effects.GrantKeyword(Keyword.DEATHTOUCH, t)
    }
    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "224"
        artist = "Simon Dominic"
        flavorText = "\"You want that venom? Collect it yourself! I'm not losing another pair of dragonhide gloves.\"\n—Old Rutstein"
        imageUri = "https://cards.scryfall.io/normal/front/4/c/4cfca481-cf2d-435d-b4b6-07b1fe2a8e5d.jpg?1782703036"
    }
}
