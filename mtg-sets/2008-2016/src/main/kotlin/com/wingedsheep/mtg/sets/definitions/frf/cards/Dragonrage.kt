package com.wingedsheep.mtg.sets.definitions.frf.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityId
import com.wingedsheep.sdk.scripting.ActivatedAbility
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Dragonrage
 * {2}{R}
 * Instant
 * Add {R} for each attacking creature you control. Until end of turn, attacking creatures you
 * control gain "{R}: This creature gets +1/+0 until end of turn."
 */
val Dragonrage = card("Dragonrage") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Instant"
    oracleText = "Add {R} for each attacking creature you control. Until end of turn, attacking creatures " +
        "you control gain \"{R}: This creature gets +1/+0 until end of turn.\""

    spell {
        effect = Effects.AddMana(Color.RED, DynamicAmounts.attackingCreaturesYouControl()) then
            Effects.GrantActivatedAbilityToGroup(
                ability = ActivatedAbility(
                    id = AbilityId.next(),
                    cost = Costs.Mana(ManaCost.parse("{R}")),
                    effect = Effects.ModifyStats(
                        power = 1,
                        toughness = 0,
                        target = EffectTarget.Self,
                        duration = Duration.EndOfTurn
                    )
                ),
                filter = GroupFilter(GameObjectFilter.Creature.attacking().youControl())
            )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "97"
        artist = "Tomasz Jedruszek"
        flavorText = "\"Dragons in the skies of Tarkir—for the first time, it feels like my home.\"\n—Sarkhan Vol"
        imageUri = "https://cards.scryfall.io/normal/front/7/7/77b7bf2a-9d68-4aa8-a0b1-5df22a5a3c1b.jpg?1783938689"
        ruling(
            "2014-11-24",
            "Even though it can generate mana, Dragonrage isn't a mana ability. It uses the stack and can be " +
                "responded to."
        )
        ruling(
            "2014-11-24",
            "Only creatures that are attacking as Dragonrage resolves will count toward how much mana is " +
                "generated, and only those creatures will gain the \"firebreathing\" activated ability. In other " +
                "words, casting Dragonrage before you've declared attackers usually won't do anything."
        )
    }
}
