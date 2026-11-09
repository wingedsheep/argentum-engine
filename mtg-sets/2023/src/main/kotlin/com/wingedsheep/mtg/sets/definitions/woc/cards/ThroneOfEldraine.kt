package com.wingedsheep.mtg.sets.definitions.woc.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ChoiceType
import com.wingedsheep.sdk.scripting.EntersWithChoice
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.ManaRestriction
import com.wingedsheep.sdk.scripting.values.ManaColorSet

/**
 * Throne of Eldraine
 * {5}
 * Legendary Artifact
 * As Throne of Eldraine enters, choose a color.
 * {T}: Add four mana of the chosen color. Spend this mana only to cast monocolored spells of that color.
 * {3}, {T}: Draw two cards. Spend only mana of the chosen color to activate this ability.
 *
 * The color is chosen as it enters (replacement) and read back by [ManaColorSet.SourceChosenColor].
 * The two restrictions are on opposite sides of the payment:
 *  - the produced mana carries [ManaRestriction.SpellsOfChosenColor] (exactly one color, and it is the
 *    chosen one), which the engine binds to the chosen color as the mana is made;
 *  - the draw ability's own cost is paid only with mana of the chosen color
 *    (`payOnlyWithManaOf`), which recolors its `{3}`.
 * So the mana the first ability makes can cast a monocolored spell of its color but cannot pay the
 * second ability, and vice versa.
 */
val ThroneOfEldraine = card("Throne of Eldraine") {
    manaCost = "{5}"
    colorIdentity = ""
    typeLine = "Legendary Artifact"
    oracleText = "As Throne of Eldraine enters, choose a color.\n" +
        "{T}: Add four mana of the chosen color. Spend this mana only to cast monocolored spells of that color.\n" +
        "{3}, {T}: Draw two cards. Spend only mana of the chosen color to activate this ability."

    replacementEffect(EntersWithChoice(ChoiceType.COLOR))

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddManaOfChosenColor(
            amount = 4,
            restriction = ManaRestriction.SpellsOfChosenColor(exactColorCount = 1),
        )
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{3}"), Costs.Tap)
        effect = Effects.DrawCards(2)
        payOnlyWithManaOf = ManaColorSet.SourceChosenColor
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "28"
        artist = "Kieran Yanner"
        imageUri = "https://cards.scryfall.io/normal/front/9/0/9076e13d-d16e-4528-aad0-8b8081b00e8d.jpg?1783914983"
    }
}
