package com.wingedsheep.mtg.sets.definitions.lorwyneclipsed.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AdditionalCost
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Champions of the Shoal
 * {3}{U}
 * Creature — Merfolk Soldier
 * 4/6
 *
 * As an additional cost to cast this spell, behold a Merfolk and exile it.
 * (Exile a Merfolk you control or a Merfolk card from your hand.)
 * Whenever this creature enters or becomes tapped, tap up to one target creature
 * and put a stun counter on it.
 * When this creature leaves the battlefield, return the exiled card to its owner's hand.
 */
val ChampionsOfTheShoal = card("Champions of the Shoal") {
    manaCost = "{3}{U}"
    typeLine = "Creature — Merfolk Soldier"
    power = 4
    toughness = 6
    oracleText = "As an additional cost to cast this spell, behold a Merfolk and exile it. " +
        "(Exile a Merfolk you control or a Merfolk card from your hand.)\n" +
        "Whenever this creature enters or becomes tapped, tap up to one target creature " +
        "and put a stun counter on it.\n" +
        "When this creature leaves the battlefield, return the exiled card to its owner's hand."

    additionalCost(AdditionalCost.BeholdAndExile(filter = Filters.WithSubtype("Merfolk")))

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        val victim = target("creature", TargetCreature(optional = true))
        effect = Effects.Tap(victim).then(Effects.AddCounters("STUN", 1, victim))
    }

    triggeredAbility {
        trigger = Triggers.BecomesTapped
        val victim = target("creature", TargetCreature(optional = true))
        effect = Effects.Tap(victim).then(Effects.AddCounters("STUN", 1, victim))
    }

    triggeredAbility {
        trigger = Triggers.LeavesBattlefield
        effect = Effects.ReturnLinkedExileToHand()
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "46"
        artist = "Daniel Zrom"
        imageUri = "https://cards.scryfall.io/normal/front/e/1/e1acdd9c-4a6d-4373-950c-f5539b54679f.jpg?1767951924"
    }
}
