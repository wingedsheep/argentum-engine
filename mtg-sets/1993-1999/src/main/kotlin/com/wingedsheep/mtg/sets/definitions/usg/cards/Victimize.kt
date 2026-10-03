package com.wingedsheep.mtg.sets.definitions.usg.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.SuccessCriterion
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Victimize
 * {2}{B}
 * Sorcery
 * Choose two target creature cards in your graveyard. Sacrifice a creature. If you do, return the
 * chosen cards to the battlefield tapped.
 *
 * Exactly two targets (not "up to"). The sacrifice is the bare imperative ([Effects.SacrificeOwn]),
 * chosen at resolution, and the return is gated on it actually happening
 * ([SuccessCriterion.PermanentsSacrificed]) — with no creature to sacrifice, nothing returns. If one
 * target became illegal, the other still returns; if both did, the spell doesn't resolve at all.
 */
val Victimize = card("Victimize") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Choose two target creature cards in your graveyard. Sacrifice a creature. If you do, " +
        "return the chosen cards to the battlefield tapped."

    spell {
        targets(TargetFilter.CreatureInYourGraveyard, count = 2)
        effect = Effects.IfYouDo(
            action = Effects.SacrificeOwn(GameObjectFilter.Creature),
            then = Effects.ForEachTarget(
                Effects.Move(
                    EffectTarget.ContextTarget(0),
                    Zone.BATTLEFIELD,
                    placement = ZonePlacement.Tapped,
                    fromZone = Zone.GRAVEYARD,
                )
            ),
            successCriterion = SuccessCriterion.PermanentsSacrificed,
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "166"
        artist = "Val Mayerik"
        flavorText = "The priest cast Xantcha to the ground. \"It is defective. We must scrap it.\""
        imageUri = "https://cards.scryfall.io/normal/front/c/a/caafe7da-0167-4c53-bbad-172f900d137b.jpg?1783946336"
        ruling(
            "2020-11-10",
            "You must choose two targets. You can't cast Victimize targeting only one creature card."
        )
        ruling(
            "2020-11-10",
            "If one of the targeted creature cards is an illegal target (for instance, because it has left your " +
                "graveyard before Victimize resolves), you'll still sacrifice a creature and put the other card onto " +
                "the battlefield. If both are illegal targets, Victimize won't resolve. You won't sacrifice a creature."
        )
        ruling(
            "2020-11-10",
            "The creature you sacrifice isn't chosen until Victimize resolves. You can't return the creature you " +
                "sacrifice because it will still be on the battlefield at the time targets are chosen."
        )
        ruling(
            "2020-11-10",
            "As Victimize resolves, you must sacrifice a creature if able. You can't change your mind and choose " +
                "not to sacrifice anything."
        )
    }
}
