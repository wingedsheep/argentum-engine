package com.wingedsheep.mtg.sets.definitions.thb.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.MayPlayExpiry

/**
 * Furious Rise — Theros Beyond Death #136 (canonical printing)
 * {2}{R} · Enchantment
 *
 * At the beginning of your end step, if you control a creature with power 4 or greater, exile the
 * top card of your library. You may play that card until you exile another card with this
 * enchantment.
 *
 * The power check is an intervening "if": it gates the trigger and is re-checked on resolution
 * (it needn't be the same creature both times). The impulse grant uses
 * [MayPlayExpiry.UntilSourceExilesAnother] (Unstable Amulet), so it survives Furious Rise leaving
 * the battlefield and is revoked only when this same enchantment exiles another card.
 */
val FuriousRise = card("Furious Rise") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Enchantment"
    oracleText = "At the beginning of your end step, if you control a creature with power 4 or greater, " +
        "exile the top card of your library. You may play that card until you exile another card with this enchantment."

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.END)
        interveningIf = Conditions.YouControl(GameObjectFilter.Creature.powerAtLeast(4))
        effect = Patterns.Exile.impulse(count = 1, expiry = MayPlayExpiry.UntilSourceExilesAnother)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "136"
        artist = "Chase Stone"
        flavorText = "Klothys set out to mend the destiny of Theros, her fury guiding every stitch."
        imageUri = "https://cards.scryfall.io/normal/front/2/5/2531fb76-038b-49ab-b4d6-31b291faa7ca.jpg?1783931552"
        ruling("2020-06-23", "If you don't control a creature with power 4 or greater as your end step begins, Furious Rise's ability doesn't trigger. If you don't control one as the ability resolves, it has no effect. It doesn't have to be the same creature at both times, however.")
        ruling("2020-06-23", "Furious Rise's effect doesn't change when you can play the exiled card. For example, if you exile a sorcery card, you can cast it only during your main phase when the stack is empty. If you exile a land card, you can't play it unless you have land plays available.")
        ruling("2020-06-23", "If Furious Rise leaves the battlefield before you play the most recently exiled card, you can play that card for as long as it remains exiled.")
        ruling("2020-06-23", "Furious Rise's ability has you exile only one card, no matter how many creatures with power 4 or greater you control beyond the first.")
    }
}
