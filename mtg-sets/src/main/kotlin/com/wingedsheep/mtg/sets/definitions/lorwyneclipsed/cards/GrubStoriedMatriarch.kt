package com.wingedsheep.mtg.sets.definitions.lorwyneclipsed.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TriggeredAbility
import com.wingedsheep.sdk.scripting.effects.AddCountersToCollectionEffect
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Chooser
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.effects.ConditionalOnCollectionEffect
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.MayEffect
import com.wingedsheep.sdk.scripting.effects.MayPayManaEffect
import com.wingedsheep.sdk.scripting.effects.SelectFromCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectionMode
import com.wingedsheep.sdk.scripting.effects.TransformEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Grub, Storied Matriarch // Grub, Notorious Auntie
 * {2}{B}
 * Legendary Creature — Goblin Warlock // Legendary Creature — Goblin Warrior
 * 2/1
 */
private val GrubNotoriousAuntie = card("Grub, Notorious Auntie") {
    manaCost = ""
    typeLine = "Legendary Creature — Goblin Warrior"
    power = 2
    toughness = 1
    oracleText = "Menace\n" +
        "Whenever Grub attacks, you may blight 1. If you do, create a tapped and attacking token " +
        "that's a copy of the blighted creature, except it has \"At the beginning of the end step, " +
        "sacrifice this token.\"\n" +
        "At the beginning of your first main phase, you may pay {B}. If you do, transform Grub."

    keywords(Keyword.MENACE)

    val sacrificeAtEndStep = TriggeredAbility.create(
        trigger = Triggers.EachEndStep.event,
        binding = Triggers.EachEndStep.binding,
        effect = Effects.SacrificeTarget(EffectTarget.Self)
    )

    triggeredAbility {
        trigger = Triggers.Attacks
        effect = MayEffect(
            effect = CompositeEffect(
                listOf(
                    GatherCardsEffect(
                        source = CardSource.ControlledPermanents(Player.You, GameObjectFilter.Creature),
                        storeAs = "blightTargets"
                    ),
                    SelectFromCollectionEffect(
                        from = "blightTargets",
                        selection = SelectionMode.ChooseUpTo(DynamicAmount.Fixed(1)),
                        chooser = Chooser.Controller,
                        storeSelected = "blighted",
                        prompt = "Blight 1 — choose a creature you control (or cancel)",
                        useTargetingUI = true,
                        alwaysPrompt = true
                    ),
                    AddCountersToCollectionEffect("blighted", Counters.MINUS_ONE_MINUS_ONE, 1),
                    ConditionalOnCollectionEffect(
                        collection = "blighted",
                        ifNotEmpty = Effects.CreateTokenCopyOfTarget(
                            target = EffectTarget.PipelineTarget("blighted"),
                            tapped = true,
                            attacking = true,
                            triggeredAbilities = listOf(sacrificeAtEndStep)
                        )
                    )
                )
            ),
            description_override = "You may blight 1. If you do, create a tapped and attacking token that's a copy of the blighted creature."
        )
    }

    triggeredAbility {
        trigger = Triggers.FirstMainPhase
        effect = MayPayManaEffect(
            cost = ManaCost.parse("{B}"),
            effect = TransformEffect(EffectTarget.Self)
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "105"
        artist = "Jesper Ejsing"
        imageUri = "https://cards.scryfall.io/normal/back/1/f/1f51adf8-8234-4dae-aedf-7633310d5111.jpg?1767734188"
    }
}

private val GrubStoriedMatriarchFrontFace = card("Grub, Storied Matriarch") {
    manaCost = "{2}{B}"
    typeLine = "Legendary Creature — Goblin Warlock"
    power = 2
    toughness = 1
    oracleText = "Menace\n" +
        "Whenever this creature enters or transforms into Grub, Storied Matriarch, return up to one target " +
        "Goblin card from your graveyard to your hand.\n" +
        "At the beginning of your first main phase, you may pay {R}. If you do, transform Grub."

    keywords(Keyword.MENACE)

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        val goblin = target(
            "Goblin card from your graveyard",
            TargetObject(optional = true, filter = TargetFilter.CardInGraveyard.ownedByYou().withSubtype(Subtype.GOBLIN))
        )
        effect = Effects.ReturnToHand(goblin)
    }

    triggeredAbility {
        trigger = Triggers.TransformsToFront
        val goblin = target(
            "Goblin card from your graveyard",
            TargetObject(optional = true, filter = TargetFilter.CardInGraveyard.ownedByYou().withSubtype(Subtype.GOBLIN))
        )
        effect = Effects.ReturnToHand(goblin)
    }

    triggeredAbility {
        trigger = Triggers.FirstMainPhase
        effect = MayPayManaEffect(
            cost = ManaCost.parse("{R}"),
            effect = TransformEffect(EffectTarget.Self)
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "105"
        artist = "Jesper Ejsing"
        imageUri = "https://cards.scryfall.io/normal/front/1/f/1f51adf8-8234-4dae-aedf-7633310d5111.jpg?1767734188"
    }
}

val GrubStoriedMatriarch: CardDefinition = CardDefinition.doubleFacedCreature(
    frontFace = GrubStoriedMatriarchFrontFace,
    backFace = GrubNotoriousAuntie
)
