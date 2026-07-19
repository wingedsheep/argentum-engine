package com.wingedsheep.mtg.sets.definitions.msh.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.GrantWard
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.effects.WardCost

/**
 * Super Strength — Marvel Super Heroes #189
 * {4}{G} · Enchantment — Aura · Common
 *
 * Enchant creature
 * Enchanted creature gets +4/+4 and has trample and ward {1}.
 *
 * The three riders are three independent static abilities over the default
 * "attached creature" scope: [ModifyStats] (layer 7c), [GrantKeyword] for trample and
 * [GrantWard] for the ward {1} (the engine generates both the keyword display and the
 * ward enforcement trigger).
 */
val SuperStrength = card("Super Strength") {
    manaCost = "{4}{G}"
    colorIdentity = "G"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\n" +
        "Enchanted creature gets +4/+4 and has trample and ward {1}. (Whenever enchanted " +
        "creature becomes the target of a spell or ability an opponent controls, counter it " +
        "unless that player pays {1}.)"

    auraTarget = Targets.Creature

    staticAbility {
        ability = ModifyStats(4, 4)
    }

    staticAbility {
        ability = GrantKeyword(Keyword.TRAMPLE)
    }

    staticAbility {
        ability = GrantWard(WardCost.Mana("{1}"))
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "189"
        artist = "Tyler Walpole"
        flavorText = "\"SEE! HULK STRONG . . . STRONGEST THERE IS!\""
        imageUri = "https://cards.scryfall.io/normal/front/1/d/1d24d200-63d4-44cf-8d3a-6c6ece5e16ff.jpg?1783902911"
    }
}
