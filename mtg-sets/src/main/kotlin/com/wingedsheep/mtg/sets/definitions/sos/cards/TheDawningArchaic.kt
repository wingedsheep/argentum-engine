package com.wingedsheep.mtg.sets.definitions.sos.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.CostReductionSource
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * The Dawning Archaic — Secrets of Strixhaven #1
 * {10} · Legendary Creature — Avatar · 7/7
 *
 * This spell costs {1} less to cast for each instant and sorcery card in your graveyard.
 * Reach
 * Whenever The Dawning Archaic attacks, you may cast target instant or sorcery card from your
 * graveyard without paying its mana cost. If that spell would be put into your graveyard, exile
 * it instead.
 *
 * Cost reduction reuses [ModifySpellCost] + [CostReductionSource.CardsInGraveyardMatchingFilter]
 * (Eddymurk Crab). The attack trigger mirrors Daring Waverider: move the targeted card to exile,
 * then [Effects.GrantFreeCastTargetFromExile] with `exileAfterResolve = true` so the cast spell
 * is exiled rather than returning to the graveyard.
 */
val TheDawningArchaic = card("The Dawning Archaic") {
    manaCost = "{10}"
    typeLine = "Legendary Creature — Avatar"
    power = 7
    toughness = 7
    oracleText = "This spell costs {1} less to cast for each instant and sorcery card in your " +
        "graveyard.\nReach\nWhenever The Dawning Archaic attacks, you may cast target instant or " +
        "sorcery card from your graveyard without paying its mana cost. If that spell would be put " +
        "into your graveyard, exile it instead."

    keywords(Keyword.REACH)

    // Costs {1} less for each instant/sorcery card in your graveyard.
    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.SelfCast,
            modification = CostModification.ReduceGenericBy(
                CostReductionSource.CardsInGraveyardMatchingFilter(
                    filter = GameObjectFilter.InstantOrSorcery,
                ),
            ),
        )
    }

    // On attack: cast a target instant/sorcery from your graveyard for free, exiling it after.
    triggeredAbility {
        trigger = Triggers.Attacks
        target = TargetObject(filter = TargetFilter.InstantOrSorceryInGraveyard.ownedByYou())
        effect = Effects.Composite(
            Effects.Move(EffectTarget.ContextTarget(0), Zone.EXILE),
            Effects.GrantFreeCastTargetFromExile(
                target = EffectTarget.ContextTarget(0),
                exileAfterResolve = true,
            ),
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "1"
        artist = "Josu Solano"
        imageUri = "https://cards.scryfall.io/normal/front/7/a/7a451985-37e1-44d8-839b-dc1e88df5c96.jpg?1775936921"
    }
}
