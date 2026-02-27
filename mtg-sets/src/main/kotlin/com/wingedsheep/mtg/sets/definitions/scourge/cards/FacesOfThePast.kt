package com.wingedsheep.mtg.sets.definitions.scourge.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.sdk.scripting.effects.Mode
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.predicates.CardPredicate

/**
 * Faces of the Past
 * {2}{U}
 * Enchantment
 * Whenever a creature dies, tap all untapped creatures that share a creature type
 * with it or untap all tapped creatures that share a creature type with it.
 *
 * Rulings (2004-10-04):
 * - You choose whether to tap or untap during resolution.
 * - You either tap all of them or untap all of them. You can't tap some and untap others.
 */
val FacesOfThePast = card("Faces of the Past") {
    manaCost = "{2}{U}"
    typeLine = "Enchantment"
    oracleText = "Whenever a creature dies, tap all untapped creatures that share a creature type with it or untap all tapped creatures that share a creature type with it."

    val sharesTypeFilter = GroupFilter(
        GameObjectFilter(
            cardPredicates = listOf(
                CardPredicate.IsCreature,
                CardPredicate.SharesCreatureTypeWithTriggeringEntity
            )
        )
    )

    triggeredAbility {
        trigger = Triggers.AnyCreatureDies
        effect = ModalEffect.chooseOne(
            Mode.noTarget(
                Effects.TapAll(sharesTypeFilter),
                "Tap all creatures that share a creature type with it"
            ),
            Mode.noTarget(
                Effects.UntapGroup(sharesTypeFilter),
                "Untap all creatures that share a creature type with it"
            )
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "35"
        artist = "Wayne England"
        flavorText = "The ties that bind can also strangle."
        imageUri = "https://cards.scryfall.io/normal/front/0/f/0f6dc35b-eb26-498f-ae35-0e860871446e.jpg?1562525440"
    }
}
