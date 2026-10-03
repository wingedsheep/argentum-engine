package com.wingedsheep.mtg.sets.definitions.soi.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Erdwal Illuminator (Shadows over Innistrad #60)
 * {1}{U}
 * Creature — Spirit 1/3
 * Flying
 * Whenever you investigate for the first time each turn, investigate an additional time.
 *
 * The trigger watches the investigate *action* (CR 701.16a), not Clue creation, so a card that only
 * says "create a Clue token" doesn't fire it. The extra investigate is the turn's second, so it
 * can't retrigger the Illuminator.
 */
val ErdwalIlluminator = card("Erdwal Illuminator") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Spirit"
    power = 1
    toughness = 3
    oracleText = "Flying\n" +
        "Whenever you investigate for the first time each turn, investigate an additional time."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.you.investigates(firstTimeEachTurn = true)
        effect = Effects.Investigate()
        description = "Whenever you investigate for the first time each turn, investigate an additional time."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "60"
        artist = "Seb McKinnon"
        flavorText = "\"When all else fails, follow the geist's lantern.\"\n—Vallon, Thraben inspector"
        imageUri = "https://cards.scryfall.io/normal/front/6/3/63df99f9-4f85-4c76-8d76-9075c9ef2b86.jpg?1783937800"
    }
}
