package com.wingedsheep.mtg.sets.definitions.afr.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Wizard's Spellbook — Adventures in the Forgotten Realms #82
 * {5}{U}{U} · Artifact — Book · Rare
 *
 * {T}: Exile target instant or sorcery card from a graveyard. Roll a d20. Activate only as a sorcery.
 * 1—9 | Copy that card. You may cast the copy.
 * 10—19 | Copy that card. You may cast the copy by paying {1} rather than paying its mana cost.
 * 20 | Copy each card exiled with this artifact. You may cast any number of the copies without
 * paying their mana costs.
 *
 * - The exile is linked to this artifact (`linkToSource = true`, CR 607), so the 20 row's "each
 *   card exiled with this artifact" reads [CardSource.FromLinkedExile] — the pile includes the card
 *   just exiled, and each Spellbook tracks its own pile (ruling).
 * - The copies are made in exile and cast during resolution (CR 707.12), the same
 *   copy → cast-from-collection chain Kaervek, the Punisher (1—9: pay its costs), Jadzi, Oracle of
 *   Arcavios (10—19: a fixed {1} alternative mana cost) and Arcane Bombardment (20: any number, free)
 *   use. Uncast copies cease to exist (CR 707.10a).
 */
val WizardsSpellbook = card("Wizard's Spellbook") {
    manaCost = "{5}{U}{U}"
    colorIdentity = "U"
    typeLine = "Artifact — Book"
    oracleText = "{T}: Exile target instant or sorcery card from a graveyard. Roll a d20. Activate only as a sorcery.\n" +
        "1—9 | Copy that card. You may cast the copy.\n" +
        "10—19 | Copy that card. You may cast the copy by paying {1} rather than paying its mana cost.\n" +
        "20 | Copy each card exiled with this artifact. You may cast any number of the copies without paying their mana costs."

    activatedAbility {
        cost = Costs.Tap
        timing = TimingRule.SorcerySpeed
        targets(TargetFilter(GameObjectFilter.InstantOrSorcery, zone = Zone.GRAVEYARD))
        description = "Exile target instant or sorcery card from a graveyard. Roll a d20. 1—9: copy that card; " +
            "you may cast the copy. 10—19: copy that card; you may cast the copy by paying {1} rather than " +
            "paying its mana cost. 20: copy each card exiled with this artifact; you may cast any number of " +
            "the copies without paying their mana costs."
        effect = Effects.Pipeline {
            val exiled = gather(CardSource.ChosenTargets)
            exile(exiled, linkToSource = true)
            val result = rollDiceResult(sides = 20)
            fun between(low: Int, high: Int) = Conditions.All(
                Conditions.CompareAmounts(result.amount, ComparisonOperator.GTE, low),
                Conditions.CompareAmounts(result.amount, ComparisonOperator.LTE, high),
            )
            ifThen(between(1, 9)) {
                val copy = copyCards(exiled)
                run(Effects.May(Effects.CastFromCollection(copy), descriptionOverride = "You may cast the copy."))
            }
            ifThen(between(10, 19)) {
                val copy = copyCards(exiled)
                run(
                    Effects.May(
                        Effects.CastFromCollectionByPaying(copy, "{1}"),
                        descriptionOverride = "You may cast the copy by paying {1} rather than paying its mana cost.",
                    )
                )
            }
            ifThen(Conditions.CompareAmounts(result.amount, ComparisonOperator.GTE, 20)) {
                val pile = gather(CardSource.FromLinkedExile())
                val copies = copyCards(pile)
                run(Effects.CastAnyNumberFromCollectionWithoutPayingCost(from = copies))
            }
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "82"
        artist = "Iris Compiet"
        imageUri = "https://cards.scryfall.io/normal/front/b/6/b6ed0a4c-0234-4341-8dcb-5eea6a9632e7.jpg?1783926504"
        ruling("2021-07-23", "If your result is 1–9, you must still pay all costs to cast the copy. You may pay any additional or alternative costs the spell has. If it has any mandatory additional costs, those must be paid.")
        ruling("2021-07-23", "If your result is 10–19, you may pay {1} instead of its mana cost. You cannot pay its regular mana cost or any alternative costs the copy has. You may pay any additional costs and must pay mandatory additional costs.")
        ruling("2021-07-23", "If your result is 20, you may cast the copies without paying their mana costs. You cannot pay their regular mana costs or any alternative costs the copies have. You may pay any additional costs and must pay mandatory additional costs. You may cast the copies in any order you choose.")
        ruling("2021-07-23", "In all cases, any copies you don't cast cease to exist after the ability is finished resolving.")
        ruling("2021-07-23", "If you control multiple copies of Wizard's Spellbook, each one tracks the cards it has exiled separately.")
    }
}
