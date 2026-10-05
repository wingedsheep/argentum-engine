package com.wingedsheep.mtg.sets.definitions.j22.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Lita, Mechanical Engineer
 * {2}{W}
 * Legendary Artifact Creature — Artificer
 * 3/3
 * Vigilance
 * At the beginning of your end step, untap each other artifact creature you control.
 * {3}{W}, {T}: Create a 5/5 colorless Vehicle artifact token named Zeppelin with flying and crew 3.
 *
 * The untap is a group untap over artifact creatures you control with `excludeSelf`, so Lita
 * herself, noncreature artifacts, and an uncrewed Zeppelin stay as they are. The Zeppelin is the
 * registered predefined token (`PredefinedTokens.Zeppelin`) — a noncreature `Artifact — Vehicle`
 * carrying flying and the engine's own crew keyword.
 */
val LitaMechanicalEngineer = card("Lita, Mechanical Engineer") {
    manaCost = "{2}{W}"
    typeLine = "Legendary Artifact Creature — Artificer"
    power = 3
    toughness = 3
    oracleText = "Vigilance\n" +
        "At the beginning of your end step, untap each other artifact creature you control.\n" +
        "{3}{W}, {T}: Create a 5/5 colorless Vehicle artifact token named Zeppelin with flying and crew 3. " +
        "(It has \"Tap any number of creatures you control with total power 3 or more: This token becomes an artifact creature until end of turn.\")"

    keywords(Keyword.VIGILANCE)

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.END)
        effect = Patterns.Group.untapGroup(
            filter = GroupFilter(GameObjectFilter.ArtifactCreature.youControl(), excludeSelf = true)
        )
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{3}{W}"), Costs.Tap)
        effect = Effects.CreatePredefinedToken("Zeppelin")
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "6"
        artist = "Bartek Fedyczak"
        imageUri = "https://cards.scryfall.io/normal/front/f/3/f3a5a044-d474-43f4-95a9-e67808908e6c.jpg?1783919196"
    }
}
