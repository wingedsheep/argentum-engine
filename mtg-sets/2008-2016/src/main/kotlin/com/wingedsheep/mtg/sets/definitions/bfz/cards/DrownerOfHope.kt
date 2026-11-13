package com.wingedsheep.mtg.sets.definitions.bfz.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivatedAbility
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Drowner of Hope
 * {5}{U}
 * Creature — Eldrazi
 * 5/5
 * Devoid (This card has no color.)
 * When this creature enters, create two 1/1 colorless Eldrazi Scion creature tokens. They have
 * "Sacrifice this token: Add {C}."
 * Sacrifice an Eldrazi Scion: Tap target creature.
 *
 * The Scion token is the same inline shape as Eyeless Watcher's. "An Eldrazi Scion" names two
 * creature types at once, so the sacrifice filter requires both subtypes (Kor Castigator's shape);
 * any Eldrazi Scion you control pays it, not only the ones this creature made.
 */
val DrownerOfHope = card("Drowner of Hope") {
    manaCost = "{5}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Eldrazi"
    power = 5
    toughness = 5
    oracleText = "Devoid (This card has no color.)\n" +
        "When this creature enters, create two 1/1 colorless Eldrazi Scion creature tokens. " +
        "They have \"Sacrifice this token: Add {C}.\"\n" +
        "Sacrifice an Eldrazi Scion: Tap target creature."

    keywords(Keyword.DEVOID)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = emptySet(),
            creatureTypes = setOf("Eldrazi", "Scion"),
            count = 2,
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

    activatedAbility {
        cost = Costs.Sacrifice(GameObjectFilter.Permanent.withSubtype(Subtype.ELDRAZI).withSubtype("Scion"))
        val t = target(TargetFilter.Creature)
        effect = Effects.Tap(t)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "57"
        artist = "Tomasz Jedruszek"
        imageUri = "https://cards.scryfall.io/normal/front/0/f/0fd5b48c-6850-47d4-8106-297be6c8f708.jpg?1783938213"
        ruling(
            "2015-08-25",
            "You can sacrifice any Eldrazi Scion to activate Drowner of Hope's last ability, " +
                "not just one created by Drowner of Hope."
        )
        ruling(
            "2015-08-25",
            "You can't sacrifice the same permanent to pay two different costs. Notably, if you " +
                "sacrifice an Eldrazi Scion to activate the last ability of Drowner of Hope, you won't add {C}."
        )
        ruling(
            "2015-08-25",
            "Sacrificing an Eldrazi Scion creature token to add {C} is a mana ability. " +
                "It doesn't use the stack and can't be responded to."
        )
    }
}
