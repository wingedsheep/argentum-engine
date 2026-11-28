package com.wingedsheep.mtg.sets.definitions.dka.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Elbrus, the Binding Blade // Withengar Unbound (Dark Ascension #147)
 *
 * A transforming Equipment: the ATTACHED-bound combat-damage trigger unattaches Elbrus from whatever
 * it is attached to, then transforms it. Per the ruling, Elbrus transforms even when it can't be
 * unattached (the equipped creature died to the damage) — the two effects are independent, and both
 * read Elbrus live, so if Elbrus itself left the battlefield neither does anything.
 *
 * Withengar's "whenever a player loses the game" is [Triggers.anyPlayer] `.losesGame()`, which fires on
 * every route to a loss (state-based action, a loses-the-game effect, a concession).
 */
private val ElbrusTheBindingBladeFront = card("Elbrus, the Binding Blade") {
    manaCost = "{7}"
    colorIdentity = "B"
    typeLine = "Legendary Artifact — Equipment"
    oracleText = "Equipped creature gets +1/+0.\n" +
        "When equipped creature deals combat damage to a player, unattach Elbrus, then transform it.\n" +
        "Equip {1}"

    staticAbility {
        ability = ModifyStats(1, 0, Filters.EquippedCreature)
    }

    triggeredAbility {
        trigger = Triggers.attached.dealsCombatDamage(Recipient.AnyPlayer)
        effect = Effects.UnattachEquipment(EffectTarget.Self) then Effects.Transform(EffectTarget.Self)
    }

    equipAbility("{1}")

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "147"
        artist = "Eric Deschamps"
        flavorText = "Those who grasp its hilt soon hear the demon's call."
        imageUri = "https://cards.scryfall.io/normal/front/6/8/683af377-c491-4f62-900c-6b83d75c33c9.jpg?1783940797"
        ruling(
            "2011-01-22",
            "The \"legend rule\" checks only for permanents with exactly the same name. Elbrus, the Binding " +
                "Blade and Withengar Unbound can be on the battlefield at the same time without either going " +
                "to its owner's graveyard.",
        )
        ruling(
            "2011-01-22",
            "Elbrus will transform even if you are unable to unattach it (most likely because the equipped " +
                "creature died).",
        )
    }
}

private val WithengarUnbound = card("Withengar Unbound") {
    manaCost = ""
    colorIdentity = "B"
    colorIndicator = "B"
    typeLine = "Legendary Creature — Demon"
    power = 13
    toughness = 13
    oracleText = "Flying, intimidate, trample\n" +
        "Whenever a player loses the game, put thirteen +1/+1 counters on Withengar Unbound."

    keywords(Keyword.FLYING, Keyword.INTIMIDATE, Keyword.TRAMPLE)

    triggeredAbility {
        trigger = Triggers.anyPlayer.losesGame()
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 13, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "147"
        artist = "Eric Deschamps"
        flavorText = "There are not enough lives on Innistrad to satisfy his thirst for retribution."
        imageUri = "https://cards.scryfall.io/normal/back/6/8/683af377-c491-4f62-900c-6b83d75c33c9.jpg?1783940797"
        ruling(
            "2011-01-22",
            "Withengar's triggered ability will trigger no matter how a player loses the game: due to a " +
                "state-based action (as a result of having a life total of 0 or less, trying to draw a card " +
                "from an empty library, or having ten poison counters), a spell or ability that says that " +
                "player loses the game, a concession, or a game loss awarded by a judge.",
        )
    }
}

val ElbrusTheBindingBlade: CardDefinition =
    CardDefinition.doubleFacedPermanent(ElbrusTheBindingBladeFront, WithengarUnbound)
