package com.wingedsheep.mtg.sets.definitions.one.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Rhuk, Hexgold Nabber
 * {2}{R}
 * Legendary Creature — Goblin Rebel
 * 2/2
 * Trample, haste
 * Whenever an equipped creature you control other than Rhuk attacks or dies, you may attach all
 * Equipment attached to that creature to Rhuk.
 *
 * "Attacks or dies" is two triggers sharing one effect. On the dies leg the creature and its
 * attachment links are gone by resolution, so [CardSource.AttachedTo] on
 * [EffectTarget.TriggeringEntity] reads the Equipment frozen on the trigger's zone change (CR
 * 608.2h). One "may" covers the whole group — all of that creature's Equipment or none of it.
 */
private val equippedCreatureYouControl = GameObjectFilter.Creature.youControl().equipped()

private val attachAllItsEquipmentToRhuk = Effects.May(
    Effects.Pipeline {
        val equipment = gather(
            CardSource.AttachedTo(
                host = EffectTarget.TriggeringEntity,
                filter = GameObjectFilter.Artifact.withSubtype(Subtype.EQUIPMENT)
            )
        )
        run(
            Effects.ForEachInCollection(
                equipment,
                Effects.AttachTargetEquipmentToCreature(
                    equipmentTarget = EffectTarget.IterationEntity,
                    creatureTarget = EffectTarget.Self
                )
            )
        )
    },
    prompt = "Attach all Equipment attached to that creature to Rhuk?"
)

val RhukHexgoldNabber = card("Rhuk, Hexgold Nabber") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Legendary Creature — Goblin Rebel"
    power = 2
    toughness = 2
    oracleText = "Trample, haste\n" +
        "Whenever an equipped creature you control other than Rhuk attacks or dies, you may attach " +
        "all Equipment attached to that creature to Rhuk."

    keywords(Keyword.TRAMPLE, Keyword.HASTE)

    triggeredAbility {
        trigger = Triggers.another(equippedCreatureYouControl).attacks()
        effect = attachAllItsEquipmentToRhuk
        description = "Whenever an equipped creature you control other than Rhuk attacks, you may " +
            "attach all Equipment attached to that creature to Rhuk."
    }

    triggeredAbility {
        trigger = Triggers.another(equippedCreatureYouControl).dies()
        effect = attachAllItsEquipmentToRhuk
        description = "Whenever an equipped creature you control other than Rhuk dies, you may " +
            "attach all Equipment attached to that creature to Rhuk."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "407"
        artist = "Andrea De Dominicis"
        flavorText = "Never get between a goblin and the closest shiny object."
        imageUri = "https://cards.scryfall.io/normal/front/d/7/d7c25806-1da6-4789-a17e-d2440184ec40.jpg?1783917920"
        ruling(
            "2023-02-04",
            "If that creature has more than one Equipment attached to it, you may move all of those Equipment to Rhuk or none of those Equipment to Rhuk. You can't move some of them and leave others."
        )
    }
}
