package com.wingedsheep.mtg.sets.definitions.inv.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CreateTokenEffect
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.scripting.values.EntityNumericProperty
import com.wingedsheep.sdk.scripting.values.EntityReference

/**
 * Aura Mutation
 * {G}{W}
 * Instant
 * Destroy target enchantment. Create X 1/1 green Saproling creature tokens, where X is that
 * enchantment's mana value.
 *
 * The token count reads the destroyed enchantment's mana value via
 * [DynamicAmount.EntityProperty]; mana value is a card characteristic that survives the move to
 * the graveyard, so the count resolves correctly after [Effects.Destroy] (CR 608.2h — last-known
 * information). If the enchantment is an illegal target on resolution the whole spell is countered
 * by the rules and no Saprolings are created.
 */
val AuraMutation = card("Aura Mutation") {
    manaCost = "{G}{W}"
    colorIdentity = "GW"
    typeLine = "Instant"
    oracleText = "Destroy target enchantment. Create X 1/1 green Saproling creature tokens, " +
        "where X is that enchantment's mana value."

    spell {
        val t = target("target", Targets.Enchantment)
        effect = Effects.Destroy(t) then CreateTokenEffect(
            count = DynamicAmount.EntityProperty(
                entity = EntityReference.Target(0),
                numericProperty = EntityNumericProperty.ManaValue
            ),
            power = 1,
            toughness = 1,
            colors = setOf(Color.GREEN),
            creatureTypes = setOf("Saproling")
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "232"
        artist = "Pete Venters"
        flavorText = "\"Life can be found in all things, even things unnatural.\"\n—Multani, maro-sorcerer"
        imageUri = "https://cards.scryfall.io/normal/front/3/8/38421179-615e-4aba-91a4-503bfee05403.jpg?1562906340"
    }
}
