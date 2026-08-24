package com.wingedsheep.mtg.sets.definitions.mkm.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Urgent Necropsy
 * {2}{B}{G}
 * Instant
 *
 * As an additional cost to cast this spell, collect evidence X, where X is the total mana value of
 * the permanents this spell targets.
 * Destroy up to one target artifact, up to one target creature, up to one target enchantment, and
 * up to one target planeswalker.
 *
 * Two shapes, both already spoken by the SDK:
 *
 * **The cost.** `Costs.additional.CollectEvidenceForTargetsTotalManaValue` is the ordinary
 * collect-evidence atom carrying a derived threshold instead of a literal one. The engine prices it
 * at CR 601.2f — after the targets are announced at 601.2c, before it is paid at 601.2h — so the
 * card names no number of its own. Per the printed ruling a caster who can't reach that total
 * *can't choose to collect evidence at all*, which makes such a set of targets an illegal cast
 * (CR 601.2e) rather than a cheaper one; the client therefore raises its evidence picker only once
 * targets are chosen, priced on what was actually chosen.
 *
 * **The targets.** Four independent "up to one target" requirements, each its own optional prompt
 * over its own type — so the caster may pick any subset, including none. The effect gathers
 * whatever was chosen ([CardSource.ChosenTargets]) and destroys it in one step, the same routing
 * Boom Box uses; binding each target positionally would break the moment a middle requirement is
 * skipped.
 */
val UrgentNecropsy = card("Urgent Necropsy") {
    manaCost = "{2}{B}{G}"
    colorIdentity = "BG"
    typeLine = "Instant"
    oracleText = "As an additional cost to cast this spell, collect evidence X, where X is the " +
        "total mana value of the permanents this spell targets.\n" +
        "Destroy up to one target artifact, up to one target creature, up to one target " +
        "enchantment, and up to one target planeswalker."

    additionalCost(Costs.additional.CollectEvidenceForTargetsTotalManaValue)

    spell {
        target(
            "up to one target artifact",
            TargetObject(optional = true, filter = TargetFilter.Artifact),
        )
        target(
            "up to one target creature",
            TargetObject(optional = true, filter = TargetFilter.Creature),
        )
        target(
            "up to one target enchantment",
            TargetObject(optional = true, filter = TargetFilter.Enchantment),
        )
        target(
            "up to one target planeswalker",
            TargetObject(optional = true, filter = TargetFilter.Planeswalker),
        )
        effect = Effects.Pipeline {
            val chosen = gather(CardSource.ChosenTargets)
            destroy(chosen)
        }
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "240"
        artist = "Uriah Voth"
        flavorText = "\"The Golgari can read the dead like a newspaper.\"\n" +
            "—Taboro of the Foundway Associates"
        imageUri = "https://cards.scryfall.io/normal/front/d/2/d2ac346a-fc46-4023-aa60-4d55170697dc.jpg?1783912833"
    }
}
