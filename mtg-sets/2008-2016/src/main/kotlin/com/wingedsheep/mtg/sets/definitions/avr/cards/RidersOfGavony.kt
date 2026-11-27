package com.wingedsheep.mtg.sets.definitions.avr.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ChoiceType
import com.wingedsheep.sdk.scripting.EntersWithChoice
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantProtectionFromToGroup
import com.wingedsheep.sdk.scripting.ProtectionScope
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Riders of Gavony — Avacyn Restored #33
 * {2}{W}{W} · Creature — Human Knight · 3/3
 *
 * Vigilance
 * As this creature enters, choose a creature type.
 * Human creatures you control have protection from creatures of the chosen type.
 *
 * The protection is late-bound to the as-enters choice through `ProtectionScope.ChosenSubtype`,
 * which the engine resolves to "protection from <chosen type> creatures" while Riders is on the
 * battlefield. Riders is itself a Human, so it is protected too.
 */
val RidersOfGavony = card("Riders of Gavony") {
    manaCost = "{2}{W}{W}"
    typeLine = "Creature — Human Knight"
    power = 3
    toughness = 3
    oracleText = "Vigilance\nAs this creature enters, choose a creature type.\n" +
        "Human creatures you control have protection from creatures of the chosen type."

    keywords(Keyword.VIGILANCE)

    replacementEffect(EntersWithChoice(ChoiceType.CREATURE_TYPE))

    staticAbility {
        ability = GrantProtectionFromToGroup(
            scope = ProtectionScope.ChosenSubtype(),
            filter = GroupFilter(GameObjectFilter.Creature.withSubtype(Subtype.HUMAN).youControl())
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "33"
        artist = "Volkan Baǵa"
        imageUri = "https://cards.scryfall.io/normal/front/b/c/bcc1647b-9271-4426-9938-7eb620ad0769.jpg?1783940731"
    }
}
