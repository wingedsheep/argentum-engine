package com.wingedsheep.mtg.sets.definitions.tdm.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AdditionalAttackTriggers
import com.wingedsheep.sdk.scripting.ChoiceType
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.EntersWithChoice
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModeOption
import com.wingedsheep.sdk.scripting.conditions.SourceChosenModeIs
import com.wingedsheep.sdk.scripting.effects.CREATED_TOKENS
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Windcrag Siege
 * {1}{R}{W}
 * Enchantment
 *
 * As this enchantment enters, choose Mardu or Jeskai.
 * • Mardu — If a creature attacking causes a triggered ability of a permanent you control to
 *   trigger, that ability triggers an additional time.
 * • Jeskai — At the beginning of your upkeep, create a 1/1 red Goblin creature token.
 *   It gains lifelink and haste until end of turn.
 *
 * Implementation: the cast-time choice is recorded via [EntersWithChoice] (ChoiceType.MODE).
 * The Mardu mode is an [AdditionalAttackTriggers] continuous static ability gated by
 * [SourceChosenModeIs] (evaluated during projection so it only applies while Mardu is chosen);
 * the engine's `duplicateAttackTriggers` pass duplicates attack-caused triggers of permanents
 * you control. The Jeskai mode is a mode-gated upkeep trigger that creates the Goblin token, then
 * grants lifelink and haste to that freshly-created token until end of turn via the
 * [CREATED_TOKENS] pipeline (a temporary grant, so the keywords don't wrongly persist if the
 * token survives the turn).
 */
val WindcragSiege = card("Windcrag Siege") {
    manaCost = "{1}{R}{W}"
    colorIdentity = "RW"
    typeLine = "Enchantment"
    oracleText = "As this enchantment enters, choose Mardu or Jeskai.\n" +
        "• Mardu — If a creature attacking causes a triggered ability of a permanent you control " +
        "to trigger, that ability triggers an additional time.\n" +
        "• Jeskai — At the beginning of your upkeep, create a 1/1 red Goblin creature token. " +
        "It gains lifelink and haste until end of turn."

    replacementEffect(
        EntersWithChoice(
            choiceType = ChoiceType.MODE,
            modeOptions = listOf(
                ModeOption(
                    id = "mardu",
                    label = "Mardu",
                    description = "If a creature attacking causes a triggered ability of a permanent you control to trigger, that ability triggers an additional time.",
                    iconKey = "mardu"
                ),
                ModeOption(
                    id = "jeskai",
                    label = "Jeskai",
                    description = "At your upkeep, create a 1/1 red Goblin with lifelink and haste until end of turn.",
                    iconKey = "jeskai"
                )
            )
        )
    )

    // Mardu — If a creature attacking causes a triggered ability of a permanent you control to
    // trigger, that ability triggers an additional time.
    staticAbility {
        condition = SourceChosenModeIs("mardu")
        ability = AdditionalAttackTriggers(attackerFilter = GameObjectFilter.Any)
    }

    // Jeskai — At the beginning of your upkeep, create a 1/1 red Goblin creature token.
    // It gains lifelink and haste until end of turn.
    triggeredAbility {
        trigger = Triggers.YourUpkeep
        triggerCondition = SourceChosenModeIs("jeskai")
        effect = Effects.Composite(
            listOf(
                Effects.CreateToken(
                    power = 1,
                    toughness = 1,
                    colors = setOf(Color.RED),
                    creatureTypes = setOf("Goblin"),
                    imageUri = "https://cards.scryfall.io/normal/front/e/2/e265ca24-96c0-4654-a8f3-bbffe288970a.jpg?1742506636"
                ),
                Effects.GrantKeyword(
                    Keyword.LIFELINK,
                    EffectTarget.PipelineTarget(CREATED_TOKENS, 0),
                    Duration.EndOfTurn
                ),
                Effects.GrantKeyword(
                    Keyword.HASTE,
                    EffectTarget.PipelineTarget(CREATED_TOKENS, 0),
                    Duration.EndOfTurn
                )
            )
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "235"
        artist = "Néstor Ossandón Leal"
        imageUri = "https://cards.scryfall.io/normal/front/3/1/31a8329b-23a1-4c49-a579-a5da8d01435a.jpg?1743204930"
        ruling("2025-04-04", "If you somehow control Windcrag Siege and no choice was made for it (perhaps because another permanent on the battlefield became a copy of it), it has neither of the two abilities.")
    }
}
