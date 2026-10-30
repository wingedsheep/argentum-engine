package com.wingedsheep.mtg.sets.definitions.iko.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Chittering Harvester
 * {5}{B}
 * Creature — Nightmare
 * 4/6
 * Mutate {4}{B}
 * Whenever this creature mutates, each opponent sacrifices a creature of their choice.
 */
val ChitteringHarvester = card("Chittering Harvester") {
    manaCost = "{5}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Nightmare"
    power = 4
    toughness = 6
    oracleText = "Mutate {4}{B} (If you cast this spell for its mutate cost, put it over or under target non-Human creature you own. They mutate into the creature on top plus all abilities from under it.)\n" +
        "Whenever this creature mutates, each opponent sacrifices a creature of their choice."

    keywordAbility(KeywordAbility.mutate("{4}{B}"))

    triggeredAbility {
        trigger = Triggers.self.mutates()
        effect = Effects.Sacrifice(GameObjectFilter.Creature, 1, EffectTarget.PlayerRef(Player.EachOpponent))
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "80"
        artist = "Grzegorz Rutkowski"
        imageUri = "https://cards.scryfall.io/normal/front/d/5/d51a0700-d4d3-4b7e-bdb5-6cd913c948d9.jpg?1783931064"
    }
}
