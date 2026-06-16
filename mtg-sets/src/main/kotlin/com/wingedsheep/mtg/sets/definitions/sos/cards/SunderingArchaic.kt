package com.wingedsheep.mtg.sets.definitions.sos.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject
import com.wingedsheep.sdk.scripting.targets.TargetPermanent
import com.wingedsheep.sdk.scripting.values.EntityReference

/**
 * Sundering Archaic
 * {6}
 * Creature — Avatar
 * 3/3
 *
 * Converge — When this creature enters, exile target nonland permanent an opponent controls with
 * mana value less than or equal to the number of colors of mana spent to cast this creature.
 * {2}: Put target card from a graveyard on the bottom of its owner's library.
 *
 * The colour-count gate on the exile target is the
 * [com.wingedsheep.sdk.scripting.predicates.CardPredicate.ManaValueAtMostColorsSpent] predicate
 * (`manaValueAtMostColorsSpent`), reading this creature's `CastRecordComponent` snapshot — sibling
 * of Astelli Reclaimer's total-mana-spent predicate, but comparing against the colour *count*.
 */
val SunderingArchaic = card("Sundering Archaic") {
    manaCost = "{6}"
    colorIdentity = ""
    typeLine = "Creature — Avatar"
    power = 3
    toughness = 3
    oracleText = "Converge — When this creature enters, exile target nonland permanent an opponent " +
        "controls with mana value less than or equal to the number of colors of mana spent to cast " +
        "this creature.\n" +
        "{2}: Put target card from a graveyard on the bottom of its owner's library."

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        val permanent = target(
            "nonland permanent an opponent controls with mana value less than or equal to the " +
                "number of colors of mana spent to cast this creature",
            TargetPermanent(
                filter = TargetFilter(
                    GameObjectFilter.NonlandPermanent
                        .opponentControls()
                        .manaValueAtMostColorsSpent(EntityReference.Source)
                )
            )
        )
        effect = Effects.Exile(permanent)
        description = "Converge — When this creature enters, exile target nonland permanent an " +
            "opponent controls with mana value less than or equal to the number of colors of mana " +
            "spent to cast this creature."
    }

    activatedAbility {
        cost = Costs.Mana("{2}")
        val graveyardCard = target(
            "target card from a graveyard",
            TargetObject(filter = TargetFilter(GameObjectFilter.Any, zone = Zone.GRAVEYARD))
        )
        effect = Effects.PutOnBottomOfLibrary(graveyardCard)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "3"
        artist = "Quintin Gleim"
        imageUri = "https://cards.scryfall.io/normal/front/c/3/c35b57e4-2358-46c0-8f09-cd27c10eaf2d.jpg?1775936933"
    }
}
