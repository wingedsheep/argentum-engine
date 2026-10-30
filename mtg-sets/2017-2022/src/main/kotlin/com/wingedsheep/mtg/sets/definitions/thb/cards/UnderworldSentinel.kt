package com.wingedsheep.mtg.sets.definitions.thb.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Underworld Sentinel
 * {3}{B}{B}
 * Creature — Skeleton Soldier
 * 4/5
 * Whenever this creature attacks, exile target creature card from your graveyard.
 * When this creature dies, put all cards exiled with it onto the battlefield.
 *
 * Trove Warden's linked-exile pair: the attack trigger files its card under the Sentinel's
 * linked-exile pile ([Effects.ExileLinkedToSource]), and the dies trigger returns the whole pile
 * with [Effects.ReturnLinkedExile] — under the Sentinel's controller's control, not the owners'
 * (ruling: a stolen Sentinel's pile comes back under the thief's control). The pile belongs to this
 * object, so a Sentinel that leaves without dying abandons it for good (ruling). With no creature
 * card in the graveyard the attack trigger simply has no target; the Sentinel still attacks.
 */
val UnderworldSentinel = card("Underworld Sentinel") {
    manaCost = "{3}{B}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Skeleton Soldier"
    power = 4
    toughness = 5
    oracleText = "Whenever this creature attacks, exile target creature card from your graveyard.\n" +
        "When this creature dies, put all cards exiled with it onto the battlefield."

    triggeredAbility {
        trigger = Triggers.self.attacks()
        val creatureCard = target(TargetFilter.CreatureInYourGraveyard)
        effect = Effects.ExileLinkedToSource(creatureCard)
        description = "Whenever this creature attacks, exile target creature card from your graveyard."
    }

    triggeredAbility {
        trigger = Triggers.self.dies()
        effect = Effects.ReturnLinkedExile()
        description = "When this creature dies, put all cards exiled with it onto the battlefield."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "293"
        artist = "Vincent Proce"
        flavorText = "Deathless soldiers called phylaskia guard the borders of the Underworld."
        imageUri = "https://cards.scryfall.io/normal/front/d/8/d8c7aa89-3777-49ec-a6d0-c568150864e8.jpg?1783931489"
        ruling("2020-01-24", "Underworld Sentinel can attack even if there's no creature card in your graveyard to exile.")
        ruling(
            "2020-01-24",
            "If you gain control of another player's Underworld Sentinel and it then dies, you'll put the " +
                "exiled cards onto the battlefield under your control."
        )
        ruling(
            "2020-01-24",
            "If Underworld Sentinel leaves the battlefield without dying, the exiled creature cards are exiled " +
                "for the rest of the game. If Underworld Sentinel returns to the battlefield later, it'll be a " +
                "new object with no memory of the creatures it exiled previously."
        )
    }
}
