package com.wingedsheep.mtg.sets.definitions.ltr.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.predicates.CardPredicate
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Mithril Coat
 * {3}
 * Legendary Artifact — Equipment
 *
 * Flash
 * Indestructible
 * When Mithril Coat enters, attach it to target legendary creature you control.
 * Equipped creature has indestructible.
 * Equip {3}
 */
val MithrilCoat = card("Mithril Coat") {
    manaCost = "{3}"
    typeLine = "Legendary Artifact — Equipment"
    oracleText = "Flash\nIndestructible\nWhen Mithril Coat enters, attach it to target legendary creature you control.\nEquipped creature has indestructible.\nEquip {3}"

    keywords(Keyword.FLASH, Keyword.INDESTRUCTIBLE)

    // When Mithril Coat enters, attach it to target legendary creature you control.
    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        val legendary = target(
            "target legendary creature you control",
            TargetCreature(
                filter = TargetFilter(
                    GameObjectFilter(
                        cardPredicates = listOf(CardPredicate.IsCreature, CardPredicate.IsLegendary)
                    ).youControl()
                )
            )
        )
        effect = Effects.AttachEquipment(legendary)
    }

    // Equipped creature has indestructible.
    staticAbility {
        ability = GrantKeyword(Keyword.INDESTRUCTIBLE, Filters.EquippedCreature)
    }

    equipAbility("{3}")

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "245"
        artist = "Igor Krstic"
        imageUri = "https://cards.scryfall.io/normal/front/0/f/0fd1fc09-a09d-45e6-8a07-3a8a83b4e6ec.jpg?1686970224"
    }
}
