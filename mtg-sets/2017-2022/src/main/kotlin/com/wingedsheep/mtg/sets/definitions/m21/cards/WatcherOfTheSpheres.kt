package com.wingedsheep.mtg.sets.definitions.m21.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Watcher of the Spheres — Core Set 2021 #227 (canonical printing)
 * {W}{U} · Creature — Bird Wizard · 2/2
 *
 * Flying
 * Creature spells with flying you cast cost {1} less to cast.
 * Whenever another creature you control with flying enters, this creature gets +1/+1 until end
 * of turn.
 *
 * The cost filter reads the spell's printed keywords, so a creature spell that only gains flying
 * once on the battlefield isn't reduced (ruling); the reduction only touches generic mana.
 */
val WatcherOfTheSpheres = card("Watcher of the Spheres") {
    manaCost = "{W}{U}"
    colorIdentity = "WU"
    typeLine = "Creature — Bird Wizard"
    power = 2
    toughness = 2
    oracleText = "Flying\n" +
        "Creature spells with flying you cast cost {1} less to cast.\n" +
        "Whenever another creature you control with flying enters, this creature gets +1/+1 " +
        "until end of turn."

    keywords(Keyword.FLYING)

    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.YouCast(GameObjectFilter.Creature.withKeyword(Keyword.FLYING)),
            modification = CostModification.ReduceGeneric(1),
        )
    }

    triggeredAbility {
        trigger = Triggers.another(GameObjectFilter.Creature.youControl().withKeyword(Keyword.FLYING)).enters()
        effect = Effects.ModifyStats(1, 1, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "227"
        artist = "G-host Lee"
        imageUri = "https://cards.scryfall.io/normal/front/3/5/354b9ea4-3d55-48cc-b426-c46b1d1bb397.jpg?1783930659"
        ruling("2020-06-23", "The cost reduction applies only to generic mana in the cost of creature spells with flying you cast. For example, if you cast a second Watcher of the Spheres, its cost won't be reduced below {W}{U}.")
        ruling("2020-06-23", "A creature spell that doesn't have flying won't cost less even if an effect will cause the creature to have flying once on the battlefield. This is also true if the creature spell itself has an ability that gives it flying once on the battlefield under certain conditions, even if those conditions are true.")
    }
}
