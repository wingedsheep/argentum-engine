package com.wingedsheep.mtg.sets.definitions.neo.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Sokenzan Smelter
 * {1}{R}
 * Creature — Goblin Artificer
 * 2/2
 * At the beginning of combat on your turn, you may pay {1} and sacrifice an artifact. If you do,
 * create a 3/1 red Construct artifact creature token with haste.
 *
 * The two-part optional cost is one [Effects.MayPay] over a composite cost: `Gate.MayPay` checks
 * every part's affordability (the {1} *and* an artifact to sacrifice) before offering the "yes",
 * and the gate's pay-then-payoff sequence stops on error, so no token is made unless both are paid.
 */
val SokenzanSmelter = card("Sokenzan Smelter") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Goblin Artificer"
    power = 2
    toughness = 2
    oracleText = "At the beginning of combat on your turn, you may pay {1} and sacrifice an artifact. " +
        "If you do, create a 3/1 red Construct artifact creature token with haste."

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.BEGIN_COMBAT)
        effect = Effects.MayPay(
            cost = Effects.PayMana("{1}") then Effects.SacrificeOwn(filter = GameObjectFilter.Artifact),
            then = Effects.CreateToken(
                power = 3,
                toughness = 1,
                colors = setOf(Color.RED),
                creatureTypes = setOf("Construct"),
                keywords = setOf(Keyword.HASTE),
                artifactToken = true,
                imageUri = "https://cards.scryfall.io/normal/front/c/b/cbafdfc0-380a-482b-b5f8-a7a00ecf8da6.jpg?1783923715"
            ),
            descriptionOverride = "You may pay {1} and sacrifice an artifact. If you do, create a 3/1 red " +
                "Construct artifact creature token with haste."
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "164"
        artist = "Svetlin Velinov"
        flavorText = "Among the artisans of Sokenzan City, reusing every scrap of metal is a point of pride."
        imageUri = "https://cards.scryfall.io/normal/front/7/5/758724f9-93c9-4178-ae40-3fefc75a010e.jpg?1783923858"
    }
}
