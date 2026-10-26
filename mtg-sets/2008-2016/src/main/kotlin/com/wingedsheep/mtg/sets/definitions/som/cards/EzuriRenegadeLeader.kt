package com.wingedsheep.mtg.sets.definitions.som.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Ezuri, Renegade Leader — Scars of Mirrodin #119
 * {1}{G}{G} · Legendary Creature — Elf Warrior · 2/2
 *
 * {G}: Regenerate another target Elf.
 * {2}{G}{G}{G}: Elf creatures you control get +3/+3 and gain trample until end of turn.
 *
 * - "Another target Elf" is any Elf permanent other than Ezuri, under any controller.
 * - The pump is Overrun's shape over the Elf creatures you control as the ability resolves —
 *   creatures that become Elves or enter later in the turn don't get it (ruling). Ezuri itself
 *   is an Elf and is included.
 */
val EzuriRenegadeLeader = card("Ezuri, Renegade Leader") {
    manaCost = "{1}{G}{G}"
    colorIdentity = "G"
    typeLine = "Legendary Creature — Elf Warrior"
    power = 2
    toughness = 2
    oracleText = "{G}: Regenerate another target Elf.\n" +
        "{2}{G}{G}{G}: Elf creatures you control get +3/+3 and gain trample until end of turn."

    activatedAbility {
        cost = Costs.Mana("{G}")
        val elf = target(TargetFilter.Permanent.withSubtype("Elf").other())
        effect = Effects.Regenerate(elf)
        description = "{G}: Regenerate another target Elf."
    }

    activatedAbility {
        cost = Costs.Mana("{2}{G}{G}{G}")
        effect = Effects.ForEachInGroup(
            GroupFilter(GameObjectFilter.Creature.withSubtype("Elf").youControl()),
            Effects.ModifyStats(3, 3, EffectTarget.IterationEntity) then
                Effects.GrantKeyword(Keyword.TRAMPLE, EffectTarget.IterationEntity)
        )
        description = "{2}{G}{G}{G}: Elf creatures you control get +3/+3 and gain trample until end of turn."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "119"
        artist = "Karl Kopinski"
        flavorText = "The infamous Ezuri commands the highest bounty the vedalken have ever placed upon an outlaw."
        imageUri = "https://cards.scryfall.io/normal/front/e/9/e9544132-bbb5-4ec4-af82-dad56e5091af.jpg?1783941719"
        ruling("2011-01-01", "Ezuri's first ability can't affect itself, but its second ability does.")
        ruling("2011-01-01", "Only Elf creatures you control when the last ability resolves will get +3/+3 and gain trample until end of turn. Non-Elf creatures that become Elves or Elf creatures that enter under your control later in the turn won't get the bonuses.")
    }
}
