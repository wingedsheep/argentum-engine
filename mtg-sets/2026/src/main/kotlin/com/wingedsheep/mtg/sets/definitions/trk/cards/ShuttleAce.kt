package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Shuttle Ace
 * {4}{W}
 * Creature — Kelpien Pilot
 * 4/4
 * During your turn, this creature has flying.
 * Whenever this creature becomes tapped, you gain 2 life.
 */
val ShuttleAce = card("Shuttle Ace") {
    manaCost = "{4}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Kelpien Pilot"
    power = 4
    toughness = 4
    oracleText = "During your turn, this creature has flying.\n" +
        "Whenever this creature becomes tapped, you gain 2 life."

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = GrantKeyword(Keyword.FLYING, GroupFilter.source()),
            condition = Conditions.IsYourTurn,
        )
    }

    triggeredAbility {
        trigger = Triggers.self.becomesTapped()
        effect = Effects.GainLife(2)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "36"
        artist = "Joan Piqué Llorens"
        flavorText = "Although she was a Kelpien, a prey species, she feared no predator once behind the controls."
        imageUri = "https://cards.scryfall.io/normal/front/5/5/559f838a-24ac-4f18-8c40-23cfde6f65cc.jpg?1786005528"
    }
}
