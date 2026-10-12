package com.wingedsheep.mtg.sets.definitions.bro.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.divRoundedUp
import com.wingedsheep.sdk.dsl.unearth
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Terisian Mindbreaker
 * {7}
 * Artifact Creature — Juggernaut
 * 6/4
 * Whenever this creature attacks, defending player mills half their library, rounded up.
 * Unearth {1}{U}{U}{U}
 *
 * Specimen Freighter's defending-player mill with Jidoor's "half their library" count, read off
 * the defending player at resolution and rounded up with [divRoundedUp].
 */
val TerisianMindbreaker = card("Terisian Mindbreaker") {
    manaCost = "{7}"
    colorIdentity = "U"
    typeLine = "Artifact Creature — Juggernaut"
    power = 6
    toughness = 4
    oracleText = "Whenever this creature attacks, defending player mills half their library, rounded up.\n" +
        "Unearth {1}{U}{U}{U} ({1}{U}{U}{U}: Return this card from your graveyard to the battlefield. It gains " +
        "haste. Exile it at the beginning of the next end step or if it would leave the battlefield. Unearth " +
        "only as a sorcery.)"

    triggeredAbility {
        trigger = Triggers.self.attacks()
        effect = Patterns.Library.mill(
            count = DynamicAmounts.zone(Player.DefendingPlayer, Zone.LIBRARY).count() divRoundedUp 2,
            target = EffectTarget.PlayerRef(Player.DefendingPlayer)
        )
    }

    unearth("{1}{U}{U}{U}")

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "83"
        artist = "Steve Prescott"
        imageUri = "https://cards.scryfall.io/normal/front/6/b/6bd86daf-7cee-454c-8d4d-97f59fe36958.jpg?1783920095"
    }
}
