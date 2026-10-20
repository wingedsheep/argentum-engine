package com.wingedsheep.mtg.sets.definitions.leg.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Floral Spuzzem
 * {3}{G}
 * Creature — Elemental
 * 2/2
 * Whenever this creature attacks and isn't blocked, you may destroy target artifact defending
 * player controls. If you do, this creature assigns no combat damage this turn.
 *
 * Same shape as Farrel's Zealot: the optional trade grants `AbilityFlag.ASSIGNS_NO_COMBAT_DAMAGE`
 * until end of turn, so the Spuzzem assigns nothing rather than having its damage prevented.
 */
val FloralSpuzzem = card("Floral Spuzzem") {
    manaCost = "{3}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Elemental"
    oracleText = "Whenever this creature attacks and isn't blocked, you may destroy target artifact " +
        "defending player controls. If you do, this creature assigns no combat damage this turn."
    power = 2
    toughness = 2

    triggeredAbility {
        trigger = Triggers.self.attacksAndIsntBlocked()
        val t = target(TargetFilter.Artifact.defendingPlayerControls())
        effect = Effects.May(
            Effects.Destroy(t) then
                Effects.GrantKeyword(
                    AbilityFlag.ASSIGNS_NO_COMBAT_DAMAGE,
                    EffectTarget.Self,
                    Duration.EndOfTurn,
                ),
            descriptionOverride = "destroy that artifact. If you do, this creature assigns no combat damage this turn",
        )
        description = "Whenever this creature attacks and isn't blocked, you may destroy target artifact defending player controls. If you do, this creature assigns no combat damage this turn."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "187"
        artist = "Rob Alexander"
        imageUri = "https://cards.scryfall.io/normal/front/d/1/d141b9e3-7129-41e5-8b44-d3867e1c7e1d.jpg?1783948048"
    }
}
