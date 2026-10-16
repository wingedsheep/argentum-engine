package com.wingedsheep.mtg.sets.definitions.snc.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.grantedActivatedAbility
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Mage's Attendant — Streets of New Capenna #21
 * {2}{W} · Creature — Cat Rogue · 3/2
 *
 * When this creature enters, create a 1/1 blue Wizard creature token with
 * "{1}, Sacrifice this token: Counter target noncreature spell unless its controller pays {1}."
 *
 * The sacrifice is part of the token's activation cost, so the Wizard is gone before the counter
 * resolves (the same shape as Wizard Replica).
 */
val MagesAttendant = card("Mage's Attendant") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Cat Rogue"
    power = 3
    toughness = 2
    oracleText = "When this creature enters, create a 1/1 blue Wizard creature token with " +
        "\"{1}, Sacrifice this token: Counter target noncreature spell unless its controller pays {1}.\""

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = setOf(Color.BLUE),
            creatureTypes = setOf("Wizard"),
            activatedAbilities = listOf(
                grantedActivatedAbility {
                    val spell = target(TargetFilter.NoncreatureSpellOnStack)
                    cost = Costs.Composite(Costs.Mana("{1}"), Costs.SacrificeSelf)
                    effect = Effects.CounterUnlessPays("{1}")
                    description = "{1}, Sacrifice this token: Counter target noncreature spell " +
                        "unless its controller pays {1}."
                },
            ),
            imageUri = "https://cards.scryfall.io/normal/front/7/f/7f3edaf7-0ebd-4a8b-9954-dd91bb797744.jpg?1783922964",
        )
        description = "When this creature enters, create a 1/1 blue Wizard creature token with " +
            "\"{1}, Sacrifice this token: Counter target noncreature spell unless its controller pays {1}.\""
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "21"
        artist = "Igor Kieryluk"
        flavorText = "Crime sometimes makes unlikely allies into lifelong friends."
        imageUri = "https://cards.scryfall.io/normal/front/0/c/0cce08e8-340b-4a67-9884-4cbf01d63790.jpg?1783923155"
    }
}
