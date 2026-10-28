package com.wingedsheep.mtg.sets.definitions.soi.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersWithDynamicCounters
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Diregraf Colossus
 * {2}{B}
 * Creature — Zombie Giant
 * 2/2
 * This creature enters with a +1/+1 counter on it for each Zombie card in your graveyard.
 * Whenever you cast a Zombie spell, create a tapped 2/2 black Zombie creature token.
 *
 * The entry count is an as-enters replacement ([EntersWithDynamicCounters]), measured while the
 * Colossus is still on its way in — so one returned from the graveyard counts itself (2016 ruling).
 */
val DiregrafColossus = card("Diregraf Colossus") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Zombie Giant"
    oracleText = "This creature enters with a +1/+1 counter on it for each Zombie card in your graveyard.\n" +
        "Whenever you cast a Zombie spell, create a tapped 2/2 black Zombie creature token."
    power = 2
    toughness = 2

    replacementEffect(
        EntersWithDynamicCounters(
            count = DynamicAmounts.count(Player.You, Zone.GRAVEYARD, GameObjectFilter.Any.withSubtype(Subtype.ZOMBIE))
        )
    )

    triggeredAbility {
        trigger = Triggers.you.casts(GameObjectFilter.Any.withSubtype(Subtype.ZOMBIE))
        effect = Effects.CreateToken(
            power = 2,
            toughness = 2,
            colors = setOf(Color.BLACK),
            creatureTypes = setOf("Zombie"),
            tapped = true
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "107"
        artist = "Vincent Proce"
        imageUri = "https://cards.scryfall.io/normal/front/9/0/9039e8f6-5513-4307-a1b5-07209dc8e1c8.jpg?1783937777"
        ruling(
            "2016-04-08",
            "If Diregraf Colossus enters the battlefield from your graveyard, its first ability will count itself among the Zombie cards in your graveyard. Similarly, if it enters the battlefield at the same time as another Zombie card enters the battlefield from your graveyard, the ability will count that other Zombie."
        )
        ruling(
            "2016-04-08",
            "Diregraf Colossus's last ability won't trigger when you cast it because it's not on the battlefield yet."
        )
    }
}
