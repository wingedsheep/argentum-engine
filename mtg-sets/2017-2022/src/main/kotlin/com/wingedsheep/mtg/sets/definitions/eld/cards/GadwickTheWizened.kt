package com.wingedsheep.mtg.sets.definitions.eld.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Gadwick, the Wizened — Throne of Eldraine #48
 * {X}{U}{U}{U} · Legendary Creature — Human Wizard · 3/3
 *
 * When Gadwick enters, draw X cards.
 * Whenever you cast a blue spell, tap target nonland permanent an opponent controls.
 *
 * The ETB reads [DynamicAmounts.castX] — the X paid to cast Gadwick, carried onto the battlefield
 * by the durable cast choices — so a Gadwick put onto the battlefield without being cast draws 0.
 */
val GadwickTheWizened = card("Gadwick, the Wizened") {
    manaCost = "{X}{U}{U}{U}"
    colorIdentity = "U"
    typeLine = "Legendary Creature — Human Wizard"
    power = 3
    toughness = 3
    oracleText = "When Gadwick enters, draw X cards.\n" +
        "Whenever you cast a blue spell, tap target nonland permanent an opponent controls."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.DrawCards(DynamicAmounts.castX())
        description = "When Gadwick enters, draw X cards."
    }

    triggeredAbility {
        trigger = Triggers.you.casts(GameObjectFilter.Any.withColor(Color.BLUE))
        val permanent = target(TargetFilter.NonlandPermanentOpponentControls)
        effect = Effects.Tap(permanent)
        description = "Whenever you cast a blue spell, tap target nonland permanent an opponent controls."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "48"
        artist = "Colin Boyer"
        flavorText = "His name is spoken only in whispers—but he hears just the same."
        imageUri = "https://cards.scryfall.io/normal/front/6/2/62ddce0d-f22a-4fcd-9a4a-d71938750ba1.jpg?1783932660"
        ruling("2019-10-04", "If Gadwick enters the battlefield without being cast, or if it was cast for a cost other than its mana cost, the value of X for its first ability is 0.")
        ruling("2019-10-04", "Gadwick's second ability resolves before the spell that caused it to trigger. It resolves even if that spell is countered.")
    }
}
