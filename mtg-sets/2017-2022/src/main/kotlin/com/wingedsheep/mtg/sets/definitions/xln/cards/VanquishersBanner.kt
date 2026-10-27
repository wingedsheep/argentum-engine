package com.wingedsheep.mtg.sets.definitions.xln.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ChoiceType
import com.wingedsheep.sdk.scripting.EntersWithChoice
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Vanquisher's Banner — Ixalan #251
 * {5} · Artifact
 *
 * As this artifact enters, choose a creature type.
 * Creatures you control of the chosen type get +1/+1.
 * Whenever you cast a creature spell of the chosen type, draw a card.
 *
 * The Chronicle of Victory shape, narrowed to creature spells: the anthem reads the chosen type
 * through `chosenSubtypeKey`, the cast trigger through `withChosenSubtype()` on a creature filter.
 * The draw trigger resolves before the spell, even if the spell is countered.
 */
val VanquishersBanner = card("Vanquisher's Banner") {
    manaCost = "{5}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "As this artifact enters, choose a creature type.\n" +
        "Creatures you control of the chosen type get +1/+1.\n" +
        "Whenever you cast a creature spell of the chosen type, draw a card."

    replacementEffect(EntersWithChoice(ChoiceType.CREATURE_TYPE))

    staticAbility {
        ability = ModifyStats(
            powerBonus = 1,
            toughnessBonus = 1,
            filter = GroupFilter.ChosenSubtypeCreatures().youControl()
        )
    }

    triggeredAbility {
        trigger = Triggers.you.casts(GameObjectFilter.Creature.withChosenSubtype())
        effect = Effects.DrawCards(1)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "251"
        artist = "Milivoj Ćeran"
        imageUri = "https://cards.scryfall.io/normal/front/6/0/60b7a85f-3a30-4ece-9bbb-61f3c4b796b8.jpg?1783935699"
        ruling(
            "2017-09-29",
            "The choice of creature type is made as Vanquisher's Banner enters the battlefield. Players can't respond to this choice. The bonus starts applying immediately."
        )
        ruling(
            "2017-09-29",
            "The last ability of Vanquisher's Banner resolves before the spell that caused it to trigger. The ability will resolve even if the creature spell is countered."
        )
        ruling(
            "2021-03-19",
            "To choose a creature type, you must choose an existing creature type, such as Fungus or Sliver. You can't choose multiple creature types, such as Fungus Sliver. Card types such as artifact can't be chosen, nor can subtypes that aren't creature types, such as Jace, Vehicle, or Treasure."
        )
        ruling(
            "2021-03-19",
            "Because damage remains marked on a creature until the damage is removed as the turn ends, nonlethal damage dealt to a creature you control of the chosen type may become lethal if Vanquisher's Banner leaves the battlefield during that turn."
        )
    }
}
