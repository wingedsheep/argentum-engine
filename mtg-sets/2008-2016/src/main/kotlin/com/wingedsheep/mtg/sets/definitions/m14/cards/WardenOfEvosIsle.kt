package com.wingedsheep.mtg.sets.definitions.m14.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget

/**
 * Warden of Evos Isle — Magic 2014 #79 (canonical printing)
 * {2}{U} · Creature — Bird Wizard · 2/2
 *
 * Flying
 * Creature spells with flying you cast cost {1} less to cast.
 *
 * The cost filter reads the spell's printed keywords, so a creature spell that only gains flying
 * once on the battlefield isn't reduced (ruling); the reduction only touches generic mana.
 */
val WardenOfEvosIsle = card("Warden of Evos Isle") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Bird Wizard"
    power = 2
    toughness = 2
    oracleText = "Flying\n" +
        "Creature spells with flying you cast cost {1} less to cast."

    keywords(Keyword.FLYING)

    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.YouCast(GameObjectFilter.Creature.withKeyword(Keyword.FLYING)),
            modification = CostModification.ReduceGeneric(1),
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "79"
        artist = "Nils Hamm"
        flavorText = "On Evos Isle, the swift and formidable aven enforce the will of the ruling sphinxes."
        imageUri = "https://cards.scryfall.io/normal/front/2/3/2316d281-21a4-460d-9062-f0737249484e.jpg?1783939929"
        ruling("2020-11-10", "A creature spell that doesn't have flying won't cost less even if an effect will cause the creature to have flying once on the battlefield. This is also true if the creature spell itself has an ability that gives it flying once on the battlefield under certain conditions, even if those conditions are true.")
        ruling("2020-11-10", "The last ability of Warden of Evos Isle can't reduce the colored mana requirement of a creature spell with flying.")
    }
}
