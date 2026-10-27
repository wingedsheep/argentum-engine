package com.wingedsheep.mtg.sets.definitions.xln.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Sanctum Seeker
 * {2}{B}{B}
 * Creature — Vampire Knight
 * 3/4
 * Whenever a Vampire you control attacks, each opponent loses 1 life and you gain 1 life.
 *
 * The gain is a flat 1 life, not "the life lost this way" — so it isn't a drain.
 */
val SanctumSeeker = card("Sanctum Seeker") {
    manaCost = "{2}{B}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Vampire Knight"
    oracleText = "Whenever a Vampire you control attacks, each opponent loses 1 life and you gain 1 life."
    power = 3
    toughness = 4

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Creature.withSubtype(Subtype.VAMPIRE).youControl()).attacks()
        effect = Effects.LoseLife(1, EffectTarget.PlayerRef(Player.EachOpponent)) then Effects.GainLife(1)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "120"
        artist = "Volkan Baǵa"
        flavorText = "The Sanctum Seekers were the first of the knightly orders to cross the sea and are still the most zealous."
        imageUri = "https://cards.scryfall.io/normal/front/7/2/72150779-0bb6-4d20-a898-cda93a66e7cd.jpg?1783935756"
        ruling("2017-09-29", "In a Two-Headed Giant game, Sanctum Seeker's ability causes the opposing team to lose 2 life and you to gain 1 life.")
    }
}
