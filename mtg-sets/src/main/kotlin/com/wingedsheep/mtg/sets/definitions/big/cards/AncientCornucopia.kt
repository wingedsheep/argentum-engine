package com.wingedsheep.mtg.sets.definitions.big.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.MayEffect
import com.wingedsheep.sdk.scripting.predicates.CardPredicate
import com.wingedsheep.sdk.scripting.values.EntityReference

/**
 * Ancient Cornucopia
 * {2}{G}
 * Artifact
 * Whenever you cast a spell that's one or more colors, you may gain 1 life for each of that
 * spell's colors. Do this only once each turn.
 * {T}: Add one mana of any color.
 */
val AncientCornucopia = card("Ancient Cornucopia") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Artifact"
    oracleText = "Whenever you cast a spell that's one or more colors, you may gain 1 life for each of that spell's colors. Do this only once each turn.\n{T}: Add one mana of any color."

    // Whenever you cast a colored spell, you may gain 1 life per color. Once each turn.
    triggeredAbility {
        trigger = Triggers.youCastSpell(
            spellFilter = GameObjectFilter(cardPredicates = listOf(CardPredicate.IsColored))
        )
        // "Do this only once each turn." — CR 603.2h, keyed to the *action*, not the trigger:
        // "Once you choose to gain life using Ancient Cornucopia's triggered ability, that
        // ability won't trigger again that turn" (Scryfall ruling). Declining leaves the turn's
        // use unspent, so a later colored spell still offers it.
        effectOncePerTurn = true
        // "you may gain 1 life for each of that spell's colors" — resolution-time yes/no.
        effect = MayEffect(Effects.GainLife(DynamicAmounts.colorCountOf(EntityReference.Triggering)))
    }

    // {T}: Add one mana of any color.
    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddAnyColorMana()
        manaAbility = true
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "16"
        artist = "Bartek Fedyczak"
        flavorText = "\"I reckon that thing's trying to put me out of a job!\"\n—Bristly Bill, cactusfolk gardener"
        imageUri = "https://cards.scryfall.io/normal/front/f/9/f977975d-0439-4731-b129-270cc4cdbb23.jpg?1739804204"
    }
}
