package com.wingedsheep.mtg.sets.definitions.znr.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Tazeem Roilmage — Zendikar Rising #84
 * {1}{U} · Creature — Merfolk Wizard · 2/1
 *
 * Kicker {4}
 * When this creature enters, if it was kicked, return target instant or sorcery card from your
 * graveyard to your hand.
 */
val TazeemRoilmage = card("Tazeem Roilmage") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Merfolk Wizard"
    oracleText = "Kicker {4} (You may pay an additional {4} as you cast this spell.)\n" +
        "When this creature enters, if it was kicked, return target instant or sorcery card from your graveyard to your hand."
    power = 2
    toughness = 1

    keywordAbility(KeywordAbility.kicker("{4}"))

    triggeredAbility {
        trigger = Triggers.self.enters()
        interveningIf = Conditions.WasKicked
        val t = target(TargetFilter.InstantOrSorceryInYourGraveyard)
        effect = Effects.ReturnToHand(t)
        description = "When this creature enters, if it was kicked, return target instant or sorcery card from your graveyard to your hand."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "84"
        artist = "Manuel Castañón"
        flavorText = "\"The Roil can be directed. Observe.\""
        imageUri = "https://cards.scryfall.io/normal/front/6/f/6f222fb7-375d-4ee2-9ff6-bd71f63ab31a.jpg"
    }
}
