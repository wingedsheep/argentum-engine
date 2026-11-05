package com.wingedsheep.mtg.sets.definitions.xln.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Verdant Sun's Avatar
 * {5}{G}{G}
 * Creature — Dinosaur Avatar
 * 5/5
 * Whenever this creature or another creature you control enters, you gain life equal to that
 * creature's toughness.
 *
 * "This creature or another" is the any-binding enters trigger, so the Avatar's own arrival
 * triggers it too. The toughness is read on resolution from the triggering creature, falling back
 * to its last-known toughness if it has left the battlefield (ruling 2017-09-29).
 */
val VerdantSunsAvatar = card("Verdant Sun's Avatar") {
    manaCost = "{5}{G}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Dinosaur Avatar"
    power = 5
    toughness = 5
    oracleText = "Whenever this creature or another creature you control enters, you gain life equal to that creature's toughness."

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Creature.youControl()).enters()
        effect = Effects.GainLife(DynamicAmounts.toughnessOf(EffectTarget.TriggeringEntity), EffectTarget.Controller)
        description = "Whenever this creature or another creature you control enters, you gain life equal to that creature's toughness."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "213"
        artist = "Izzy"
        flavorText = "\"Ixalli's fertile rays enliven us all. Each of us is a testament to the sun's strength and love.\"\n—Chitlati, Lightning Rider"
        imageUri = "https://cards.scryfall.io/normal/front/9/d/9dbb5b6a-dc74-4e3e-9de1-5b379abdf2b4.jpg?1783935717"
        ruling(
            "2017-09-29",
            "The entering creature's toughness is determined as the ability of Verdant Sun's Avatar resolves. If that creature has left the battlefield, use its toughness as it last existed on the battlefield. If the creature's toughness was less than 0, your life total won't change."
        )
    }
}
