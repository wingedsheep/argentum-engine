package com.wingedsheep.mtg.sets.definitions.mh3.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Scurrilous Sentry — {3}{B} Creature — Human Knight Rogue 2/3.
 *
 * "Enters or attacks" is two trigger conditions on one printed line; the SDK models a trigger as a
 * single event pattern, so it is authored as two triggered abilities sharing one effect.
 */
val ScurrilousSentry = card("Scurrilous Sentry") {
    manaCost = "{3}{B}"
    typeLine = "Creature — Human Knight Rogue"
    power = 2
    toughness = 3
    oracleText = "Menace\nWhenever this creature enters or attacks, it connives. (Draw a card, then discard a card. If you discarded a nonland card, put a +1/+1 counter on this creature.)"

    keywords(Keyword.MENACE)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.Connive()
    }

    triggeredAbility {
        trigger = Triggers.self.attacks()
        effect = Effects.Connive()
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "108"
        artist = "Leonardo Santanna"
        flavorText = "Queen Marchesa's agents always manage to find the exact evidence they're looking for."
        imageUri = "https://cards.scryfall.io/normal/front/2/9/29e2805f-59fa-4a6d-97bc-266191b2aa8d.jpg?1783911276"
        ruling("2024-06-07", "If no card is discarded, most likely because that player's hand is empty and an effect says they can't draw cards, the conniving creature does not receive a +1/+1 counter.")
        ruling("2024-06-07", "If a resolving spell or ability instructs a specific creature to connive but that creature has left the battlefield, the creature still connives. If you discard a nonland card this way, you won't put a +1/+1 counter on anything. Abilities that trigger \"when [that creature] connives\" will trigger.")
    }
}
