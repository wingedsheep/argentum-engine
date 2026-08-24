package com.wingedsheep.mtg.sets.definitions.drk.cards

import com.wingedsheep.sdk.core.CardType
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.predicates.StatePredicate
import com.wingedsheep.sdk.scripting.targets.TargetPermanent

/**
 * Savaen Elves
 * {G}
 * Creature — Elf
 * 1/1
 * {G}{G}, {T}: Destroy target Aura attached to a land.
 *
 * The target is the *Aura*, not the land it enchants — filtered as an Aura enchantment whose
 * [StatePredicate.AttachedToCardType] host is a land, the same shape Arabian Nights' Pyramids uses
 * for the identical clause.
 */
val SavaenElves = card("Savaen Elves") {
    manaCost = "{G}"
    colorIdentity = "G"
    typeLine = "Creature — Elf"
    power = 1
    toughness = 1
    oracleText = "{G}{G}, {T}: Destroy target Aura attached to a land."

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{G}{G}"), Costs.Tap)
        val aura = target(
            "target",
            TargetPermanent(
                filter = TargetFilter(
                    GameObjectFilter.Enchantment.withSubtype("Aura").copy(
                        statePredicates = listOf(StatePredicate.AttachedToCardType(CardType.LAND))
                    )
                )
            )
        )
        effect = Effects.Destroy(aura)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "84"
        artist = "Ron Spencer"
        flavorText = "\"Purity of magic can only come from purity of the land. How can a meal " +
            "nourish if the ingredients are spoiled?\" —Sidaine of Savaen"
        imageUri = "https://cards.scryfall.io/normal/front/3/8/38fb3014-f631-4a75-92cd-7e626b13a4c3.jpg?1783947930"
    }
}
