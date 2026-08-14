package com.wingedsheep.mtg.sets.definitions.woe.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AdditionalETBOrLTBTriggers
import com.wingedsheep.sdk.scripting.GameObjectFilter

/** Virtue of Knowledge // Vantress Visions. */
val VirtueOfKnowledge = card("Virtue of Knowledge") {
    manaCost = "{4}{U}"
    colorIdentity = "U"
    typeLine = "Enchantment"
    oracleText = "If a permanent entering causes a triggered ability of a permanent you control " +
        "to trigger, that ability triggers an additional time."

    staticAbility {
        ability = AdditionalETBOrLTBTriggers(
            filter = GameObjectFilter.Any,
            mustBeYouControl = false,
        )
    }

    adventure("Vantress Visions") {
        manaCost = "{1}{U}"
        typeLine = "Instant — Adventure"
        oracleText = "Copy target activated or triggered ability you control. You may choose new " +
            "targets for the copy."
        spell {
            val ability = target(
                "activated or triggered ability you control",
                Targets.ActivatedOrTriggeredAbilityYouControl,
            )
            effect = Effects.CopyTargetSpellOrAbility(ability)
        }
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "76"
        artist = "Piotr Dura"
        imageUri = "https://cards.scryfall.io/normal/front/d/f/df606cf5-67dc-46f4-8c79-1d2f1d054391.jpg?1783915112"

        ruling(
            "2023-09-01",
            "Virtue of Knowledge's ability affects a permanent's own enters-the-battlefield " +
                "triggered abilities as well as other triggered abilities that trigger when that " +
                "permanent enters the battlefield."
        )
        ruling(
            "2023-09-01",
            "Replacement effects and abilities that apply as a permanent enters the battlefield " +
                "are unaffected by Virtue of Knowledge's ability."
        )
        ruling(
            "2023-09-01",
            "Virtue of Knowledge's ability doesn't copy the triggered ability; it causes that " +
                "ability to trigger an additional time. Choices are made separately for each " +
                "instance."
        )
        ruling(
            "2023-09-01",
            "Multiple copies of Virtue of Knowledge are additive: two copies cause an ability to " +
                "trigger three times, not four."
        )
        ruling(
            "2023-09-01",
            "If a permanent enters at the same time as Virtue of Knowledge and causes an ability " +
                "of a permanent you control to trigger, that ability triggers an additional time."
        )
        ruling(
            "2023-09-01",
            "The source of the copy created by Vantress Visions is the same as the source of the " +
                "original ability."
        )
        ruling(
            "2023-09-01",
            "If the ability copied by Vantress Visions is modal, its mode is copied and can't be " +
                "changed."
        )
        ruling(
            "2023-09-01",
            "If a spell is cast as an Adventure, its controller exiles it instead of putting it " +
                "into its owner's graveyard as it resolves. That player may later cast its " +
                "permanent face from exile."
        )
    }
}
