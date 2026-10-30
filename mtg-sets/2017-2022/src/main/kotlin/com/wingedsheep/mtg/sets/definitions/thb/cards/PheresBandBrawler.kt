package com.wingedsheep.mtg.sets.definitions.thb.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Pheres-Band Brawler
 * {4}{G}{G}
 * Creature — Centaur Warrior
 * 4/4
 * When this creature enters, it fights up to one target creature you don't control.
 *
 * Wicked Wolf's enters-fight: `optional = true` is the "up to one". If the Brawler has left the
 * battlefield by resolution, [Effects.Fight] deals no damage either way (ruling, CR 701.14b).
 */
val PheresBandBrawler = card("Pheres-Band Brawler") {
    manaCost = "{4}{G}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Centaur Warrior"
    power = 4
    toughness = 4
    oracleText = "When this creature enters, it fights up to one target creature you don't control. " +
        "(Each deals damage equal to its power to the other.)"

    triggeredAbility {
        trigger = Triggers.self.enters()
        val foe = target(TargetFilter.CreatureOpponentControls, optional = true)
        effect = Effects.Fight(EffectTarget.Self, foe)
        description = "When this creature enters, it fights up to one target creature you don't control."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "193"
        artist = "John Stanko"
        imageUri = "https://cards.scryfall.io/normal/front/1/0/10ac20a9-6b10-4165-ad81-a47587b64d1e.jpg?1783931531"
        ruling(
            "2020-01-24",
            "If the target creature is an illegal target when Pheres-Band Brawler's ability tries to resolve, " +
                "the ability doesn't resolve. If Pheres-Band Brawler is no longer on the battlefield, the target " +
                "creature won't deal or be dealt damage."
        )
    }
}
