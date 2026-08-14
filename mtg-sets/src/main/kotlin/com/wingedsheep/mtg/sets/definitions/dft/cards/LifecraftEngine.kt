package com.wingedsheep.mtg.sets.definitions.dft.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ChoiceType
import com.wingedsheep.sdk.scripting.EntersWithChoice
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantChosenSubtype
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Lifecraft Engine
 * {3}
 * Artifact — Vehicle
 * 4/4
 *
 * As this Vehicle enters, choose a creature type.
 * Vehicle creatures you control are the chosen creature type in addition to their other types.
 * Each creature you control of the chosen type other than this Vehicle gets +1/+1.
 * Crew 3
 *
 * The same shape as Adaptive Automaton, one step wider: [EntersWithChoice] captures the creature
 * type durably, [GrantChosenSubtype] adds it (Layer 4) and the lord [ModifyStats] pays it off
 * (Layer 7c). The two differences from Automaton are what make the card interesting:
 *
 *  - The grant's filter is **Vehicle creatures you control**, not the source. A Vehicle is only a
 *    creature while something animates it (crew, or an effect like Chandra's), so the affected set
 *    is exactly the currently-animated Vehicles — including this one once it's crewed, which the
 *    oracle wording deliberately includes.
 *  - That makes the grant *depend* on the crew animation under CR 613.8a: applying the animation
 *    changes which permanents the grant applies to, so the animation is applied first no matter
 *    which effect has the earlier timestamp. `LifecraftEngineScenarioTest` pins that ordering.
 *
 * The lord reads `excludeSelf` for "other than this Vehicle", so a crewed Lifecraft Engine is the
 * chosen type (from its own first ability) but never buffs itself.
 */
val LifecraftEngine = card("Lifecraft Engine") {
    manaCost = "{3}"
    colorIdentity = ""
    typeLine = "Artifact — Vehicle"
    power = 4
    toughness = 4
    oracleText = "As this Vehicle enters, choose a creature type.\n" +
        "Vehicle creatures you control are the chosen creature type in addition to their other types.\n" +
        "Each creature you control of the chosen type other than this Vehicle gets +1/+1.\n" +
        "Crew 3"

    // As this Vehicle enters, choose a creature type.
    replacementEffect(EntersWithChoice(ChoiceType.CREATURE_TYPE))

    // Vehicle creatures you control are the chosen creature type in addition to their other types.
    staticAbility {
        ability = GrantChosenSubtype(
            filter = GroupFilter(GameObjectFilter.Creature.withSubtype(Subtype.VEHICLE)).youControl()
        )
    }

    // Each creature you control of the chosen type other than this Vehicle gets +1/+1.
    staticAbility {
        ability = ModifyStats(
            powerBonus = 1,
            toughnessBonus = 1,
            filter = GroupFilter.ChosenSubtypeCreatures(excludeSelf = true).youControl()
        )
    }

    keywordAbility(KeywordAbility.crew(3))

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "234"
        artist = "Mirko Failoni"
        imageUri = "https://cards.scryfall.io/normal/front/4/0/40c92203-17df-4f10-92c6-ebcc79f01357.jpg?1783907849"
    }
}
