package com.wingedsheep.mtg.sets.definitions.hob.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Bard the Bowman
 * {1}{W}{U}
 * Legendary Creature — Human Archer
 * 1/3
 * Reach
 * Whenever you draw your second card each turn, put a +1/+1 counter on target creature. It gains
 * lifelink until end of turn.
 *
 *  - **"Whenever you draw your second card each turn"** is [Triggers.NthCardDrawn]`(2)`, whose default
 *    scope is the controller. The per-turn tally lives on `CardsDrawnThisTurnComponent` and resets each
 *    turn, so the ability fires at most once per turn without needing `oncePerTurn`; a single multi-card
 *    draw that crosses the threshold fires it exactly once (CR 121.2), and cards put into hand without
 *    the word "draw" (CR 121.5) never advance the count.
 *  - **"target creature"** is unrestricted — any creature, not just one you control — so a bare
 *    [TargetCreature] with no filter. The counter and the lifelink grant share the one target `t`, so
 *    if it becomes illegal the whole ability fizzles and neither half happens.
 */
val BardTheBowman = card("Bard the Bowman") {
    manaCost = "{1}{W}{U}"
    colorIdentity = "WU"
    typeLine = "Legendary Creature — Human Archer"
    oracleText = "Reach\n" +
        "Whenever you draw your second card each turn, put a +1/+1 counter on target creature. " +
        "It gains lifelink until end of turn."
    power = 1
    toughness = 3
    keywords(Keyword.REACH)

    triggeredAbility {
        trigger = Triggers.NthCardDrawn(2)
        val t = target("target creature to get a +1/+1 counter and lifelink", TargetCreature())
        effect = Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 1, t)
            .then(Effects.GrantKeyword(Keyword.LIFELINK, t))
        description = "Whenever you draw your second card each turn, put a +1/+1 counter on " +
            "target creature. It gains lifelink until end of turn."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "145"
        artist = "Miklós Ligeti"
        flavorText = "No one had dared to face the Dragon for many an age; nor would they have " +
            "dared now, if it hadn't been for the grim-voiced Bard urging them on."
        imageUri = "https://cards.scryfall.io/normal/front/0/b/0b84e232-428c-424a-848c-ef95debc6e50.jpg?1784377009"
    }
}
