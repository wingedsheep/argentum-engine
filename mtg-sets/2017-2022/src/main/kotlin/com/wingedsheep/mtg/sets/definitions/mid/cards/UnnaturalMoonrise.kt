package com.wingedsheep.mtg.sets.definitions.mid.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.grantedTriggeredAbility
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Unnatural Moonrise
 * {R}{G}
 * Sorcery
 * It becomes night. Until end of turn, target creature gets +1/+0 and gains trample and
 * "Whenever this creature deals combat damage to a player, draw a card."
 * Flashback {2}{R}{G}
 *
 * [Effects.BecomeNight] (as Into the Night), then the Commando Raid grant-a-trigger rail alongside an
 * end-of-turn pump and trample. The granted ability's "draw a card" is controlled by the creature's
 * controller, who is the one drawing.
 */
val UnnaturalMoonrise = card("Unnatural Moonrise") {
    manaCost = "{R}{G}"
    colorIdentity = "RG"
    typeLine = "Sorcery"
    oracleText = "It becomes night. Until end of turn, target creature gets +1/+0 and gains trample and " +
        "\"Whenever this creature deals combat damage to a player, draw a card.\"\n" +
        "Flashback {2}{R}{G} (You may cast this card from your graveyard for its flashback cost. Then exile it.)"

    spell {
        val t = target(TargetFilter.Creature)
        effect = Effects.Pipeline {
            run(Effects.BecomeNight)
            run(Effects.ModifyStats(1, 0, t))
            run(Effects.GrantKeyword(Keyword.TRAMPLE, t))
            run(
                Effects.GrantTriggeredAbility(
                    ability = grantedTriggeredAbility {
                        trigger = Triggers.self.dealsCombatDamage(Recipient.AnyPlayer)
                        effect = Effects.DrawCards(1)
                    },
                    target = t
                )
            )
        }
    }

    keywordAbility(KeywordAbility.flashback("{2}{R}{G}"))

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "247"
        artist = "Ryan Pancoast"
        imageUri = "https://cards.scryfall.io/normal/front/b/f/bfdd9df1-6a3b-4f03-8eca-00ed22c02566.jpg?1783925550"
    }
}
