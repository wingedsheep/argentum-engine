package com.wingedsheep.mtg.sets.definitions.thb.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Tymaret, Chosen from Death
 * {B}{B}
 * Legendary Enchantment Creature — Demigod
 * 2/★
 * Tymaret's toughness is equal to your devotion to black.
 * {1}{B}: Exile up to two target cards from graveyards. You gain 1 life for each creature card
 * exiled this way.
 *
 * The toughness is Daxos's characteristic-defining [dynamicToughness] over
 * [DynamicAmounts.devotionTo] black. The activated ability is The Binding of the Titans' chapter II:
 * up to two graveyard targets, exiled through [moveTracked] so the life gain counts only the
 * creature cards that actually moved — a target that left its graveyard in response gains nothing.
 */
val TymaretChosenFromDeath = card("Tymaret, Chosen from Death") {
    manaCost = "{B}{B}"
    colorIdentity = "B"
    typeLine = "Legendary Enchantment Creature — Demigod"
    oracleText = "Tymaret's toughness is equal to your devotion to black. (Each {B} in the mana costs " +
        "of permanents you control counts toward your devotion to black.)\n" +
        "{1}{B}: Exile up to two target cards from graveyards. You gain 1 life for each creature card " +
        "exiled this way."

    power = 2
    dynamicToughness(DynamicAmounts.devotionTo(Color.BLACK))

    activatedAbility {
        cost = Costs.Mana("{1}{B}")
        targets(TargetFilter.CardInGraveyard, count = 2, optional = true)
        effect = Effects.Pipeline {
            val exiled = moveTracked(gather(CardSource.ChosenTargets), CardDestination.ToZone(Zone.EXILE))
            val creatures = filter(exiled, GameObjectFilter.Creature)
            run(Effects.GainLife(creatures.count))
        }
        description = "{1}{B}: Exile up to two target cards from graveyards. You gain 1 life for each " +
            "creature card exiled this way."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "119"
        artist = "Chase Stone"
        imageUri = "https://cards.scryfall.io/normal/front/1/4/14b28eae-e8ed-4b99-b6ec-86d0716ec473.jpg?1783931559"
        ruling("2020-01-24", "The ability that defines Tymaret's toughness works in all zones, not just the battlefield.")
        ruling(
            "2020-01-24",
            "Because damage remains marked on a creature until the damage is removed as the turn ends, " +
                "nonlethal damage dealt to Tymaret may become lethal if your devotion to black decreases " +
                "during that turn."
        )
    }
}
