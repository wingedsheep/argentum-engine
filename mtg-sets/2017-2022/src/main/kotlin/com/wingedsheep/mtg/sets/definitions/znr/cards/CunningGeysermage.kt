package com.wingedsheep.mtg.sets.definitions.znr.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.conditions.WasKicked
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Cunning Geysermage — Zendikar Rising #55
 * {2}{U} · Creature — Human Wizard · 3/2
 *
 * Kicker {2}{U}
 * When this creature enters, if it was kicked, return up to one other target creature to its
 * owner's hand.
 *
 * "If it was kicked" is an intervening-if ([WasKicked]): an unkicked Geysermage never puts the
 * trigger on the stack. "Up to one other" is an optional target that excludes the Geysermage.
 */
val CunningGeysermage = card("Cunning Geysermage") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Human Wizard"
    oracleText = "Kicker {2}{U} (You may pay an additional {2}{U} as you cast this spell.)\n" +
        "When this creature enters, if it was kicked, return up to one other target creature to its owner's hand."
    power = 3
    toughness = 2

    keywordAbility(KeywordAbility.kicker("{2}{U}"))

    triggeredAbility {
        trigger = Triggers.self.enters()
        interveningIf = WasKicked
        val victim = target(TargetFilter.OtherCreature, optional = true)
        effect = Effects.ReturnToHand(victim)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "55"
        artist = "Chris Rallis"
        flavorText = "\"Come on, Skitch, we'll lose them in the geyserfield.\""
        imageUri = "https://cards.scryfall.io/normal/front/2/2/2217f0a9-5af5-45cc-8cf7-8878d4f66dd8.jpg"
    }
}
