package com.wingedsheep.mtg.sets.definitions.mom.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.SelectionMode
import com.wingedsheep.sdk.scripting.effects.ZonePlacement

/**
 * Knight-Errant of Eos
 * {4}{W}
 * Creature — Human Knight
 * 4/4
 * Convoke
 * When this creature enters, look at the top six cards of your library. You may reveal up to two
 * creature cards with mana value X or less from among them, where X is the number of creatures that
 * convoked this creature. Put the revealed cards into your hand, then shuffle.
 *
 * X is the convoke record the spell carries onto the permanent (CR 702.51c), read by
 * [DynamicAmounts.convokedCreatureCount] — it counts creatures that have since left, per the
 * ruling. [Patterns.Library.lookAtTopAndTakeMatching] with an up-to-two selection, the kept cards
 * revealed as they go to hand, and the rest shuffled back into the library.
 */
val KnightErrantOfEos = card("Knight-Errant of Eos") {
    manaCost = "{4}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Knight"
    power = 4
    toughness = 4
    oracleText = "Convoke (Your creatures can help cast this spell. Each creature you tap while " +
        "casting this spell pays for {1} or one mana of that creature's color.)\n" +
        "When this creature enters, look at the top six cards of your library. You may reveal up to " +
        "two creature cards with mana value X or less from among them, where X is the number of " +
        "creatures that convoked this creature. Put the revealed cards into your hand, then shuffle."

    keywords(Keyword.CONVOKE)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Patterns.Library.lookAtTopAndTakeMatching(
            count = DynamicAmounts.fixed(6),
            filter = GameObjectFilter.Creature.manaValueAtMostDynamic(DynamicAmounts.convokedCreatureCount()),
            prompt = "You may reveal up to two creature cards with mana value X or less and put them into your hand",
            selection = SelectionMode.ChooseUpTo(DynamicAmounts.fixed(2)),
            keepRevealed = true,
            restDestination = CardDestination.ToZone(Zone.LIBRARY, placement = ZonePlacement.Shuffled),
        )
        description = "When this creature enters, look at the top six cards of your library. You may " +
            "reveal up to two creature cards with mana value X or less from among them, where X is the " +
            "number of creatures that convoked this creature. Put the revealed cards into your hand, then shuffle."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "26"
        artist = "Kevin Sidharta"
        imageUri = "https://cards.scryfall.io/normal/front/a/b/ab2ad652-2406-491a-9f22-23e974f943d7.jpg?1783917058"
        ruling(
            "2023-04-14",
            "You can't tap more creatures to convoke Knight-Errant of Eos than is necessary to pay for the spell. In most cases, this means five creatures. However, if there are any additional costs to cast Knight-Errant of Eos, you may use convoke to pay those additional costs as well."
        )
        ruling(
            "2023-04-14",
            "It doesn't matter if the creatures that convoked Knight-Errant of Eos are still on the battlefield or not as the enters-the-battlefield ability is resolving. You'll get credit for them when determining the value of X."
        )
    }
}
