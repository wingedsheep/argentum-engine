package com.wingedsheep.mtg.sets.definitions.kld.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.MoveType
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Noxious Gearhulk
 * {4}{B}{B}
 * Artifact Creature — Construct
 * 5/4
 * Menace
 * When this creature enters, you may destroy another target creature. If a creature is destroyed
 * this way, you gain life equal to its toughness.
 *
 * - The toughness is frozen with `storeNumber` *before* the destroy, so the life gain reads the
 *   creature as it last existed on the battlefield (counters, Auras, pump effects included) — the
 *   target itself is live-only once it has left.
 * - "If a creature is destroyed this way" is the `moveTracked` set: an indestructible or
 *   regenerated creature drops out of it, so no life is gained. A creature destroyed but put into
 *   a zone other than the graveyard by a replacement (e.g. exiled instead) is still recorded as
 *   moved, so the life is gained, per the ruling.
 */
val NoxiousGearhulk = card("Noxious Gearhulk") {
    manaCost = "{4}{B}{B}"
    colorIdentity = "B"
    typeLine = "Artifact Creature — Construct"
    power = 5
    toughness = 4
    oracleText = "Menace\nWhen this creature enters, you may destroy another target creature. If a " +
        "creature is destroyed this way, you gain life equal to its toughness."

    keywords(Keyword.MENACE)

    triggeredAbility {
        trigger = Triggers.self.enters()
        optional = true
        val creature = target(TargetFilter.OtherCreature)
        effect = Effects.Pipeline {
            val toughness = storeNumber(DynamicAmounts.toughnessOf(creature))
            val targeted = gather(CardSource.ChosenTargets)
            val destroyed = moveTracked(
                targeted,
                CardDestination.ToZone(Zone.GRAVEYARD),
                moveType = MoveType.Destroy
            )
            ifNotEmpty(destroyed) {
                run(Effects.GainLife(toughness.amount))
            }
        }
        description = "When this creature enters, you may destroy another target creature. If a " +
            "creature is destroyed this way, you gain life equal to its toughness."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "96"
        artist = "Lius Lasahido"
        imageUri = "https://cards.scryfall.io/normal/front/9/f/9f86e5fe-8723-4494-b4cc-b7ac3a047bd1.jpg?1783937202"
        ruling("2016-09-20", "If the target creature has indestructible, it isn't destroyed this way and you won't gain life. If it is destroyed but put into a zone other than a graveyard, you'll gain life.")
        ruling("2016-09-20", "Use the toughness of the creature as it last existed on the battlefield to determine how much life to gain.")
    }
}
