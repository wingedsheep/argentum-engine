package com.wingedsheep.mtg.sets.definitions.blb.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Hazardroot Herbalist
 * {2}{G}
 * Creature — Rabbit Druid
 * 1/4
 * Whenever you attack, target creature you control gets +1/+0 until end of turn.
 * If that creature is a token, it also gains deathtouch until end of turn.
 */
val HazardrootHerbalist = card("Hazardroot Herbalist") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Rabbit Druid"
    oracleText = "Whenever you attack, target creature you control gets +1/+0 until end of turn. If that creature is a token, it also gains deathtouch until end of turn."
    power = 1
    toughness = 4

    triggeredAbility {
        trigger = Triggers.YouAttack
        val creature = target("creature you control", TargetCreature(filter = TargetFilter.CreatureYouControl))
        effect = Effects.ModifyStats(1, 0, creature)
            .then(ConditionalEffect(
                condition = Conditions.TargetMatchesFilter(GameObjectFilter.Token, targetIndex = 0),
                effect = Effects.GrantKeyword(Keyword.DEATHTOUCH, creature)
            ))
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "174"
        artist = "Josiah \"Jo\" Cameron"
        flavorText = "Just because it's natural doesn't mean it's healthy."
        imageUri = "https://cards.scryfall.io/normal/front/e/2/e2882982-b3a3-4762-a550-6b82db1038e8.jpg?1721426818"
    }
}
