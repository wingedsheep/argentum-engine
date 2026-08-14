package com.wingedsheep.mtg.sets.definitions.mkm.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetCreature
import com.wingedsheep.sdk.scripting.targets.TargetPermanent


/**
 * Forensic Researcher
 * {2}{U}
 * Creature — Merfolk Detective
 * 1/3
 *
 * {T}: Untap another target permanent you control.
 * {T}, Collect evidence 3: Tap target creature you don't control.
 *
 * The **activated-ability cost** shape. Collect evidence rides the shared cost vocabulary
 * (`Costs.CollectEvidence(3)` → `CostAtom.CollectEvidence`), so it composes with `{T}` through the
 * ordinary `Costs.Composite` with no mechanic-specific plumbing.
 *
 * Nothing here is linked (CR 701.59c): no ability on this card asks whether evidence was collected,
 * so the cost stamps no choice slot. Per CR 701.59b the second ability simply **isn't activatable**
 * while the graveyard's total mana value is under 3 — the enumerator omits it rather than offering
 * an unpayable activation.
 */
val ForensicResearcher = card("Forensic Researcher") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Merfolk Detective"
    power = 1
    toughness = 3
    oracleText = "{T}: Untap another target permanent you control.\n" +
        "{T}, Collect evidence 3: Tap target creature you don't control. (To collect evidence 3, " +
        "exile cards with total mana value 3 or greater from your graveyard.)"

    activatedAbility {
        cost = Costs.Tap
        val permanent = target(
            "another target permanent you control",
            TargetPermanent(filter = TargetFilter.Permanent.youControl().copy(excludeSelf = true)),
        )
        effect = Effects.Untap(permanent)
        description = "Untap another target permanent you control."
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Tap, Costs.CollectEvidence(3))
        val creature = target(
            "target creature you don't control",
            TargetCreature(filter = TargetFilter.Creature.opponentControls()),
        )
        effect = Effects.Tap(creature)
        description = "Tap target creature you don't control."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "58"
        artist = "Aldo Domínguez"
        imageUri = "https://cards.scryfall.io/normal/front/1/3/1384df5d-d705-49cb-a982-1588cbf303d8.jpg"
    }
}
