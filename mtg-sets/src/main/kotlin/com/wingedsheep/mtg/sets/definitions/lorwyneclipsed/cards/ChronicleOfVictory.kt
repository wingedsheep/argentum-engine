package com.wingedsheep.mtg.sets.definitions.lorwyneclipsed.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.scripting.TriggerSpec
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ChoiceType
import com.wingedsheep.sdk.scripting.EntersWithChoice
import com.wingedsheep.sdk.scripting.GameEvent.SpellCastEvent
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeywordToCreatureGroup
import com.wingedsheep.sdk.scripting.ModifyStatsForCreatureGroup
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Chronicle of Victory
 * {6}
 * Legendary Artifact
 * As Chronicle of Victory enters, choose a creature type.
 * Creatures you control of the chosen type get +2/+2 and have first strike and trample.
 * Whenever you cast a spell of the chosen type, draw a card.
 */
val ChronicleOfVictory = card("Chronicle of Victory") {
    manaCost = "{6}"
    typeLine = "Legendary Artifact"
    oracleText = "As Chronicle of Victory enters, choose a creature type.\n" +
            "Creatures you control of the chosen type get +2/+2 and have first strike and trample.\n" +
            "Whenever you cast a spell of the chosen type, draw a card."

    replacementEffect(EntersWithChoice(ChoiceType.CREATURE_TYPE))

    val chosenTypeYouControl = GroupFilter(
        GameObjectFilter.Creature.youControl(),
        chosenSubtypeKey = "chosenCreatureType"
    )

    staticAbility {
        ability = ModifyStatsForCreatureGroup(
            powerBonus = 2,
            toughnessBonus = 2,
            filter = chosenTypeYouControl
        )
    }

    staticAbility {
        ability = GrantKeywordToCreatureGroup(Keyword.FIRST_STRIKE, chosenTypeYouControl)
    }

    staticAbility {
        ability = GrantKeywordToCreatureGroup(Keyword.TRAMPLE, chosenTypeYouControl)
    }

    triggeredAbility {
        trigger = TriggerSpec(
            event = SpellCastEvent(
                spellFilter = GameObjectFilter.Any.withChosenSubtype(),
                player = Player.You
            ),
            binding = TriggerBinding.ANY
        )
        effect = Effects.DrawCards(1)
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "253"
        artist = "Aldo Domínguez"
        flavorText = "\"For those we lost, that they may forever live.\"\n—Tapestry inscription"
        imageUri = "https://cards.scryfall.io/normal/front/b/3/b3c2d68d-690b-41e7-99ed-2d20c7e0a9b4.jpg?1767658597"
    }
}
