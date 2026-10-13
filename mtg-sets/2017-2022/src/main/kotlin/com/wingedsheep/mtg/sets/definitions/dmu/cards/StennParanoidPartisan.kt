package com.wingedsheep.mtg.sets.definitions.dmu.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ChoiceSlot
import com.wingedsheep.sdk.scripting.ChoiceType
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.EntersWithChoice
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModeOption
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Stenn, Paranoid Partisan
 * {W}{U}
 * Legendary Creature — Human Wizard
 * 2/2
 * As Stenn enters, choose a card type other than creature or land.
 * Spells you cast of the chosen type cost {1} less to cast.
 * {1}{W}{U}: Exile Stenn. Return it to the battlefield under its owner's control at the beginning
 * of the next end step.
 *
 * "As Stenn enters" is a true as-enters choice (a replacement, not a trigger), so it's an
 * [EntersWithChoice] of `ChoiceType.MODE` whose option ids are the card types' display names.
 * The mode id lands in [ChoiceSlot.MODE] as text, and `ofChosenCardTypeComponent(ChoiceSlot.MODE)`
 * compares a spell's card types against that text — the same predicate Arachne's
 * `ChooseCardTypeForSource` slot feeds, read from the as-enters slot instead. The offered types
 * are every castable card type except creature and land.
 *
 * The reduction is `ReduceGeneric(1)` on spells *you* cast (generic only, per the ruling). The
 * blink is Fleeting Spirit's: exile now, a delayed trigger at the next end step returns it — as a
 * new object, so Stenn makes a fresh card-type choice as it comes back.
 */
val StennParanoidPartisan = card("Stenn, Paranoid Partisan") {
    manaCost = "{W}{U}"
    colorIdentity = "WU"
    typeLine = "Legendary Creature — Human Wizard"
    power = 2
    toughness = 2
    oracleText = "As Stenn enters, choose a card type other than creature or land.\n" +
        "Spells you cast of the chosen type cost {1} less to cast.\n" +
        "{1}{W}{U}: Exile Stenn. Return it to the battlefield under its owner's control at the " +
        "beginning of the next end step."

    replacementEffect(
        EntersWithChoice(
            choiceType = ChoiceType.MODE,
            modeOptions = listOf("Artifact", "Battle", "Enchantment", "Instant", "Kindred", "Planeswalker", "Sorcery")
                .map { type ->
                    ModeOption(
                        id = type,
                        label = type,
                        description = "$type spells you cast cost {1} less to cast."
                    )
                }
        )
    )

    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.YouCast(GameObjectFilter.Any.ofChosenCardTypeComponent(ChoiceSlot.MODE)),
            modification = CostModification.ReduceGeneric(1),
        )
    }

    activatedAbility {
        cost = Costs.Mana("{1}{W}{U}")
        effect = Effects.Move(EffectTarget.Self, Zone.EXILE) then
            Effects.CreateDelayedTrigger(
                step = Step.END,
                effect = Effects.Move(EffectTarget.Self, Zone.BATTLEFIELD)
            )
        description = "{1}{W}{U}: Exile Stenn. Return it to the battlefield under its owner's control " +
            "at the beginning of the next end step."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "221"
        artist = "Mila Pesic"
        imageUri = "https://cards.scryfall.io/normal/front/0/7/07177a8e-5ded-4084-a22f-2d8fe933de7a.jpg?1783921274"

        ruling(
            "2022-09-09",
            "The cost reduction applies only to generic mana in the cost of spells you cast of the chosen card type."
        )
        ruling(
            "2022-09-09",
            "If Stenn's controller activates its last ability during a player's end step, the delayed triggered ability that returns it to the battlefield won't trigger until the next time an end step begins, usually during the next player's turn."
        )
    }
}
