package com.wingedsheep.mtg.sets.definitions.fem.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Elvish Hunter
 * {1}{G}
 * Creature — Elf Archer
 * 1/1
 * {1}{G}, {T}: Target creature doesn't untap during its controller's next untap step.
 *
 * Barl's Cage on a body — the same `AbilityFlag.DOESNT_UNTAP` grant, bounded by
 * [Duration.UntilAfterAffectedControllersNextUntap] so it expires against the *target's*
 * controller's untap step, not Elvish Hunter's controller's.
 */
val ElvishHunter = card("Elvish Hunter") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Elf Archer"
    oracleText = "{1}{G}, {T}: Target creature doesn't untap during its controller's next untap step."
    power = 1
    toughness = 1

    activatedAbility {
        val creature = target(TargetFilter.Creature)
        cost = Costs.Composite(Costs.Mana("{1}{G}"), Costs.Tap)
        effect = Effects.GrantKeyword(
            AbilityFlag.DOESNT_UNTAP,
            creature,
            Duration.UntilAfterAffectedControllersNextUntap(),
        )
        description = "{1}{G}, {T}: Target creature doesn't untap during its controller's next untap step."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "67a"
        artist = "Mark Poole"
        flavorText = "\"Elves often tipped their arrows with a drug that caused a deep but harmless sleep.\"\n—*Sarpadian Empires, vol. III*"
        imageUri = "https://cards.scryfall.io/normal/front/e/0/e00455ac-c7ce-4916-98ed-cca9354e3f22.jpg?1783947888"
    }
}
