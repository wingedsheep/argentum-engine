package com.wingedsheep.mtg.sets.definitions.lorwyneclipsed.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.EffectPatterns
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AdditionalCost
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Champion of the Weird
 * {3}{B}
 * Creature — Goblin Berserker
 * 5/5
 *
 * As an additional cost to cast this spell, behold a Goblin and exile it.
 * (Exile a Goblin you control or a Goblin card from your hand.)
 * Pay 1 life, Blight 2: Target opponent blights 2. Activate only as a sorcery.
 * When this creature leaves the battlefield, return the exiled card to its owner's hand.
 */
val ChampionOfTheWeird = card("Champion of the Weird") {
    manaCost = "{3}{B}"
    typeLine = "Creature — Goblin Berserker"
    power = 5
    toughness = 5
    oracleText = "As an additional cost to cast this spell, behold a Goblin and exile it. " +
        "(Exile a Goblin you control or a Goblin card from your hand.)\n" +
        "Pay 1 life, Blight 2: Target opponent blights 2. Activate only as a sorcery.\n" +
        "When this creature leaves the battlefield, return the exiled card to its owner's hand."

    additionalCost(AdditionalCost.BeholdAndExile(filter = Filters.WithSubtype("Goblin")))

    activatedAbility {
        cost = Costs.Composite(Costs.PayLife(1), Costs.Blight(2))
        target = Targets.Opponent
        effect = EffectPatterns.blight(2, Player.TargetOpponent)
        timing = TimingRule.SorcerySpeed
    }

    triggeredAbility {
        trigger = Triggers.LeavesBattlefield
        effect = Effects.ReturnLinkedExileToHand()
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "95"
        artist = "Lucas Graciano"
        imageUri = "https://cards.scryfall.io/normal/front/e/6/e600dd01-65ac-489c-a52a-decbc3a9a4f3.jpg?1767659622"
    }
}
