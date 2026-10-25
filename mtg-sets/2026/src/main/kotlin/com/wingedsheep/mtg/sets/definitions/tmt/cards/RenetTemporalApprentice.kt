package com.wingedsheep.mtg.sets.definitions.tmt.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.predicates.StatePredicate
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Renet, Temporal Apprentice
 * {3}{U}{U}
 * Legendary Creature — Human Wizard
 * 4/3
 *
 * Flash
 * When Renet enters, return each other nonland permanent that entered
 * this turn to its owner's hand.
 */
val RenetTemporalApprentice = card("Renet, Temporal Apprentice") {
    manaCost = "{3}{U}{U}"
    colorIdentity = "U"
    typeLine = "Legendary Creature — Human Wizard"
    oracleText = "Flash\nWhen Renet enters, return each other nonland permanent that entered this turn to its owner's hand."
    power = 4
    toughness = 3

    keywords(Keyword.FLASH)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.ForEachInGroup(
            filter = GroupFilter(
                GameObjectFilter.NonlandPermanent.copy(
                    statePredicates = GameObjectFilter.NonlandPermanent.statePredicates +
                        StatePredicate.EnteredThisTurn()
                ),
                excludeSelf = true
            ),
            effect = Effects.Move(EffectTarget.IterationEntity, Zone.HAND)
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "50"
        artist = "Yuhong Ding"
        flavorText = "\"You guys don't get sick during time travel, do you?\""
        imageUri = "https://cards.scryfall.io/normal/front/2/a/2ae30766-c858-4f4e-a042-14af55698cb2.jpg?1769005718"
    }
}
