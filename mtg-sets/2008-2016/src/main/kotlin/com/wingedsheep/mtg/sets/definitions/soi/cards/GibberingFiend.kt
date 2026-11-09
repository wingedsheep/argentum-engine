package com.wingedsheep.mtg.sets.definitions.soi.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Gibbering Fiend (Shadows over Innistrad #161)
 * {1}{R}
 * Creature — Devil
 * 2/1
 *
 * When this creature enters, it deals 1 damage to each opponent.
 * Delirium — At the beginning of each opponent's upkeep, if there are four or more card types among
 * cards in your graveyard, this creature deals 1 damage to that player.
 */
val GibberingFiend = card("Gibbering Fiend") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Devil"
    power = 2
    toughness = 1
    oracleText = "When this creature enters, it deals 1 damage to each opponent.\n" +
        "Delirium — At the beginning of each opponent's upkeep, if there are four or more card types " +
        "among cards in your graveyard, this creature deals 1 damage to that player."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.DealDamage(1, EffectTarget.PlayerRef(Player.EachOpponent))
    }

    triggeredAbility {
        trigger = Triggers.anOpponent.beginningOf(Step.UPKEEP)
        interveningIf = Conditions.Delirium()
        effect = Effects.DealDamage(1, EffectTarget.PlayerRef(Player.TriggeringPlayer))
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "161"
        artist = "Chris Rallis"
        imageUri = "https://cards.scryfall.io/normal/front/5/1/5113c437-3276-4591-b95e-c2afb9d6329a.jpg"
    }
}
