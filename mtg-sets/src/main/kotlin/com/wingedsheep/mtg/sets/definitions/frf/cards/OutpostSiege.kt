package com.wingedsheep.mtg.sets.definitions.frf.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ChoiceType
import com.wingedsheep.sdk.scripting.EntersWithChoice
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModeOption
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.conditions.SourceChosenModeIs
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.DealDamageEffect
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.GrantMayPlayFromExileEffect
import com.wingedsheep.sdk.scripting.effects.MayPlayExpiry
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.dsl.Effects

/**
 * Outpost Siege
 * {3}{R}
 * Enchantment
 *
 * As this enchantment enters, choose Khans or Dragons.
 * • Khans — At the beginning of your upkeep, exile the top card of your library.
 *   Until end of turn, you may play that card.
 * • Dragons — Whenever a creature you control leaves the battlefield, this
 *   enchantment deals 1 damage to any target.
 *
 * Implementation: the choice is recorded via the generic
 * [EntersWithChoice] (ChoiceType.MODE), which writes a stable mode id onto
 * the permanent. Each of the two triggered abilities is gated by
 * [SourceChosenModeIs] so only the active mode's trigger fires.
 */
val OutpostSiege = card("Outpost Siege") {
    manaCost = "{3}{R}"
    colorIdentity = "R"
    typeLine = "Enchantment"
    oracleText = "As this enchantment enters, choose Khans or Dragons.\n" +
        "• Khans — At the beginning of your upkeep, exile the top card of your library. " +
        "Until end of turn, you may play that card.\n" +
        "• Dragons — Whenever a creature you control leaves the battlefield, this enchantment " +
        "deals 1 damage to any target."

    replacementEffect(
        EntersWithChoice(
            choiceType = ChoiceType.MODE,
            modeOptions = listOf(
                ModeOption(
                    id = "khans",
                    label = "Khans",
                    description = "At your upkeep, exile the top card of your library; you may play it this turn.",
                    iconKey = "khans"
                ),
                ModeOption(
                    id = "dragons",
                    label = "Dragons",
                    description = "Whenever a creature you control leaves the battlefield, deal 1 damage to any target.",
                    iconKey = "dragons"
                )
            )
        )
    )

    // Khans — At the beginning of your upkeep, impulse-draw the top card.
    triggeredAbility {
        trigger = Triggers.YourUpkeep
        triggerCondition = SourceChosenModeIs("khans")
        effect = Effects.Composite(listOf(
            GatherCardsEffect(
                source = CardSource.TopOfLibrary(DynamicAmount.Fixed(1)),
                storeAs = "exiledCard"
            ),
            MoveCollectionEffect(
                from = "exiledCard",
                destination = CardDestination.ToZone(Zone.EXILE)
            ),
            GrantMayPlayFromExileEffect("exiledCard", MayPlayExpiry.EndOfTurn)
        ))
    }

    // Dragons — Whenever a creature you control leaves the battlefield, deal 1 damage.
    triggeredAbility {
        trigger = Triggers.leavesBattlefield(
            filter = GameObjectFilter.Creature.youControl(),
            binding = TriggerBinding.ANY,
        )
        triggerCondition = SourceChosenModeIs("dragons")
        val any = target("target", Targets.Any)
        effect = DealDamageEffect(1, any)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "110"
        artist = "Daarken"
        imageUri = "https://cards.scryfall.io/normal/front/8/8/880f6602-f335-4ad8-9e2f-f32b6d715f72.jpg?1648060713"
    }
}
