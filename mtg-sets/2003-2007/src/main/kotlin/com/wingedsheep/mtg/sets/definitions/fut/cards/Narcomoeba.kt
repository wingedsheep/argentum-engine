package com.wingedsheep.mtg.sets.definitions.fut.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Narcomoeba — Future Sight #54
 * {1}{U} · Creature — Illusion · 1/1
 *
 * Flying
 * When this card is put into your graveyard from your library, you may put it onto the battlefield.
 *
 * The trigger functions only from the graveyard (Gaea's Blessing's shape). The move is gated on the
 * card still being in the graveyard on resolution (ruling).
 */
val Narcomoeba = card("Narcomoeba") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Illusion"
    oracleText = "Flying\nWhen this card is put into your graveyard from your library, you may put it onto the battlefield."
    power = 1
    toughness = 1
    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.changesZone(from = Zone.LIBRARY, to = Zone.GRAVEYARD)
        triggerZone = Zone.GRAVEYARD
        effect = Effects.May(
            Effects.Move(EffectTarget.Self, Zone.BATTLEFIELD, fromZone = Zone.GRAVEYARD),
            prompt = "Put Narcomoeba onto the battlefield?",
            sourceRequiredZone = Zone.GRAVEYARD,
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "54"
        artist = "Matt Stewart"
        flavorText = "It was created by the Iquati as a living memory—one that objects to being forgotten."
        imageUri = "https://cards.scryfall.io/normal/front/f/7/f76b3746-2e2c-4560-a2d2-e7b5b92833b2.jpg?1783943117"
        ruling("2024-01-12", "If Narcomoeba is revealed or looked at while moving from your library to your graveyard (such as with Mind Funeral), its ability will trigger when it's put into your graveyard. It was still in your library while it was revealed or being looked at.")
        ruling("2024-01-12", "If Narcomoeba is removed from your graveyard before its ability resolves, you can't put it onto the battlefield.")
    }
}
