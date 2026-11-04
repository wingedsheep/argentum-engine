package com.wingedsheep.mtg.sets.definitions.sos.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.div
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Pox Plague
 * {B}{B}{B}{B}{B}
 * Sorcery
 * Each player loses half their life, then discards half the cards in their hand, then sacrifices
 * half the permanents they control of their choice. Round down each time.
 *
 * A modern Pox/Smallpox. Per the original Pox rulings, the effect is processed in three separate
 * stages, in order — every player loses life, *then* every player discards, *then* every player
 * sacrifices — and the amount for each stage is not calculated until that stage is performed (so an
 * earlier stage's discards/sacrifices change the count for later stages). Each stage is therefore
 * its own [ForEachPlayerEffect] over [Player.ActivePlayerFirst] (APNAP order, CR 101.4), and the
 * three are sequenced with `then`.
 *
 * Within a stage the body's `controllerId` is rebound to the iterated player, so [Player.You] /
 * [EffectTarget.Controller] resolve to that player and the per-player "half, rounded down" amount
 * (`Divide(<count>, Fixed(2), roundUp = false)`) reads *their* own life / hand / permanents:
 * - life loss → `Divide(LifeTotal(You), 2)` to themselves
 * - discard → `Divide(AggregateZone(You, HAND), 2)` (player chooses which cards)
 * - sacrifice → `Divide(AggregateBattlefield(You, Permanent), 2)` permanents "of their choice" —
 *   one `Sacrifice(countPerPlayer = true)` rather than a loop, so every player's count is read
 *   before anyone sacrifices and the sacrifices are one simultaneous event (CR 101.4)
 */
val PoxPlague = card("Pox Plague") {
    manaCost = "{B}{B}{B}{B}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Each player loses half their life, then discards half the cards in their hand, " +
        "then sacrifices half the permanents they control of their choice. Round down each time."

    spell {
        effect = Effects.ForEachPlayer(
            Player.ActivePlayerFirst,
            Effects.LoseLife(
                amount = DynamicAmounts.lifeTotal(Player.You) / 2,
                target = EffectTarget.Controller,
            ),
        ) then Effects.ForEachPlayer(
            Player.ActivePlayerFirst,
            Effects.Discard(
                count = DynamicAmounts.zone(Player.You, Zone.HAND).count() / 2,
                target = EffectTarget.Controller,
            ),
        ) then Effects.Sacrifice(
            filter = GameObjectFilter.Permanent,
            count = DynamicAmounts.battlefield(
                Player.You,
                GameObjectFilter.Permanent,
            ).count() / 2,
            target = EffectTarget.PlayerRef(Player.ActivePlayerFirst),
            countPerPlayer = true,
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "94"
        artist = "Camille Alquier"
        flavorText = "The darkest corners of the Detention Bog hide natural laboratories that " +
            "quietly spawn deadly infections."
        imageUri = "https://cards.scryfall.io/normal/front/9/c/9c99c17b-ad3a-4859-97e8-469718b81cd9.jpg?1775937566"
    }
}
