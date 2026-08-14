package com.wingedsheep.mtg.sets.definitions.dft.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetCreature
import com.wingedsheep.sdk.scripting.targets.TargetPermanent

/**
 * Scrap Compactor — Aetherdrift #242
 * {1} · Artifact
 *
 * {3}, {T}, Sacrifice this artifact: It deals 3 damage to target creature.
 * {6}, {T}, Sacrifice this artifact: Destroy target creature or Vehicle.
 *
 * Two independent activated abilities sharing one body — sacrificing the Compactor is part of
 * each cost, so at most one of them ever resolves. Both are modelled as separate
 * `activatedAbility` blocks rather than a modal choice: the printed card is two abilities, and
 * the action menu should show both prices side by side.
 *
 * "It deals 3 damage" — the Compactor is the damage source even though it is already in the
 * graveyard when the ability resolves (CR 608.2g / last-known information); the default
 * `damageSource` on [Effects.DealDamage] is the ability's source, which is exactly that.
 * The second mode reaches Vehicles too ([GameObjectFilter.CreatureOrVehicle]), which a Vehicle
 * matches by subtype whether or not it is currently crewed.
 */
val ScrapCompactor = card("Scrap Compactor") {
    manaCost = "{1}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "{3}, {T}, Sacrifice this artifact: It deals 3 damage to target creature.\n" +
        "{6}, {T}, Sacrifice this artifact: Destroy target creature or Vehicle."

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{3}"), Costs.Tap, Costs.SacrificeSelf)
        val t = target("target creature", TargetCreature())
        effect = Effects.DealDamage(3, t)
        description = "{3}, {T}, Sacrifice this artifact: It deals 3 damage to target creature."
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{6}"), Costs.Tap, Costs.SacrificeSelf)
        val t = target(
            "target creature or Vehicle",
            TargetPermanent(filter = TargetFilter(GameObjectFilter.CreatureOrVehicle))
        )
        effect = Effects.Move(t, Zone.GRAVEYARD, byDestruction = true)
        description = "{6}, {T}, Sacrifice this artifact: Destroy target creature or Vehicle."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "242"
        artist = "Viko Menezes"
        flavorText = "From the screaming of Duskmourn's possessed metal came the happy cries of freed souls."
        imageUri = "https://cards.scryfall.io/normal/front/1/2/12ccd555-60d4-49a1-b022-1e119344172a.jpg?1783907846"
    }
}
