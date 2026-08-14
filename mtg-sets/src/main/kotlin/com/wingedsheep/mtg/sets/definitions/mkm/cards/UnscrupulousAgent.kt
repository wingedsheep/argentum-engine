package com.wingedsheep.mtg.sets.definitions.mkm.cards

import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Unscrupulous Agent
 * {1}{B}
 * Creature — Elf Detective
 * 1/1
 * When this creature enters, target opponent exiles a card from their hand.
 *
 * The exile is chosen by the target opponent: `Patterns.Hand.exileFromHand` derives the
 * chooser from the effect target, so the opponent picks which of their own cards is exiled
 * (same primitive as Skullcap Snail).
 */
val UnscrupulousAgent = card("Unscrupulous Agent") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Elf Detective"
    oracleText = "When this creature enters, target opponent exiles a card from their hand."
    power = 1
    toughness = 1

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        val opponent = target("target opponent", Targets.Opponent)
        effect = Patterns.Hand.exileFromHand(1, opponent)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "109"
        artist = "Michal Ivan"
        flavorText = "Obtaining evidence through the \"proper\" channels can take weeks of paperwork. " +
            "Sometimes justice just can't wait."
        imageUri = "https://cards.scryfall.io/normal/front/3/7/37692f9a-3825-43aa-aacb-1bb92cb5bd07.jpg?1783912890"
    }
}
