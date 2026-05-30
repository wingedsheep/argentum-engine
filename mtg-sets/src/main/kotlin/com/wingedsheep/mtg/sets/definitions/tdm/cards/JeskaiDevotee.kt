package com.wingedsheep.mtg.sets.definitions.tdm.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.ManaColorSet

/**
 * Jeskai Devotee
 * {1}{R}
 * Creature — Orc Monk
 * 2/2
 *
 * Flurry — Whenever you cast your second spell each turn, this creature gets +1/+1
 * until end of turn.
 * {1}: Add {U}, {R}, or {W}. Activate only once each turn.
 */
val JeskaiDevotee = card("Jeskai Devotee") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Orc Monk"
    power = 2
    toughness = 2
    oracleText = "Flurry — Whenever you cast your second spell each turn, this creature gets +1/+1 until end of turn.\n{1}: Add {U}, {R}, or {W}. Activate only once each turn."

    flurry {
        effect = Effects.ModifyStats(1, 1, EffectTarget.Self)
    }

    activatedAbility {
        cost = Costs.Mana("{1}")
        manaAbility = true
        restrictions = listOf(ActivationRestriction.OncePerTurn)
        effect = Effects.AddManaOfChoice(
            ManaColorSet.Specific(setOf(Color.BLUE, Color.RED, Color.WHITE))
        )
        description = "{1}: Add {U}, {R}, or {W}. Activate only once each turn."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "110"
        artist = "Xavier Ribeiro"
        flavorText = "A master of the Whirling Blade technique, he did not hesitate to leap to the monastery's defense."
        imageUri = "https://cards.scryfall.io/normal/front/2/7/27f31f9c-7149-4608-9b18-b3530a2efd4a.jpg?1743204404"
    }
}
