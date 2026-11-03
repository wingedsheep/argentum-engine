package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.ScaleCounterPlacement
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Dr. Joseph M'Benga
 * {2}{G}{G}
 * Legendary Creature — Human Doctor
 * 3/3
 *
 * Other creatures you control with counters on them have hexproof.
 * If you would put one or more counters on a creature you control, put twice that many of each of
 * those kinds of counters on that creature instead.
 *
 * Dominion Supervisor's any-counter grant (minus this creature), plus Vorinclex's "if you would put"
 * doubling (placer = you, any counter kind) narrowed to creatures you control.
 */
val DrJosephMBenga = card("Dr. Joseph M'Benga") {
    manaCost = "{2}{G}{G}"
    colorIdentity = "G"
    typeLine = "Legendary Creature — Human Doctor"
    power = 3
    toughness = 3
    oracleText = "Other creatures you control with counters on them have hexproof.\n" +
        "If you would put one or more counters on a creature you control, put twice that many of each of " +
        "those kinds of counters on that creature instead."

    staticAbility {
        ability = GrantKeyword(
            Keyword.HEXPROOF,
            GroupFilter(GameObjectFilter.Creature.youControl().withAnyCounter()).other(),
        )
    }

    replacementEffect(
        ScaleCounterPlacement(
            placer = Player.You,
            appliesTo = EventPattern.CounterPlacementEvent(
                counterType = null,
                recipient = Recipient.CreatureYouControl,
            ),
        )
    )

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "196"
        artist = "Romana Kendelic"
        flavorText = "\"I prefer saving lives to taking them.\""
        imageUri = "https://cards.scryfall.io/normal/front/c/2/c27e6865-40ba-406c-9dce-d8a7a9b8035b.jpg?1792508272"
    }
}
