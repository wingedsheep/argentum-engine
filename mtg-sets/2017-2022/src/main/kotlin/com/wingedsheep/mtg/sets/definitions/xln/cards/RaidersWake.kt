package com.wingedsheep.mtg.sets.definitions.xln.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Raiders' Wake
 * {3}{B}
 * Enchantment
 * Whenever an opponent discards a card, that player loses 2 life.
 * Raid — At the beginning of your end step, if you attacked this turn, target opponent discards a card.
 *
 * The life-loss half is [Megrim]'s trigger with life loss instead of damage; the raid half is
 * [DeadeyeTormentor]'s discard on [MaraudingLooter]'s end-step intervening-if.
 */
val RaidersWake = card("Raiders' Wake") {
    manaCost = "{3}{B}"
    colorIdentity = "B"
    typeLine = "Enchantment"
    oracleText = "Whenever an opponent discards a card, that player loses 2 life.\n" +
        "Raid — At the beginning of your end step, if you attacked this turn, target opponent discards a card."

    triggeredAbility {
        trigger = Triggers.anOpponent.discards()
        effect = Effects.LoseLife(2, EffectTarget.PlayerRef(Player.TriggeringPlayer))
    }

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.END)
        interveningIf = Conditions.YouAttackedThisTurn
        val opponent = target(Targets.Opponent)
        effect = Effects.Discard(1, opponent)
        description = "Raid — At the beginning of your end step, if you attacked this turn, target opponent discards a card."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "116"
        artist = "Zack Stella"
        flavorText = "One was spared to tell the gruesome tale."
        imageUri = "https://cards.scryfall.io/normal/front/f/d/fd81dd41-a982-46d2-a572-432580be73c5.jpg?1783935756"
    }
}
