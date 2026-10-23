package com.wingedsheep.mtg.sets.definitions.afr.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.GrantWard
import com.wingedsheep.sdk.scripting.effects.WardCost
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * The Tarrasque
 * {6}{G}{G}{G}
 * Legendary Creature — Dinosaur
 * 10/10
 * The Tarrasque has haste and ward {10} as long as it was cast.
 * Whenever The Tarrasque attacks, it fights target creature defending player controls.
 *
 * "As long as it was cast" is [Conditions.WasCast] gating two self-scoped grants: the condition
 * reads the cast marker the permanent carries from resolution, so it holds from any zone it was
 * cast from and is gone once The Tarrasque is put onto the battlefield some other way.
 */
val TheTarrasque = card("The Tarrasque") {
    manaCost = "{6}{G}{G}{G}"
    colorIdentity = "G"
    typeLine = "Legendary Creature — Dinosaur"
    power = 10
    toughness = 10
    oracleText = "The Tarrasque has haste and ward {10} as long as it was cast.\n" +
        "Whenever The Tarrasque attacks, it fights target creature defending player controls."

    staticAbility { condition = Conditions.WasCast; ability = GrantKeyword(Keyword.HASTE, Filters.Self) }
    staticAbility { condition = Conditions.WasCast; ability = GrantWard(WardCost.Mana("{10}"), Filters.Self) }

    triggeredAbility {
        trigger = Triggers.self.attacks()
        val creature = target(TargetFilter.Creature.defendingPlayerControls())
        effect = Effects.Fight(EffectTarget.Self, creature)
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "207"
        artist = "Filip Burburan"
        flavorText = "When it rises, nations fall."
        imageUri = "https://cards.scryfall.io/normal/front/8/a/8a26fa15-d81f-4152-ae33-e91aa276b3fc.jpg?1783926454"
        ruling("2021-07-23", "The Tarrasque will have haste and ward {10} even if it was cast from a zone other than your hand. May we suggest the command zone?")
        ruling("2021-07-23", "Fighting is not optional. If there is at least one legal target for The Tarrasque's last ability, it must fight.")
        ruling("2021-07-23", "The fight happens in the declare attackers step before blockers are declared. Unless the creature it is fighting survives, that creature won't be able to block that combat.")
        ruling("2021-07-23", "If a player casts a spell that targets multiple permanents their opponent controls with ward, each of those ward abilities will trigger. If that player doesn't pay for all of them, the spell will be countered.")
    }
}
