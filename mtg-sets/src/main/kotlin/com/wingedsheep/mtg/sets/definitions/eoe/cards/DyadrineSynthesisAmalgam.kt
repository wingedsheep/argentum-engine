package com.wingedsheep.mtg.sets.definitions.eoe.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersWithDynamicCounters
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.CreateTokenEffect
import com.wingedsheep.sdk.scripting.effects.DrawCardsEffect
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.MayEffect
import com.wingedsheep.sdk.scripting.effects.RemoveCountersEffect
import com.wingedsheep.sdk.scripting.effects.SelectFromCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectionMode
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.dsl.Effects

/**
 * Dyadrine, Synthesis Amalgam
 * {X}{G}{W}
 * Legendary Artifact Creature — Construct
 * 0/1
 *
 * Trample
 * Dyadrine enters with a number of +1/+1 counters on it equal to the amount of mana spent to cast it.
 * Whenever you attack, you may remove a +1/+1 counter from each of two creatures you control.
 * If you do, draw a card and create a 2/2 colorless Robot artifact creature token.
 */
val DyadrineSynthesisAmalgam = card("Dyadrine, Synthesis Amalgam") {
    manaCost = "{X}{G}{W}"
    colorIdentity = "GW"
    typeLine = "Legendary Artifact Creature — Construct"
    power = 0
    toughness = 1
    oracleText = "Trample\n" +
        "Dyadrine enters with a number of +1/+1 counters on it equal to the amount of mana spent to cast it.\n" +
        "Whenever you attack, you may remove a +1/+1 counter from each of two creatures you control. " +
        "If you do, draw a card and create a 2/2 colorless Robot artifact creature token."

    keywords(Keyword.TRAMPLE)

    // Enters with +1/+1 counters equal to the total mana spent to cast it.
    replacementEffect(
        EntersWithDynamicCounters(count = DynamicAmount.TotalManaSpent)
    )

    // Whenever you attack, optionally remove a +1/+1 counter from each of two creatures
    // you control. If both removals happen, draw a card and create a 2/2 Robot token.
    triggeredAbility {
        trigger = Triggers.YouAttack
        effect = MayEffect(
            effect = Effects.Composite(
                listOf(
                    GatherCardsEffect(
                        source = CardSource.BattlefieldMatching(
                            filter = GameObjectFilter.Creature
                                .youControl()
                                .withCounter(Counters.PLUS_ONE_PLUS_ONE),
                            player = Player.You,
                        ),
                        storeAs = "candidates",
                    ),
                    SelectFromCollectionEffect(
                        from = "candidates",
                        selection = SelectionMode.ChooseExactly(DynamicAmount.Fixed(2)),
                        storeSelected = "chosen",
                        useTargetingUI = true,
                        prompt = "Choose two creatures you control to remove a +1/+1 counter from",
                    ),
                    RemoveCountersEffect(
                        counterType = Counters.PLUS_ONE_PLUS_ONE,
                        count = 1,
                        target = EffectTarget.PipelineTarget("chosen", 0),
                    ),
                    RemoveCountersEffect(
                        counterType = Counters.PLUS_ONE_PLUS_ONE,
                        count = 1,
                        target = EffectTarget.PipelineTarget("chosen", 1),
                    ),
                    DrawCardsEffect(1, EffectTarget.Controller),
                    CreateTokenEffect(
                        power = 2,
                        toughness = 2,
                        colors = setOf(), // colorless
                        creatureTypes = setOf("Robot"),
                        artifactToken = true,
                        imageUri = "https://cards.scryfall.io/normal/front/c/4/c46f9a07-005c-44b7-8057-b2f00b274dd6.jpg?1756281130",
                    ),
                )
            ),
            descriptionOverride = "You may remove a +1/+1 counter from each of two creatures you " +
                "control. If you do, draw a card and create a 2/2 colorless Robot artifact creature token.",
        )
        description = "Whenever you attack, you may remove a +1/+1 counter from each of two " +
            "creatures you control. If you do, draw a card and create a 2/2 colorless Robot " +
            "artifact creature token."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "216"
        artist = "Igor Grechanyi"
        imageUri = "https://cards.scryfall.io/normal/front/9/9/994ca692-7138-4dcb-bf46-5da530f86036.jpg?1752947440"
    }
}
