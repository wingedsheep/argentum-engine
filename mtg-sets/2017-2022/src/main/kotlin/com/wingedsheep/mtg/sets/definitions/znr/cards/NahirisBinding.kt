package com.wingedsheep.mtg.sets.definitions.znr.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantAttack
import com.wingedsheep.sdk.scripting.CantBlock
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.PreventActivatedAbilities
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Nahiri's Binding — Zendikar Rising #29
 * {1}{W}{W} · Enchantment — Aura
 *
 * Enchant creature or planeswalker
 * Enchanted permanent can't attack or block, and its activated abilities can't be activated.
 *
 * Prison Sentence's three-static lock (combat lock on the attached creature plus
 * [PreventActivatedAbilities] scoped by `attachedToBySource()`, mana and loyalty abilities
 * included) on Hold for Questioning's creature-or-planeswalker host.
 */
val NahirisBinding = card("Nahiri's Binding") {
    manaCost = "{1}{W}{W}"
    colorIdentity = "W"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature or planeswalker\n" +
        "Enchanted permanent can't attack or block, and its activated abilities can't be activated."

    auraTarget = TargetObject(filter = TargetFilter(GameObjectFilter.CreatureOrPlaneswalker))

    staticAbility {
        ability = CantAttack(filter = GroupFilter.attachedCreature())
    }

    staticAbility {
        ability = CantBlock(filter = GroupFilter.attachedCreature())
    }

    staticAbility {
        ability = PreventActivatedAbilities(
            filter = GameObjectFilter.Permanent.attachedToBySource(),
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "29"
        artist = "Magali Villeneuve"
        flavorText = "\"This is my world, Jace, down to its very bones.\""
        imageUri = "https://cards.scryfall.io/normal/front/4/2/421eac28-d6f2-44f2-b7ca-eb73fbf23887.jpg?1783929411"
        ruling("2020-09-25", "No abilities of the enchanted permanent can be activated, including mana abilities.")
        ruling("2020-09-25", "A planeswalker enchanted this way may be attacked.")
    }
}
