package com.wingedsheep.mtg.sets.definitions.chk.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.grantedActivatedAbility
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantBlock
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantActivatedAbility
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Villainous Ogre
 * {2}{B}
 * Creature — Ogre Warrior
 * 3/2
 * This creature can't block.
 * As long as you control a Demon, this creature has "{B}: Regenerate this creature."
 *
 * The regeneration ability is a *granted* ability, not a printed one with an activation
 * restriction: a self-scoped [GrantActivatedAbility] gated by a [ConditionalStaticAbility]. The
 * ability enumerator unwraps the condition against the Ogre, so the ability only exists while you
 * control a Demon. "A Demon" is any permanent with the subtype, not just a creature.
 */
val VillainousOgre = card("Villainous Ogre") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Ogre Warrior"
    oracleText = "This creature can't block.\n" +
        "As long as you control a Demon, this creature has \"{B}: Regenerate this creature.\""
    power = 3
    toughness = 2

    staticAbility {
        ability = CantBlock()
    }

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = GrantActivatedAbility(
                ability = grantedActivatedAbility {
                    cost = Costs.Mana("{B}")
                    effect = Effects.Regenerate(EffectTarget.Self)
                    description = "{B}: Regenerate this creature."
                },
                filter = GroupFilter.source(),
            ),
            condition = Conditions.YouControl(GameObjectFilter.Permanent.withSubtype("Demon")),
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "148"
        artist = "Tony Szczudlo"
        flavorText = "\"The war saw the ogres emerge from their caves, reeking of blood, with the power of oni in their veins.\"\n—*Observations of the Kami War*"
        imageUri = "https://cards.scryfall.io/normal/front/0/2/0277c1ba-eae4-442b-8b80-20869ba20568.jpg?1783944307"
    }
}
