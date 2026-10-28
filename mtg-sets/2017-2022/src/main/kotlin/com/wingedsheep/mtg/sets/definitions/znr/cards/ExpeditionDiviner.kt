package com.wingedsheep.mtg.sets.definitions.znr.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.grantedTriggeredAbility
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantTriggeredAbility
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Expedition Diviner
 * {3}{U}
 * Creature — Merfolk Wizard
 * 3/2
 *
 * Flying
 * As long as you control another Wizard, this creature has "When this creature dies, draw a card."
 *
 * Same shape as Oculus Whelp: a [ConditionalStaticAbility] around a self-scoped
 * [GrantTriggeredAbility]. The dies trigger looks back (CR 603.10a) — the engine freezes the condition
 * onto the exit snapshot against the pre-event state, so another Wizard dying alongside it still
 * counts (the printed ruling). "Another Wizard" is any other permanent with the subtype (excludeSelf),
 * as Expedition Healer reads "another Cleric".
 */
val ExpeditionDiviner = card("Expedition Diviner") {
    manaCost = "{3}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Merfolk Wizard"
    power = 3
    toughness = 2
    oracleText = "Flying\n" +
        "As long as you control another Wizard, this creature has \"When this creature dies, draw a card.\""

    keywords(Keyword.FLYING)

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = GrantTriggeredAbility(
                ability = grantedTriggeredAbility {
                    trigger = Triggers.self.dies()
                    effect = Effects.DrawCards(1)
                    description = "When this creature dies, draw a card."
                },
                filter = GroupFilter.source(),
            ),
            condition = Conditions.YouControl(
                GameObjectFilter.Permanent.withSubtype(Subtype.WIZARD),
                excludeSelf = true
            ),
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "57"
        artist = "Heonhwa"
        flavorText = "Wizards' spells twist the fabric of reality to channel elemental energies, peer into " +
            "the future, and even calm the Roil."
        imageUri = "https://cards.scryfall.io/normal/front/0/9/090f6e94-fb5f-4861-a465-f11766303676.jpg?1783929398"
        ruling("2020-09-25", "Expedition Diviner has just one instance of its triggered ability, no matter how many other Wizards you control beyond the first.")
        ruling("2020-09-25", "If Expedition Diviner dies at the same time that each other Wizard you control leaves the battlefield, its ability triggers. For example, if the only creatures you control are two Expedition Diviners and both die at the same time, their abilities each trigger.")
    }
}
