package com.wingedsheep.mtg.sets.definitions.vow.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.TimingRule

/**
 * Heron-Blessed Geist
 * {4}{W}
 * Creature — Spirit
 * 3/3
 *
 * Flying
 * {3}{W}, Exile this card from your graveyard: Create two 1/1 white Spirit creature tokens with
 * flying. Activate only if you control an enchantment and only as a sorcery.
 *
 * A graveyard-activated ability (the Suspicious Shambler idiom): `activateFromZone = GRAVEYARD`
 * with a composite {3}{W} + [Costs.ExileSelf] cost, gated to sorcery speed
 * ([TimingRule.SorcerySpeed]) and to controlling an enchantment
 * ([ActivationRestriction.OnlyIfCondition] over [Conditions.ControlEnchantment]). Paying it
 * makes two 1/1 white flying Spirit tokens.
 */
val HeronBlessedGeist = card("Heron-Blessed Geist") {
    manaCost = "{4}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Spirit"
    power = 3
    toughness = 3
    oracleText = "Flying\n" +
        "{3}{W}, Exile this card from your graveyard: Create two 1/1 white Spirit creature tokens " +
        "with flying. Activate only if you control an enchantment and only as a sorcery."

    keywords(Keyword.FLYING)

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{3}{W}"), Costs.ExileSelf)
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = setOf(Color.WHITE),
            creatureTypes = setOf("Spirit"),
            keywords = setOf(Keyword.FLYING),
            count = 2
        )
        timing = TimingRule.SorcerySpeed
        activateFromZone = Zone.GRAVEYARD
        restrictions = listOf(ActivationRestriction.OnlyIfCondition(Conditions.ControlEnchantment))
        description = "{3}{W}, Exile this card from your graveyard: Create two 1/1 white Spirit " +
            "creature tokens with flying. Activate only if you control an enchantment and only as " +
            "a sorcery."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "19"
        artist = "Jason A. Engle"
        imageUri = "https://cards.scryfall.io/normal/front/8/3/83bacf4a-4e99-4008-97e4-3a82dddd4e45.jpg?1782703183"
    }
}
