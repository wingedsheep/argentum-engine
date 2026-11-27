package com.wingedsheep.mtg.sets.definitions.avr.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.grantedActivatedAbility
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantActivatedAbility
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Dual Casting
 * {1}{R}
 * Enchantment — Aura
 * Enchant creature
 * Enchanted creature has "{R}, {T}: Copy target instant or sorcery spell you control.
 * You may choose new targets for the copy."
 *
 * The granted ability belongs to the enchanted creature (it is the creature that taps), so it is a
 * [GrantActivatedAbility] onto the attached permanent — the Lavamancer's Skill shape — carrying the
 * Nivix Guildmage copy ability.
 */
val DualCasting = card("Dual Casting") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\n" +
        "Enchanted creature has \"{R}, {T}: Copy target instant or sorcery spell you control. " +
        "You may choose new targets for the copy.\""

    auraTarget = TargetObject(filter = TargetFilter.Creature)

    staticAbility {
        ability = GrantActivatedAbility(
            ability = grantedActivatedAbility {
                cost = Costs.Composite(Costs.Mana("{R}"), Costs.Tap)
                val spell = target(TargetFilter.InstantOrSorcerySpellOnStack.youControl())
                effect = Effects.CopyTargetSpell(target = spell)
            }
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "133"
        artist = "Johannes Voss"
        flavorText = "Hours after the Helvault opened, his powers returned twice as strong as before."
        imageUri = "https://cards.scryfall.io/normal/front/7/a/7aa45bfd-7075-470d-8aaa-16e34109eb5a.jpg?1783940686"
        ruling(
            "2012-05-01",
            "The ability granted by Dual Casting can target (and copy) any instant or sorcery spell you control, not just one with targets."
        )
        ruling(
            "2012-05-01",
            "The copy will have the same targets as the spell it's copying unless you choose new ones. You may change any number of the targets, including all of them or none of them. If, for one of the targets, you can't choose a new legal target, then it remains unchanged (even if the current target is illegal)."
        )
    }
}
