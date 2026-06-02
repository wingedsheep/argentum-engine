package com.wingedsheep.mtg.sets.definitions.tdm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ChoiceType
import com.wingedsheep.sdk.scripting.EntersWithChoice
import com.wingedsheep.sdk.scripting.EventPattern.OneOrMoreDealCombatDamageToPlayerEvent
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.ModeOption
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.TriggerSpec
import com.wingedsheep.sdk.scripting.conditions.SourceChosenModeIs
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Frostcliff Siege
 * {1}{U}{R}
 * Enchantment
 *
 * As this enchantment enters, choose Jeskai or Temur.
 * • Jeskai — Whenever one or more creatures you control deal combat damage to a player, draw a card.
 * • Temur — Creatures you control get +1/+0 and have trample and haste.
 *
 * Implementation: the cast-time choice is recorded via [EntersWithChoice] (ChoiceType.MODE).
 * The Jeskai triggered ability is gated by [SourceChosenModeIs]; the Temur mode is three
 * mode-gated continuous static abilities (a +1/+0 anthem plus trample and haste grants). A
 * static ability gated by [SourceChosenModeIs] only contributes its continuous effect while
 * that mode is the chosen one, so the lord effects vanish if the mode somehow changes.
 *
 * Note (Jeskai): the trigger uses [Triggers.YourCreaturesDealCombatDamageToPlayer], which fires
 * once per player dealt combat damage by your creatures this combat (batched per creature →
 * per player), matching the ruling that it triggers once for each player damaged.
 */
val FrostcliffSiege = card("Frostcliff Siege") {
    manaCost = "{1}{U}{R}"
    colorIdentity = "UR"
    typeLine = "Enchantment"
    oracleText = "As this enchantment enters, choose Jeskai or Temur.\n" +
        "• Jeskai — Whenever one or more creatures you control deal combat damage to a player, draw a card.\n" +
        "• Temur — Creatures you control get +1/+0 and have trample and haste."

    replacementEffect(
        EntersWithChoice(
            choiceType = ChoiceType.MODE,
            modeOptions = listOf(
                ModeOption(
                    id = "jeskai",
                    label = "Jeskai",
                    description = "Whenever one or more creatures you control deal combat damage to a player, draw a card.",
                    iconKey = "jeskai"
                ),
                ModeOption(
                    id = "temur",
                    label = "Temur",
                    description = "Creatures you control get +1/+0 and have trample and haste.",
                    iconKey = "temur"
                )
            )
        )
    )

    // Jeskai — Whenever one or more creatures you control deal combat damage to a player, draw a card.
    triggeredAbility {
        trigger = TriggerSpec(
            OneOrMoreDealCombatDamageToPlayerEvent(
                sourceFilter = GameObjectFilter.Creature.youControl()
            ),
            TriggerBinding.ANY
        )
        triggerCondition = SourceChosenModeIs("jeskai")
        effect = Effects.DrawCards(1)
    }

    // Temur — Creatures you control get +1/+0 and have trample and haste.
    staticAbility {
        condition = SourceChosenModeIs("temur")
        ability = ModifyStats(
            powerBonus = 1,
            toughnessBonus = 0,
            filter = GroupFilter.AllCreaturesYouControl
        )
    }
    staticAbility {
        condition = SourceChosenModeIs("temur")
        ability = GrantKeyword(
            keyword = Keyword.TRAMPLE,
            filter = GroupFilter.AllCreaturesYouControl
        )
    }
    staticAbility {
        condition = SourceChosenModeIs("temur")
        ability = GrantKeyword(
            keyword = Keyword.HASTE,
            filter = GroupFilter.AllCreaturesYouControl
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "187"
        artist = "Dan Murayama Scott"
        imageUri = "https://cards.scryfall.io/normal/front/a/7/a750aabb-9788-494a-841f-bf75717970a7.jpg?1743204732"
        ruling("2025-04-04", "If you somehow control Frostcliff Siege and no choice was made for it (perhaps because another permanent on the battlefield became a copy of it), it has neither of the two abilities.")
        ruling("2025-04-04", "If you chose Jeskai and creatures you control deal combat damage to multiple players at the same time, Frostcliff Siege's ability will trigger once for each player dealt combat damage this way.")
    }
}
