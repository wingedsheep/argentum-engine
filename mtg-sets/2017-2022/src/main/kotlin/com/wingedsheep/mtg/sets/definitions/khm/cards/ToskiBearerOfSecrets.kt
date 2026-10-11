package com.wingedsheep.mtg.sets.definitions.khm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.MustAttack
import com.wingedsheep.sdk.scripting.events.Recipient

/**
 * Toski, Bearer of Secrets
 * {3}{G}
 * Legendary Creature — Squirrel
 * 1/1
 * This spell can't be countered.
 * Indestructible
 * Toski attacks each combat if able.
 * Whenever a creature you control deals combat damage to a player, draw a card.
 *
 * The draw trigger is an observer trigger on Toski itself (as printed), firing once per
 * creature you control that connects — Toski included.
 */
val ToskiBearerOfSecrets = card("Toski, Bearer of Secrets") {
    manaCost = "{3}{G}"
    colorIdentity = "G"
    typeLine = "Legendary Creature — Squirrel"
    power = 1
    toughness = 1
    oracleText = "This spell can't be countered.\n" +
        "Indestructible\n" +
        "Toski attacks each combat if able.\n" +
        "Whenever a creature you control deals combat damage to a player, draw a card."

    cantBeCountered = true

    keywords(Keyword.INDESTRUCTIBLE)

    staticAbility {
        ability = MustAttack()
    }

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Creature.youControl()).dealsCombatDamage(Recipient.AnyPlayer)
        effect = Effects.DrawCards(1)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "197"
        artist = "Jason Rainville"
        imageUri = "https://cards.scryfall.io/normal/front/f/9/f9e79b59-94c8-4697-bf88-f0a0433170f5.jpg?1783928203"
        ruling("2021-02-05", "A spell or ability that counters spells can still target Toski. When that spell or ability resolves, Toski won't be countered, but any additional effects of that spell or ability will still happen.")
        ruling("2021-02-05", "If Toski can't attack for some reason (such as being tapped or having come under that player's control that turn), then it doesn't attack. If there's a cost associated with having it attack, you aren't forced to pay that cost, so it doesn't have to attack in that case either.")
    }
}
