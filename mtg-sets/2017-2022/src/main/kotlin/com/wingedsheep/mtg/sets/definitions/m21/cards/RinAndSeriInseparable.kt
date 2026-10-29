package com.wingedsheep.mtg.sets.definitions.m21.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Rin and Seri, Inseparable — Core Set 2021 #278
 * {1}{R}{G}{W} · Legendary Creature — Dog Cat · 4/4
 *
 * Whenever you cast a Dog spell, create a 1/1 green Cat creature token.
 * Whenever you cast a Cat spell, create a 1/1 white Dog creature token.
 * {R}{G}{W}, {T}: Rin and Seri deals damage to any target equal to the number of Dogs you control.
 * You gain life equal to the number of Cats you control.
 *
 * Both counts are read as the ability resolves (and include Rin and Seri itself, a Dog Cat). If the
 * target is illegal on resolution the whole ability fizzles, so no life is gained.
 */
private val dogsYouControl = DynamicAmounts.battlefield(Player.You, GameObjectFilter.Permanent.withSubtype("Dog")).count()
private val catsYouControl = DynamicAmounts.battlefield(Player.You, GameObjectFilter.Permanent.withSubtype("Cat")).count()

val RinAndSeriInseparable = card("Rin and Seri, Inseparable") {
    manaCost = "{1}{R}{G}{W}"
    colorIdentity = "RGW"
    typeLine = "Legendary Creature — Dog Cat"
    power = 4
    toughness = 4
    oracleText = "Whenever you cast a Dog spell, create a 1/1 green Cat creature token.\n" +
        "Whenever you cast a Cat spell, create a 1/1 white Dog creature token.\n" +
        "{R}{G}{W}, {T}: Rin and Seri deals damage to any target equal to the number of Dogs you control. " +
        "You gain life equal to the number of Cats you control."

    triggeredAbility {
        trigger = Triggers.you.casts(GameObjectFilter.Any.withSubtype("Dog"))
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = setOf(Color.GREEN),
            creatureTypes = setOf("Cat"),
            imageUri = "https://cards.scryfall.io/normal/front/f/b/fbdf8dc1-1b10-4fce-97b9-1f5600500cc1.jpg?1783930586",
        )
        description = "Whenever you cast a Dog spell, create a 1/1 green Cat creature token."
    }

    triggeredAbility {
        trigger = Triggers.you.casts(GameObjectFilter.Any.withSubtype("Cat"))
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = setOf(Color.WHITE),
            creatureTypes = setOf("Dog"),
            imageUri = "https://cards.scryfall.io/normal/front/4/f/4f8107b3-8539-4b9c-8d0d-c512c940838f.jpg?1783930586",
        )
        description = "Whenever you cast a Cat spell, create a 1/1 white Dog creature token."
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{R}{G}{W}"), Costs.Tap)
        val t = target(Targets.Any)
        effect = Effects.DealDamage(dogsYouControl, t) then Effects.GainLife(catsYouControl)
        description = "{R}{G}{W}, {T}: Rin and Seri deals damage to any target equal to the number of Dogs " +
            "you control. You gain life equal to the number of Cats you control."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "278"
        artist = "Leesha Hannigan"
        imageUri = "https://cards.scryfall.io/normal/front/d/6/d605c780-a42a-4816-8fb9-63e3114a8246.jpg?1784483577"
        ruling("2020-06-23", "Rin and Seri's first two abilities don't trigger when you cast Rin and Seri.")
        ruling(
            "2020-06-23",
            "If the chosen target is an illegal target by the time Rin and Seri's last ability tries to " +
                "resolve, the ability won't resolve. You won't gain life."
        )
        ruling(
            "2020-06-23",
            "The amount of damage Rin and Seri's last ability deals is determined only as that ability " +
                "resolves. If Rin and Seri is still on the battlefield, it'll count itself."
        )
    }
}
