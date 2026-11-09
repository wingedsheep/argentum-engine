package com.wingedsheep.mtg.sets.definitions.mkc.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.PreventExtraTurns
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Trouble in Pairs
 * {2}{W}{W}
 * Enchantment
 *
 * If an opponent would begin an extra turn, that player skips that turn instead.
 * Whenever an opponent attacks you with two or more creatures, draws their second card each turn,
 * or casts their second spell each turn, you draw a card.
 *
 * The first ability is [PreventExtraTurns] scoped to opponents: the extra turn is created normally
 * and skipped only as it would begin, so it happens if Trouble in Pairs has left by then.
 *
 * The second is one triggered ability over three events, each of which triggers it on its own — a
 * single turn can trigger it three times, and every attack declaration with two or more creatures
 * aimed at you (extra combats included) triggers it again. "Attacks you with two or more creatures"
 * counts only creatures attacking you, not a planeswalker you control.
 */
val TroubleInPairs = card("Trouble in Pairs") {
    manaCost = "{2}{W}{W}"
    colorIdentity = "W"
    typeLine = "Enchantment"
    oracleText = "If an opponent would begin an extra turn, that player skips that turn instead.\n" +
        "Whenever an opponent attacks you with two or more creatures, draws their second card each turn, " +
        "or casts their second spell each turn, you draw a card."

    replacementEffect(PreventExtraTurns(EventPattern.ExtraTurnEvent(Player.EachOpponent)))

    triggeredAbility {
        trigger = Triggers.or(
            Triggers.you.isAttacked(minAttackers = 2),
            Triggers.anOpponent.drawsNth(2),
            Triggers.anOpponent.castsNth(2),
        )
        effect = Effects.DrawCards(1)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "15"
        artist = "Fay Dalton"
        flavorText = "Together, they're greater than the sum of their rap sheets."
        imageUri = "https://cards.scryfall.io/normal/front/0/f/0f61e93f-5f97-4c7d-b3d5-0e05242faeb3.jpg?1783913050"
        ruling("2024-02-02", "An \"extra turn\" is any turn created by a spell or ability. Notably, it doesn't include additional turns taken in tournaments after time expires for a round.")
        ruling("2024-02-02", "Trouble in Pairs counts spells that were cast even if they didn't resolve. This means it still counts spells that were countered.")
        ruling("2024-02-02", "Extra turns can still be created while Trouble in Pairs is on the battlefield. They're not skipped until they would begin, so if Trouble in Pairs leaves the battlefield before that happens, the extra turns will be unaffected.")
        ruling("2024-02-02", "Trouble in Pairs doesn't have to be on the battlefield to see the first card an opponent draws in a turn or the first spell they cast in a turn. As long as it's already on the battlefield when they draw their second card or cast their second spell in a turn, its last ability will trigger.")
        ruling("2024-02-02", "Trouble in Pairs's last ability can trigger multiple times per opponent per turn. For example, if an opponent draws a second card, casts a second spell, and attacks you with two or more creatures in a single turn, Trouble in Pairs's last ability will trigger once when each of those events occurs, for a total of three times.")
        ruling("2024-02-02", "If an opponent attacks you with two or more creatures more than once in a turn (probably because they've generated extra combat phases), Trouble in Pairs's last ability will trigger each time that happens.")
    }
}
