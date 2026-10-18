package com.wingedsheep.mtg.sets.definitions.leg.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantBeTargetedBy
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.UntargetableBy
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.predicates.StatePredicate

/**
 * Tetsuo Umezawa — Legends #262
 * {U}{B}{R} · Legendary Creature — Human Archer · 3/3
 *
 * Tetsuo Umezawa can't be the target of Aura spells.
 * {U}{B}{B}{R}, {T}: Destroy target tapped or blocking creature.
 *
 * [CantBeTargetedBy] with [UntargetableBy.SpellsWithSubtype] — only Aura *spells*: an Aura's
 * abilities may still target him, and an Aura put onto the battlefield without being cast may still
 * be attached.
 */
val TetsuoUmezawa = card("Tetsuo Umezawa") {
    manaCost = "{U}{B}{R}"
    colorIdentity = "UBR"
    typeLine = "Legendary Creature — Human Archer"
    power = 3
    toughness = 3
    oracleText = "Tetsuo Umezawa can't be the target of Aura spells.\n" +
        "{U}{B}{B}{R}, {T}: Destroy target tapped or blocking creature."

    staticAbility {
        ability = CantBeTargetedBy(UntargetableBy.SpellsWithSubtype(Subtype.AURA))
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{U}{B}{B}{R}"), Costs.Tap)
        val creature = target(
            TargetFilter(
                GameObjectFilter.Creature.withStatePredicate(
                    StatePredicate.Or(listOf(StatePredicate.IsTapped, StatePredicate.IsBlocking))
                )
            )
        )
        effect = Effects.Destroy(creature)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "262"
        artist = "Julie Baroh"
        imageUri = "https://cards.scryfall.io/normal/front/8/3/8384f87b-26c2-45b7-98ef-352c384f205e.jpg?1783948032"
    }
}
