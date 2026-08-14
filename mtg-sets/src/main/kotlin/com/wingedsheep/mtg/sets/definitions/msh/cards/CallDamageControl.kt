package com.wingedsheep.mtg.sets.definitions.msh.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Call Damage Control — Marvel Super Heroes #162
 * {1}{G} · Sorcery
 *
 * Choose up to two. Return those cards from your graveyard to your hand.
 * • Target artifact card.
 * • Target creature card.
 * • Target enchantment card.
 * • Target land card.
 *
 * Implementation notes:
 *  - "Choose up to two" is `chooseCount = 2, minChooseCount = 0` with the default
 *    `allowRepeat = false` — CR 700.2d, the same mode can't be chosen twice, so this is at most
 *    one card of each named type. Choosing zero modes is legal (the spell still resolves and does
 *    nothing), which is what `minChooseCount = 0` buys.
 *  - Each mode carries its own target requirement, chosen as the spell is cast (CR 601.2c), and
 *    each mode's payoff reads that mode's own `ContextTarget(0)`. The shared "Return those cards
 *    from your graveyard to your hand" line is folded into each mode rather than modelled as a
 *    separate step, since a mode's target *is* the card it returns; a target that leaves the
 *    graveyard before resolution simply makes that mode do nothing (CR 608.2b) without affecting
 *    the other.
 *  - A card that is both an artifact and a creature is a legal target for either mode; the two
 *    modes are separate requirements, so it may even be chosen by both — returning it once.
 */
val CallDamageControl = card("Call Damage Control") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "Choose up to two. Return those cards from your graveyard to your hand.\n" +
        "• Target artifact card.\n" +
        "• Target creature card.\n" +
        "• Target enchantment card.\n" +
        "• Target land card."

    spell {
        modal(chooseCount = 2, minChooseCount = 0) {
            mode("Return target artifact card from your graveyard to your hand") {
                target = TargetObject(
                    filter = TargetFilter(GameObjectFilter.Artifact.ownedByYou(), zone = Zone.GRAVEYARD)
                )
                effect = Effects.ReturnToHand(EffectTarget.ContextTarget(0))
            }
            mode("Return target creature card from your graveyard to your hand") {
                target = TargetObject(
                    filter = TargetFilter(GameObjectFilter.Creature.ownedByYou(), zone = Zone.GRAVEYARD)
                )
                effect = Effects.ReturnToHand(EffectTarget.ContextTarget(0))
            }
            mode("Return target enchantment card from your graveyard to your hand") {
                target = TargetObject(
                    filter = TargetFilter(GameObjectFilter.Enchantment.ownedByYou(), zone = Zone.GRAVEYARD)
                )
                effect = Effects.ReturnToHand(EffectTarget.ContextTarget(0))
            }
            mode("Return target land card from your graveyard to your hand") {
                target = TargetObject(
                    filter = TargetFilter(GameObjectFilter.Land.ownedByYou(), zone = Zone.GRAVEYARD)
                )
                effect = Effects.ReturnToHand(EffectTarget.ContextTarget(0))
            }
        }
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "162"
        artist = "Slawomir Maniak"
        flavorText = "When the heroes clock out, they clock in."
        imageUri = "https://cards.scryfall.io/normal/front/d/e/de1ed886-c33b-4cfd-8442-944cec654a8b.jpg?1783902921"
    }
}
