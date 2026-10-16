package com.wingedsheep.mtg.sets.definitions.pip.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Overencumbered
 * {1}{W}
 * Enchantment — Aura
 * Enchant opponent
 * When this Aura enters, enchanted opponent creates a Clue token, a Food token, and a Junk token.
 * At the beginning of combat on enchanted opponent's turn, that player may pay {1} for each artifact
 * they control. If they don't, creatures can't attack this combat.
 *
 *  - The three tokens are created under the enchanted opponent's control (they are its artifacts
 *    too, so the Aura hands them the first three points of its own tax).
 *  - The combat trigger is keyed to the enchanted player's beginning of combat, so "that player" is
 *    the active player the step trigger binds as its triggering player — read from the trigger
 *    rather than the Aura's attachment, so the payment still happens if the Aura leaves in response.
 *    The tax is counted at resolution; an unaffordable tax skips the prompt straight to the lock.
 *  - "Creatures can't attack this combat" is every creature (not just theirs), as a live-filter
 *    rule-modifying effect lasting until end of combat — so a creature that enters afterwards can't
 *    attack either, and an additional combat phase later in the turn is unaffected.
 */
val Overencumbered = card("Overencumbered") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant opponent\n" +
        "When this Aura enters, enchanted opponent creates a Clue token, a Food token, and a Junk token.\n" +
        "At the beginning of combat on enchanted opponent's turn, that player may pay {1} for each " +
        "artifact they control. If they don't, creatures can't attack this combat."

    auraTarget = Targets.Opponent

    triggeredAbility {
        trigger = Triggers.self.enters()
        val enchantedOpponent = EffectTarget.PlayerRef(Player.EnchantedPlayer)
        effect = Effects.CreateClue(controller = enchantedOpponent) then
            Effects.CreateFood(controller = enchantedOpponent) then
            Effects.CreateJunk(controller = enchantedOpponent)
        description = "When this Aura enters, enchanted opponent creates a Clue token, a Food token, " +
            "and a Junk token."
    }

    triggeredAbility {
        trigger = Triggers.player(Player.EnchantedPlayer).beginningOf(Step.BEGIN_COMBAT)
        effect = Effects.MayPay(
            cost = Effects.PayDynamicMana(
                amount = DynamicAmounts.battlefield(Player.TriggeringPlayer, GameObjectFilter.Artifact).count(),
                payer = Player.TriggeringPlayer
            ),
            then = Effects.Nothing,
            otherwise = Effects.CantAttackGroup(GroupFilter.AllCreatures, Duration.EndOfCombat),
            decisionMaker = EffectTarget.PlayerRef(Player.TriggeringPlayer),
            descriptionOverride = "Pay {1} for each artifact you control? If you don't, creatures " +
                "can't attack this combat."
        )
        description = "At the beginning of combat on enchanted opponent's turn, that player may pay {1} " +
            "for each artifact they control. If they don't, creatures can't attack this combat."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "18"
        artist = "Elizabeth Peiró"
        imageUri = "https://cards.scryfall.io/normal/front/4/7/47058eaa-348d-4bbb-8df5-b931eba067f3.jpg?1783912418"
    }
}
