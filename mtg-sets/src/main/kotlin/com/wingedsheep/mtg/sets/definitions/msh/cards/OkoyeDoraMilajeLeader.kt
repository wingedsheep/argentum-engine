package com.wingedsheep.mtg.sets.definitions.msh.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Okoye, Dora Milaje Leader — Marvel Super Heroes #27
 * {3}{W} · Legendary Creature — Human Warrior Hero · 3/2
 *
 * When Okoye enters, create two 1/1 white Soldier creature tokens.
 * Attacking creature tokens you control have first strike.
 *
 * The static ability is an anthem-shaped [GrantKeyword] over a [GroupFilter] narrowed to
 * *attacking* *token* creatures you control — all three predicates are evaluated against
 * projected state each time the layer system runs, so a token that stops attacking (or a
 * nontoken attacker) never gets first strike.
 */
val OkoyeDoraMilajeLeader = card("Okoye, Dora Milaje Leader") {
    manaCost = "{3}{W}"
    colorIdentity = "W"
    typeLine = "Legendary Creature — Human Warrior Hero"
    power = 3
    toughness = 2
    oracleText = "When Okoye enters, create two 1/1 white Soldier creature tokens.\n" +
        "Attacking creature tokens you control have first strike."

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = setOf(Color.WHITE),
            creatureTypes = setOf("Soldier"),
            count = 2,
            imageUri = "https://cards.scryfall.io/normal/front/e/c/ecd686bf-d14b-491c-b0c5-88fc8f0472f9.jpg?1783902804",
        )
        description = "When Okoye enters, create two 1/1 white Soldier creature tokens."
    }

    staticAbility {
        ability = GrantKeyword(
            Keyword.FIRST_STRIKE,
            GroupFilter(GameObjectFilter.Creature.token().attacking().youControl()),
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "27"
        artist = "L.A. Draws"
        flavorText = "\"No, my king, we did not kill them. There were, however, many injuries.\""
        imageUri = "https://cards.scryfall.io/normal/front/2/c/2c89acf5-20f0-4441-b96f-2c5cacd685fb.jpg?1783902970"
    }
}
