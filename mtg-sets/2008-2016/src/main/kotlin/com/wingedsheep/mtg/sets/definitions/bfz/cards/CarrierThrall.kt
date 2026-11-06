package com.wingedsheep.mtg.sets.definitions.bfz.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivatedAbility
import com.wingedsheep.sdk.scripting.TimingRule

/**
 * Carrier Thrall
 * {1}{B}
 * Creature — Vampire
 * 2/1
 * When this creature dies, create a 1/1 colorless Eldrazi Scion creature token. It has
 * "Sacrifice this token: Add {C}."
 *
 * The Scion is the Blisterpod shape: a dies trigger creating a colorless 1/1 token carrying a
 * sacrifice mana ability.
 */
val CarrierThrall = card("Carrier Thrall") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Vampire"
    power = 2
    toughness = 1
    oracleText = "When this creature dies, create a 1/1 colorless Eldrazi Scion creature token. " +
        "It has \"Sacrifice this token: Add {C}.\""

    triggeredAbility {
        trigger = Triggers.self.dies()
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = emptySet(),
            creatureTypes = setOf("Eldrazi", "Scion"),
            activatedAbilities = listOf(
                ActivatedAbility(
                    cost = Costs.SacrificeSelf,
                    effect = Effects.AddColorlessMana(1),
                    isManaAbility = true,
                    timing = TimingRule.ManaAbility,
                    descriptionOverride = "Sacrifice this token: Add {C}.",
                )
            ),
            imageUri = "https://cards.scryfall.io/normal/front/b/9/b999a0fe-d2d0-4367-9abb-6ce5f3764f19.jpg?1783938121",
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "106"
        artist = "Lius Lasahido"
        flavorText = "\"Later we will mourn those we have lost. First we must end their suffering.\"\n—Drana, Kalastria bloodchief"
        imageUri = "https://cards.scryfall.io/normal/front/b/d/bd2ab895-9225-4eba-90c3-4023db4f8b70.jpg?1783938203"
        ruling("2015-08-25", "Eldrazi Scions are similar to Eldrazi Spawn, seen in the Zendikar block. Note that Eldrazi Scions are 1/1, not 0/1.")
        ruling("2015-08-25", "Eldrazi and Scion are each separate creature types. Anything that affects Eldrazi will affect these tokens, for example.")
        ruling("2015-08-25", "Sacrificing an Eldrazi Scion creature token to add {C} is a mana ability. It doesn't use the stack and can't be responded to.")
        ruling("2015-08-25", "Some instants and sorceries that create Eldrazi Scions require targets. If all targets for such a spell have become illegal by the time that spell tries to resolve, the spell won't resolve and none of its effects will happen. You won't get any Eldrazi Scions.")
    }
}
