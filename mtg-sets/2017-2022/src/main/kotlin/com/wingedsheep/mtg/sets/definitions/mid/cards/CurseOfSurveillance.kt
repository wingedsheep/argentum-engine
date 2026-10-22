package com.wingedsheep.mtg.sets.definitions.mid.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetPlayer

/**
 * Curse of Surveillance (Innistrad: Midnight Hunt)
 * {4}{U}
 * Enchantment — Aura Curse
 *
 * Enchant player
 * At the beginning of enchanted player's upkeep, any number of target players other than that
 * player each draw cards equal to the number of Curses attached to that player.
 *
 * "Other than that player" is a [TargetPlayer] restriction comparing the candidate with
 * [Player.EnchantedPlayer]; the count is [DynamicAmounts.cursesAttachedToEnchantedPlayer], which
 * includes this Curse. The Curse's controller picks the targets wherever it is attached (the
 * card's ruling).
 */
val CurseOfSurveillance = card("Curse of Surveillance") {
    manaCost = "{4}{U}"
    colorIdentity = "U"
    typeLine = "Enchantment — Aura Curse"
    oracleText = "Enchant player\n" +
        "At the beginning of enchanted player's upkeep, any number of target players other than " +
        "that player each draw cards equal to the number of Curses attached to that player."

    auraTarget = Targets.Player

    triggeredAbility {
        trigger = Triggers.player(Player.EnchantedPlayer).beginningOf(Step.UPKEEP)
        target(
            TargetPlayer(
                unlimited = true,
                restriction = Conditions.Not(Conditions.PlayerIs(Player.Candidate, Player.EnchantedPlayer)),
                descriptionOverride = "any number of target players other than enchanted player"
            )
        )
        effect = Effects.ForEachTarget(
            Effects.DrawCards(
                DynamicAmounts.cursesAttachedToEnchantedPlayer(),
                EffectTarget.ContextTarget(0)
            )
        )
        description = "At the beginning of enchanted player's upkeep, any number of target players " +
            "other than that player each draw cards equal to the number of Curses attached to that player."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "46"
        artist = "Igor Kieryluk"
        imageUri = "https://cards.scryfall.io/normal/front/d/6/d6a5b3b2-4f27-4c97-9d87-d7bdcc06d36a.jpg?1783925645"
    }
}
