package com.wingedsheep.mtg.sets.definitions.blb.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.scripting.ActivatedAbility
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantActivatedAbility
import com.wingedsheep.sdk.scripting.LoseAllAbilities
import com.wingedsheep.sdk.scripting.TransformPermanent
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.predicates.CardPredicate
import com.wingedsheep.sdk.scripting.targets.TargetPermanent

/**
 * Sugar Coat
 * {2}{U}
 * Enchantment — Aura
 *
 * Flash
 * Enchant creature or Food
 * Enchanted permanent is a colorless Food artifact with "{2}, {T}, Sacrifice this
 * artifact: You gain 3 life" and loses all other card types and abilities.
 */
val SugarCoat = card("Sugar Coat") {
    manaCost = "{2}{U}"
    typeLine = "Enchantment — Aura"
    oracleText = "Flash\nEnchant creature or Food\nEnchanted permanent is a colorless Food artifact with \"{2}, {T}, Sacrifice this artifact: You gain 3 life\" and loses all other card types and abilities."

    keywords(Keyword.FLASH)

    // Enchant creature or Food
    auraTarget = TargetPermanent(
        filter = TargetFilter(
            GameObjectFilter(
                cardPredicates = listOf(
                    CardPredicate.Or(
                        listOf(
                            CardPredicate.IsCreature,
                            CardPredicate.HasSubtype(Subtype("Food"))
                        )
                    )
                )
            )
        )
    )

    // "is a colorless Food artifact" — Layer 4 (type) + Layer 5 (color)
    staticAbility {
        ability = TransformPermanent(
            setCardTypes = setOf("ARTIFACT"),
            setSubtypes = setOf("Food"),
            setColors = emptySet() // colorless
        )
    }

    // "loses all other card types and abilities" — Layer 6
    staticAbility {
        ability = LoseAllAbilities()
    }

    // "with '{2}, {T}, Sacrifice this artifact: You gain 3 life'" — Layer 6
    staticAbility {
        ability = GrantActivatedAbility(
            ability = ActivatedAbility(
                cost = Costs.Composite(
                    Costs.Mana("{2}"),
                    Costs.Tap,
                    Costs.SacrificeSelf
                ),
                effect = Effects.GainLife(3),
                descriptionOverride = "{2}, {T}, Sacrifice this artifact: You gain 3 life."
            )
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "76"
        artist = "Gaboleps"
        flavorText = "Some jokes have the punchline baked in."
        imageUri = "https://cards.scryfall.io/normal/front/f/c/fcacbe71-efb0-49e1-b2d0-3ee65ec6cf8b.jpg?1721426291"
    }
}
