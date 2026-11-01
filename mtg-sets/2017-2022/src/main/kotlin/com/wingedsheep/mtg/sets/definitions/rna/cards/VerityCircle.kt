package com.wingedsheep.mtg.sets.definitions.rna.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TapReason
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Verity Circle — Ravnica Allegiance #58
 * {2}{U} · Enchantment
 *
 * "If it isn't being declared as an attacker" names the tap's own cause, so it is the
 * `exceptReason = TapReason.ATTACK` axis on the tap trigger: a creature tapped by any other cause
 * (a tap effect, a `{T}` cost, crew, convoke, mana) still triggers it, and per the rulings a
 * vigilant attacker tapped later in combat does too. "You may draw" is an optional trigger.
 */
val VerityCircle = card("Verity Circle") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Enchantment"
    oracleText = "Whenever a creature an opponent controls becomes tapped, if it isn't being declared " +
        "as an attacker, you may draw a card.\n" +
        "{4}{U}: Tap target creature without flying."

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Creature.opponentControls())
            .becomesTapped(exceptReason = TapReason.ATTACK)
        optional = true
        effect = Effects.DrawCards(1)
    }

    activatedAbility {
        cost = Costs.Mana("{4}{U}")
        val t = target(TargetFilter.Creature.withoutKeyword(Keyword.FLYING))
        effect = Effects.Tap(t)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "58"
        artist = "Volkan Baǵa"
        flavorText = "\"Here, there is only truth.\"\n—Barvisa, Azorius emissary"
        imageUri = "https://cards.scryfall.io/normal/front/3/d/3d866d26-b630-46d3-bcc2-b810c844cc89.jpg?1783933701"
    }
}
