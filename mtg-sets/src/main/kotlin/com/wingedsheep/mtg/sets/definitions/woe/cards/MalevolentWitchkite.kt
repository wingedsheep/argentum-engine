package com.wingedsheep.mtg.sets.definitions.woe.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.SacrificeEffect
import com.wingedsheep.sdk.scripting.predicates.CardPredicate

/**
 * Malevolent Witchkite
 * {4}{B}{B}
 * Creature — Dragon Warlock
 * 5/4
 *
 * Flying
 * When this creature enters, sacrifice any number of artifacts, enchantments, and/or tokens, then
 * draw that many cards.
 *
 * `SacrificeEffect(any = true)` supplies the zero-to-all battlefield selection and publishes the
 * number actually sacrificed to the following dynamic draw. The union deliberately includes every
 * token, including token creatures that are neither artifacts nor enchantments.
 */
val MalevolentWitchkite = card("Malevolent Witchkite") {
    manaCost = "{4}{B}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Dragon Warlock"
    oracleText = "Flying\nWhen this creature enters, sacrifice any number of artifacts, enchantments, " +
        "and/or tokens, then draw that many cards."
    power = 5
    toughness = 4

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = SacrificeEffect(
            filter = GameObjectFilter.Artifact.or(GameObjectFilter.Enchantment).or(
                GameObjectFilter.Any.withCardPredicate(CardPredicate.IsToken)
            ),
            any = true,
        ).then(Effects.DrawCards(DynamicAmounts.permanentsSacrificedThisWay()))
        description = "When this creature enters, sacrifice any number of artifacts, enchantments, " +
            "and/or tokens, then draw that many cards."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "315"
        artist = "Donato Giancola"
        flavorText = "\"Was there a fearsome witch around here? Or was it a cruel dragon? The tales " +
            "were confu—oh no.\"\n—Osval, adventurer"
        imageUri = "https://cards.scryfall.io/normal/front/d/6/d6cb3c6d-560d-40ca-b5c0-27322863cead.jpg?1783915040"

        ruling("2023-09-01", "As Malevolent Witchkite's triggered ability resolves, you may choose to sacrifice zero permanents.")
        ruling(
            "2023-09-01",
            "If any abilities trigger when you sacrifice permanents, those abilities won't be put onto " +
                "the stack until after you've drawn cards. Those abilities won't trigger if you choose " +
                "to sacrifice zero permanents."
        )
    }
}
