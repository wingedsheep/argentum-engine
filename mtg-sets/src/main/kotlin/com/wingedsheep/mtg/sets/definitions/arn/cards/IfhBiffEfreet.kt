package com.wingedsheep.mtg.sets.definitions.arn.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.DealDamageEffect
import com.wingedsheep.sdk.scripting.effects.ForEachPlayerEffect
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.dsl.Effects

/**
 * Ifh-Bíff Efreet
 * {2}{G}{G}
 * Creature — Efreet
 * 3/3
 * Flying
 * {G}: This creature deals 1 damage to each creature with flying and each player.
 * Any player may activate this ability.
 */
val IfhBiffEfreet = card("Ifh-Bíff Efreet") {
    manaCost = "{2}{G}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Efreet"
    power = 3
    toughness = 3
    oracleText = "Flying\n{G}: This creature deals 1 damage to each creature with flying and each player. Any player may activate this ability."

    keywords(Keyword.FLYING)

    activatedAbility {
        cost = Costs.Mana("{G}")
        effect = Effects.ForEachInGroup(
            GroupFilter(GameObjectFilter.Creature.withKeyword(Keyword.FLYING)),
            DealDamageEffect(1, EffectTarget.Self),
        ).then(
            ForEachPlayerEffect(Player.Each, listOf(DealDamageEffect(1, EffectTarget.Controller)))
        )
        restrictions = listOf(ActivationRestriction.AnyPlayerMay)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "50"
        artist = "Jesper Myrfors"
        imageUri = "https://cards.scryfall.io/normal/front/c/0/c0b10fb7-8667-42bf-aeb6-35767a82917b.jpg?1562930986"
    }
}
